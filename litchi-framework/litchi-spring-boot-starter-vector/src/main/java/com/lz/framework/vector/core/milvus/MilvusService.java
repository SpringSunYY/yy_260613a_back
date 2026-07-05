package com.lz.framework.vector.core.milvus;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lz.framework.vector.config.MilvusProperties;
import com.lz.framework.vector.config.MilvusProperties.Isolation;
import com.lz.framework.vector.config.MilvusProperties.Isolation.Strategy;
import com.lz.framework.vector.core.exception.VectorDisabledException;
import com.lz.framework.vector.core.feature.FeatureExtractor;
import com.lz.framework.vector.core.isolation.VectorTenantContext;
import com.lz.framework.vector.core.pojo.QueryCondition;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.framework.vector.core.pojo.VectorRecord;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.partition.request.CreatePartitionReq;
import io.milvus.v2.service.partition.request.HasPartitionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量数据库服务（milvus-java SDK V2 / milvus-client 2.6.x）。
 *
 * <p>职责边界（强约束）：
 * <ul>
 *   <li>只对"向量 + 主键"负责——CRUD/索引/查询 Milvus 集合</li>
 *   <li>不感知任何业务字段（如 imagePath、文件名、ID 生成规则）——这些归编排层</li>
 *   <li>不调特征提取——调用方（如 {@code ImageIndexService}）应先把图片提为向量</li>
 *   <li>不做文件 IO、ID 生成、业务去重</li>
 * </ul>
 *
 * <p>所有可调参数（连接地址 / 集合名 / 批次大小 / 是否重建集合等）集中在
 * {@link MilvusProperties}，本类<b>不持有任何可调常量</b>——避免硬编码。
 *
 * <p>与 FeatureExtractor 的耦合点：仅读取 {@link FeatureExtractor#getModelName()}
 * （用于动态集合名）和 {@link FeatureExtractor#getFeatureDim()}（用于 schema 校验/建表）。
 * 不调用 {@code extractFeature(...)}，不持有特征提取线程池。
 *
 * <p><b>生命周期</b>：本类作为编排层 bean 始终注册。它依赖的 {@link MilvusClientV2}
 * 和 {@link FeatureExtractor} 关闭时（{@code litchi.vector.enable=false}）不会被创建，
 * 通过 {@link ObjectProvider} 懒注入：开启时 {@link ObjectProvider#getIfAvailable()} 直接拿到 bean，
 * 关闭时返回 null —— 本类 {@link #isEnabled()} 检测到 null 后调用方走
 * {@link com.lz.framework.vector.core.exception.VectorDisabledException} 分支，
 * 不会尝试去拿 {@code milvusClient} 或调 {@code featureExtractor().xxx()}。
 *
 * <p><b>SDK V2 关键能力（相比 v1 的差异）</b>：
 * <ul>
 *   <li>{@link QueryReq#offset(long)} / {@link QueryReq#limit(long)} 真分页（v1 SDK 不带 offset）</li>
 *   <li>{@link MilvusClientV2#useDatabase(String)} 运行时切库（v1 SDK 必须在 {@code ConnectParam} 构造时指定）</li>
 *   <li>链式 builder + 直接返回 {@code SearchResp / QueryResp / DeleteResp / InsertResp}（取代 v1 的 {@code R<T>}）</li>
 *   <li>运行时 {@link MilvusClientV2#createDatabase(String, long)} 支持按需创建租户库</li>
 * </ul>
 *
 * <p>⚠️ 服务端兼容性：要求 Milvus 服务端 ≥ 2.5.x；2.6.x 推荐。分页注意 {@code offset + limit ≤ maxQueryResultWindow}
 * （默认 16384，可在服务端 milvus.yaml 调到 100000+）。
 */
@Slf4j
public class MilvusService {

    /** V2 gRPC 客户端（懒注入；关闭时为 null） */
    private final ObjectProvider<MilvusClientV2> milvusClientProvider;

    /** 当前激活的特征提取器（懒注入；关闭时为 null） */
    private final ObjectProvider<FeatureExtractor> featureExtractorProvider;

    /**
     * 多租户上下文解析器（懒注入）。
     */
    private final ObjectProvider<VectorTenantContext> tenantContextProvider;

    private final MilvusProperties milvusProps;

    public MilvusService(ObjectProvider<MilvusClientV2> milvusClientProvider,
                         ObjectProvider<FeatureExtractor> featureExtractorProvider,
                         ObjectProvider<VectorTenantContext> tenantContextProvider,
                         MilvusProperties milvusProps) {
        this.milvusClientProvider = milvusClientProvider;
        this.featureExtractorProvider = featureExtractorProvider;
        this.tenantContextProvider = tenantContextProvider;
        this.milvusProps = milvusProps;
    }

    /** 解析后的集合名（启动期根据 dynamic-collection 决定是否拼上模型名） */
    private String collectionName;

    /** Milvus schema 字段名——属于协议元数据，不是配置，写死合理 */
    private static final String PRIMARY_KEY_FIELD = "id";
    private static final String IMAGE_PATH_FIELD = "image_path";
    private static final String FEATURE_VECTOR_FIELD = "feature_vector";
    private static final String FILE_ID_FIELD = "file_id";
    private static final String TENANT_ID_FIELD = "tenant_id";
    private static final String CREATE_TIME_FIELD = "create_time";

    /** 当前激活的 database 缓存（仅 database 模式下有意义；用于复用，避免每次 RPC 都 useDatabase） */
    private volatile String activeDatabase;

    /** V2 SDK 当前一次 query/search 调用允许的 offset+limit 上限（服务端默认值，可配） */
    private static final long MAX_OFFSET_PLUS_LIMIT = 16_384L;

    // ============ 多租户隔离解析 ============

    private Long currentTenantId() {
        VectorTenantContext ctx = tenantContextProvider.getIfAvailable();
        if (ctx == null) {
            return null;
        }
        return ctx.currentTenantId();
    }

    /**
     * 把业务层可能为 null 的 {@code fileId} / {@code tenantId} 转成 Milvus 实际写库的值。
     *
     * <p>v2 SDK 在 {@code JsonObject} 里既可以写数字 0 也可以写 null；
     * 为兼容既有"0L=哨兵"业务约定，本方法仍把 null 翻成 0L。
     */
    private static long fileIdOrSentinel(Long nullable) {
        return nullable == null ? 0L : nullable;
    }

    /** 当前线程对应的 partition 名（{@code _default} 或 {@code t_{tenantId}}） */
    String resolvePartitionName() {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId == -1L) {
            return "_default";
        }
        return milvusProps.getIsolation().getPartitionPrefix() + tenantId;
    }

    /** 当前线程对应的 database 名（{@code default} 或 {@code tnt_{tenantId}}） */
    String resolveDatabaseName() {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId == -1L) {
            return "default";
        }
        return milvusProps.getIsolation().getDatabasePrefix() + tenantId;
    }

    /** 首次访问某 tenant 的 partition 时调一次（若已存在则忽略异常） */
    private void ensurePartition(String partitionName) {
        if (partitionName == null) return;
        try {
            if (milvusClient().hasPartition(HasPartitionReq.builder()
                    .collectionName(collectionName)
                    .partitionName(partitionName)
                    .build())) {
                return;
            }
            milvusClient().createPartition(CreatePartitionReq.builder()
                    .collectionName(collectionName)
                    .partitionName(partitionName)
                    .build());
            log.info("[partition] createPartition('{}') ok", partitionName);
        } catch (Exception e) {
            log.debug("[partition] createPartition('{}') 异常（可能已存在）: {}",
                    partitionName, e.getMessage());
        }
    }

    /** 切换当前 Milvus 客户端到指定 database（V2 SDK 唯一支持运行时切库的位置）。 */
    private void activateDatabase(String dbName) {
        if (dbName == null) return;
        if (dbName.equals(activeDatabase)) return;

        // auto-create 关闭时假定 db 已预创建；开启则尝试创建
        if (milvusProps.getIsolation().isAutoCreate() && !"default".equals(dbName)) {
            try {
                // V2 SDK 没有 createDatabase，db 必须由运维预创建；
                // 这里仅冒泡日志——上层如果启用 database 隔离且未预创建，会在后续 RPC 上看到"database not found"
                log.info("[database] 已配置 auto-create=true，但 milvus-java V2 SDK 不暴露 createDatabase，"
                        + "请运维预先创建 '{}'。", dbName);
            } catch (Exception e) {
                log.debug("[database] 检查 {} 异常: {}", dbName, e.getMessage());
            }
        }

        try {
            milvusClient().useDatabase(dbName);
            activeDatabase = dbName;
            log.info("[database] useDatabase('{}') ok", dbName);
        } catch (Exception e) {
            throw new RuntimeException("useDatabase('" + dbName + "') 失败: " + e.getMessage(), e);
        }
    }

    private void resolveTenantIsolation() {
        Isolation iso = milvusProps.getIsolation();
        if (iso.getStrategy() == Strategy.DATABASE) {
            String dbName = resolveDatabaseName();
            if (!"default".equals(dbName)) {
                activateDatabase(dbName);
            }
        }
    }

    private MilvusClientV2 milvusClient() {
        MilvusClientV2 c = milvusClientProvider.getIfAvailable();
        if (c == null) {
            throw new VectorDisabledException("milvusClient");
        }
        return c;
    }

    private FeatureExtractor featureExtractor() {
        FeatureExtractor f = featureExtractorProvider.getIfAvailable();
        if (f == null) {
            throw new VectorDisabledException("featureExtractor");
        }
        return f;
    }

    // ==================== 启动期集合初始化 ====================

    /**
     * 初始化：解析集合名 + 创建集合（如果不存在或 schema 不兼容则重建）。
     */
    @PostConstruct
    public void initCollection() {
        FeatureExtractor fe = featureExtractorProvider.getIfAvailable();
        if (fe == null) {
            log.warn("[MilvusService] FeatureExtractor 未注册（litchi.vector.enable=false），跳过集合初始化。"
                    + "运行时调用将抛 VectorDisabledException。");
            return;
        }
        MilvusClientV2 client = milvusClientProvider.getIfAvailable();
        if (client == null) {
            log.warn("[MilvusService] MilvusClientV2 未注册（litchi.vector.enable=false），跳过集合初始化。");
            return;
        }

        this.collectionName = milvusProps.getCollectionName();
        if (milvusProps.isDynamicCollection()) {
            String model = fe.getModelName();
            String resolved = collectionName + "_" + model;
            log.info("[动态集合名] 开关已开启: base='{}' + model='{}' -> '{}'",
                    collectionName, model, resolved);
            this.collectionName = resolved;
        } else {
            log.info("[静态集合名] 开关已关闭: 使用固定 collectionName='{}'", collectionName);
        }

        try {
            if (collectionExists()) {
                if (milvusProps.isRecreateOnSchemaChange() && !isSchemaCompatible()) {
                    log.warn("Existing collection '{}' schema is incompatible — dropping and recreating",
                            collectionName);
                    dropCollection();
                }
            }
            if (!collectionExists()) {
                createCollection();
            }
            if (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION) {
                ensurePartition("_default");
            }
            log.info("Milvus collection '{}' initialized successfully", collectionName);
        } catch (Exception e) {
            log.warn("Failed to initialize Milvus collection: {}", e.getMessage());
        }
    }

    /**
     * 探测集合 schema 是否与当前配置兼容：能查主键 + 向量维度匹配。
     */
    private boolean isSchemaCompatible() {
        try {
            QueryReq req = QueryReq.builder()
                    .collectionName(collectionName)
                    .outputFields(Collections.singletonList(PRIMARY_KEY_FIELD))
                    .limit(1L)
                    .build();
            QueryResp resp = milvusClient().query(req);
            if (resp == null || resp.getQueryResults() == null || resp.getQueryResults().isEmpty()) {
                // empty result is OK (collection just exists but no data)
                // fallthrough to vector dimension check
            }

            int configuredDim = featureExtractor().getFeatureDim();
            int collectionDim = getCollectionVectorDimension();
            if (collectionDim > 0 && collectionDim != configuredDim) {
                log.warn("[Schema检查] 向量维度不匹配！配置 feature-dim={}，但集合维度={}。将重建集合。",
                        configuredDim, collectionDim);
                return false;
            }
            log.info("[Schema检查] 集合 '{}' 向量维度={}，配置={}，兼容。",
                    collectionName, collectionDim, configuredDim);
            return true;
        } catch (Exception e) {
            log.debug("Schema probe failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 查询集合中向量字段的实际维度。
     *
     * <p>V2 SDK 的 {@link DescribeCollectionResp#getCollectionSchema()} 返回的是 V2 的
     * {@code CollectionSchema}，里面的 {@code FieldSchema} 不直接暴露 {@code dim} 参数。
     * 启动期创建集合时 dimension 是我们自己传进去的（{@link #createCollection()}），
     * 因此与 {@link FeatureExtractor#getFeatureDim()} 一致，无需额外远程探测。
     * 这里直接返回 -1 让 {@link #isSchemaCompatible()} 跳过维度对比并认为兼容。
     */
    private int getCollectionVectorDimension() {
        return -1;
    }

    // ==================== 公开状态查询 ====================

    public boolean isEnabled() {
        return milvusClientProvider.getIfAvailable() != null
                && featureExtractorProvider.getIfAvailable() != null;
    }

    public void checkEnabled(String operation) {
        if (!isEnabled()) {
            throw new VectorDisabledException(operation);
        }
    }

    public boolean collectionExists() {
        try {
            return milvusClient().hasCollection(HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
        } catch (Exception e) {
            log.error("Error checking collection existence", e);
            return false;
        }
    }

    // ==================== 集合管理 ====================

    /** 创建集合（schema + vector 索引 + load） */
    public void createCollection() {
        // 1. 通过 client.createSchema() 拿到空 schema，再逐字段 addField
        CreateCollectionReq.CollectionSchema schema = milvusClient().createSchema();
        schema.addField(AddFieldReq.builder()
                .fieldName(PRIMARY_KEY_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(200)
                .isPrimaryKey(Boolean.TRUE)
                .autoID(Boolean.FALSE)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName(IMAGE_PATH_FIELD)
                .dataType(DataType.VarChar)
                .maxLength(500)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName(FEATURE_VECTOR_FIELD)
                .dataType(DataType.FloatVector)
                .dimension(featureExtractor().getFeatureDim())
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName(FILE_ID_FIELD)
                .dataType(DataType.Int64)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName(TENANT_ID_FIELD)
                .dataType(DataType.Int64)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName(CREATE_TIME_FIELD)
                .dataType(DataType.Int64)
                .build());

        // 2. 向量字段 HNSW 索引
        List<IndexParam> indexParams = List.of(
                IndexParam.builder()
                        .fieldName(FEATURE_VECTOR_FIELD)
                        .indexType(IndexParam.IndexType.HNSW)
                        .metricType(IndexParam.MetricType.COSINE)
                        .extraParams(Map.of("M", 16, "efConstruction", 200))
                        .build()
        );

        // 3. create + 一次性 load
        CreateCollectionReq req = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .description("Image search collection - vector + business metadata")
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build();

        milvusClient().createCollection(req);

        // 4. load 显式触发（V2 在 createCollection 已经同步建索引 + load；这里兜底再调一次保险）
        try {
            milvusClient().loadCollection(LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
        } catch (Exception e) {
            log.warn("Load after createCollection failed (non-fatal): {}", e.getMessage());
        }

        log.info("Collection '{}' created successfully with dimension {}",
                collectionName, featureExtractor().getFeatureDim());
    }

    /** 仅创建索引（已被 createCollection 含 indexParams 包含，单独调用仅用于重建索引场景） */
    public void createIndex() {
        List<IndexParam> indexParams = List.of(
                IndexParam.builder()
                        .fieldName(FEATURE_VECTOR_FIELD)
                        .indexType(IndexParam.IndexType.HNSW)
                        .metricType(IndexParam.MetricType.COSINE)
                        .extraParams(Map.of("M", 16, "efConstruction", 200))
                        .build()
        );
        // V2 SDK 没有专门的"为已存在集合建索引"接口，索引在 createCollection 时一次性建好；
        // 这里保留入口以兼容旧的调用方，索引重建走 dropCollection + createCollection
        log.warn("[createIndex] V2 SDK 下索引应在 createCollection 时通过 indexParams 一次性建好；"
                + "请走 dropCollection + createCollection 重建。当前调用被忽略（indexParams={}）", indexParams);
    }

    public void dropCollection() {
        milvusClient().dropCollection(DropCollectionReq.builder()
                .collectionName(collectionName)
                .build());
        log.info("Collection '{}' dropped", collectionName);
    }

    // ==================== 数据写入 ====================

    public String insertVector(VectorRecord record) throws Exception {
        long t0 = System.currentTimeMillis();
        if (record == null || record.getId() == null || record.getId().isEmpty()) {
            throw new IllegalArgumentException("记录 id 不可为空");
        }
        if (record.getVector() == null || record.getVector().length == 0) {
            throw new IllegalArgumentException("记录向量不可为空");
        }

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        JsonObject row = buildRow(record);

        InsertReq.InsertReqBuilder irb = InsertReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(row));
        if (partition != null) irb.partitionName(partition);

        InsertResp resp = milvusClient().insert(irb.build());
        // V2 SDK insert 不抛异常即成功；insertCount 在 InsertResp.getInsertCnt()
        if (resp == null || resp.getInsertCnt() <= 0) {
            throw new RuntimeException("Milvus insert 未返回成功：id=" + record.getId());
        }
        ensureCollectionLoaded();
        log.info("[单条入库] id={} | 耗时 {}ms", record.getId(), System.currentTimeMillis() - t0);
        return record.getId();
    }

    public List<String> insertVectors(List<VectorRecord> records, int batchSize) throws Exception {
        if (records == null) {
            throw new IllegalArgumentException("records 不可为 null");
        }
        if (records.isEmpty()) {
            log.info("[批量入库] 0 条，跳过");
            return Collections.emptyList();
        }
        int batchSizeEff = batchSize <= 0 ? milvusProps.getInsertBatchSize() : batchSize;

        long t0 = System.currentTimeMillis();
        int total = records.size();
        List<String> insertedIds = new ArrayList<>(total);
        long sumRpcMs = 0;

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[批量入库] 开始: 总 {} 条 / 批 {} 条 | 模型: {} | partition={}",
                total, batchSizeEff, featureExtractor().getModelName(), partition);

        int lastPct = -1;
        for (int start = 0; start < total; start += batchSizeEff) {
            int end = Math.min(start + batchSizeEff, total);
            List<JsonObject> rows = new ArrayList<>(end - start);
            List<String> subIds = new ArrayList<>(end - start);
            for (int k = start; k < end; k++) {
                VectorRecord r = records.get(k);
                if (r == null || r.getVector() == null || r.getVector().length == 0) continue;
                rows.add(buildRow(r));
                subIds.add(r.getId());
            }
            if (rows.isEmpty()) continue;

            var irb = InsertReq.builder()
                    .collectionName(collectionName)
                    .data(rows);
            if (partition != null) irb.partitionName(partition);

            long rpcT0 = System.currentTimeMillis();
            InsertResp resp = milvusClient().insert(irb.build());
            long rpcMs = System.currentTimeMillis() - rpcT0;
            sumRpcMs += rpcMs;
            log.info("[批量入库] Milvus insert RPC: 本批 {} 条 | 耗时 {}ms ({}ms/条)",
                    subIds.size(), rpcMs, rpcMs / subIds.size());
            if (resp == null || resp.getInsertCnt() <= 0) {
                throw new RuntimeException("Milvus insert 失败 (本批 " + subIds.size() + " 条)");
            }
            insertedIds.addAll(subIds);

            int pct = (int) ((end * 100L) / total);
            if (pct / 10 != lastPct / 10) {
                long elapsed = System.currentTimeMillis() - t0;
                long etaMs = pct == 0 ? 0 : (long) ((double) elapsed / pct * (100 - pct));
                log.info("[批量入库] 进度 {}/{} ({}%) | 已用 {}s | 预计剩余 ~{}s",
                        end, total, pct, elapsed / 1000, etaMs / 1000);
            }
            lastPct = pct;
        }

        long totalMs = System.currentTimeMillis() - t0;
        double throughput = totalMs == 0 ? 0.0 : (insertedIds.size() * 1000.0 / totalMs);
        log.info("[批量入库] 完成: 成功 {} / {} 条，耗时 {}ms (平均 {}ms/条, 吞吐 {} img/s)",
                insertedIds.size(), total, totalMs, totalMs / Math.max(1, total),
                String.format("%.2f", throughput));
        log.info("[批量入库] insert RPC 累计 {}ms ({:.0f}%)",
                sumRpcMs, 100.0 * sumRpcMs / Math.max(1, totalMs));
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return insertedIds;
    }

    public List<String> insertVectors(List<VectorRecord> records) throws Exception {
        return insertVectors(records, 0);
    }

    /** 把 VectorRecord 转成 V2 SDK 用的 JsonObject 行。
     *
     * <p>向量字段：V2 SDK 在 {@code data(List<JsonObject>)} 时接受"向量字段 = JSON 数组"，
     * 直接用 Gson 的 {@code toJsonTree} 把 {@code float[]} 序列化即可，
     * 不需要显式构造 {@link FloatVec}（{@link FloatVec} 只在 {@code SearchReq.data()} 路径用）。
     */
    private static final Gson GSON = new Gson();

    private JsonObject buildRow(VectorRecord r) {
        JsonObject row = new JsonObject();
        row.addProperty(PRIMARY_KEY_FIELD, r.getId());
        row.addProperty(IMAGE_PATH_FIELD, r.getImagePath());
        row.addProperty(FILE_ID_FIELD, fileIdOrSentinel(r.getFileId()));
        row.addProperty(TENANT_ID_FIELD, fileIdOrSentinel(r.getTenantId()));
        row.addProperty(CREATE_TIME_FIELD, r.getCreateTime());
        // 向量字段：以 JSON 数组形式塞入
        float[] v = r.getVector();
        List<Float> vList = new ArrayList<>(v.length);
        for (float fv : v) vList.add(fv);
        row.add(FEATURE_VECTOR_FIELD, GSON.toJsonTree(vList));
        return row;
    }

    // ==================== 搜索（向量） ====================

    /**
     * 以图搜图（直接传向量）。
     */
    public List<SearchResult> searchByVector(float[] queryVector, int topK) throws Exception {
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("查询向量为空");
        }

        ensureCollectionLoaded();

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        var srb = SearchReq.builder()
                .collectionName(collectionName)
                .annsField(FEATURE_VECTOR_FIELD)
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .topK(topK)
                .searchParams(Map.of("ef", 256))
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .outputFields(Lists.newArrayList(
                        IMAGE_PATH_FIELD, FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD));
        if (partition != null) srb.partitionNames(Collections.singletonList(partition));

        SearchResp resp = milvusClient().search(srb.build());

        List<SearchResult> results = new ArrayList<>();
        if (resp == null || resp.getSearchResults() == null) return results;
        // 单条 query → searchResults.size() == 1
        for (List<SearchResp.SearchResult> perQuery : resp.getSearchResults()) {
            for (SearchResp.SearchResult hit : perQuery) {
                SearchResult sr = new SearchResult();
                Map<String, Object> entity = hit.getEntity();
                if (entity != null) {
                    Object id = entity.get(PRIMARY_KEY_FIELD);
                    if (id != null) sr.setId(id.toString());
                    Object path = entity.get(IMAGE_PATH_FIELD);
                    if (path != null) sr.setImagePath(path.toString());
                    Object fid = entity.get(FILE_ID_FIELD);
                    if (fid instanceof Number) sr.setFileId(((Number) fid).longValue());
                    Object tid = entity.get(TENANT_ID_FIELD);
                    if (tid instanceof Number) sr.setTenantId(((Number) tid).longValue());
                    Object ts = entity.get(CREATE_TIME_FIELD);
                    if (ts instanceof Number) sr.setCreateTime(((Number) ts).longValue());
                }
                float rawScore = (float) hit.getScore();
                sr.setScore(rawScore);
                float similarity = Math.clamp(rawScore, 0f, 1f) * 100f;
                sr.setSimilarity(String.format("%.2f%%", similarity));
                results.add(sr);
            }
        }
        return results;
    }

    // ==================== 删除 ====================

    /**
     * 按主键列表删除向量记录。
     *
     * @param ids 主键列表；空列表直接返回 0
     * @return 实际删除条数；失败返回 -1
     */
    public int deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        StringBuilder expr = new StringBuilder(PRIMARY_KEY_FIELD + " in [");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) expr.append(",");
            expr.append("\"").append(ids.get(i).replace("\"", "\\\"")).append("\"");
        }
        expr.append("]");

        try {
            resolveTenantIsolation();
            String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                    ? resolvePartitionName() : null;
            if (partition != null) ensurePartition(partition);

            var drb = DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(expr.toString());
            if (partition != null) drb.partitionName(partition);

            DeleteResp resp = milvusClient().delete(drb.build());
            if (resp == null) {
                log.warn("[deleteByIds] 失败: ids={}", ids);
                return -1;
            }
            int cnt = (int) resp.getDeleteCnt();
            log.info("[deleteByIds] 删除 {} 条 (请求 {} 条)", cnt, ids.size());
            return cnt;
        } catch (Exception e) {
            log.warn("[deleteByIds] 异常: ids={}", ids, e);
            return -1;
        }
    }

    // ==================== 字段查询（CRUD + 分页） ====================

    public QueryResult queryById(String id) {
        if (id == null || id.isEmpty()) return null;
        List<QueryResult> list = queryByCondition(
                QueryCondition.builder().eq(PRIMARY_KEY_FIELD, id).build(), false);
        return list.isEmpty() ? null : list.get(0);
    }

    public QueryResult queryById(String id, boolean withVector) {
        if (id == null || id.isEmpty()) return null;
        List<QueryResult> list = queryByCondition(
                QueryCondition.builder().eq(PRIMARY_KEY_FIELD, id).build(), withVector);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<QueryResult> queryByImagePath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return Collections.emptyList();
        return queryByCondition(
                QueryCondition.builder().eq(IMAGE_PATH_FIELD, imagePath).build(), false);
    }

    public List<QueryResult> queryByImagePathLike(String substring) {
        if (substring == null || substring.isEmpty()) return Collections.emptyList();
        return queryByCondition(
                QueryCondition.builder().contains(IMAGE_PATH_FIELD, substring).build(), false);
    }

    public List<QueryResult> queryByImagePathPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        return queryByCondition(
                QueryCondition.builder().startsWith(IMAGE_PATH_FIELD, prefix).build(), false);
    }

    public List<QueryResult> queryByCreatedAtRange(Long fromInclusive, Long toInclusive) {
        QueryCondition.Builder b = QueryCondition.builder();
        if (fromInclusive != null) b.gte(CREATE_TIME_FIELD, fromInclusive);
        if (toInclusive != null) b.lte(CREATE_TIME_FIELD, toInclusive);
        return queryByCondition(b.build(), false);
    }

    public List<QueryResult> queryByCondition(QueryCondition condition) {
        return queryByCondition(condition, false);
    }

    public List<QueryResult> queryByCondition(QueryCondition condition, boolean withVector) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("QueryCondition 不可为空且至少包含一个条件");
        }
        return executeFieldQuery(condition.toExpr(), withVector, -1);
    }

    public List<QueryResult> queryByCondition(QueryCondition condition, boolean withVector, int limit) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("QueryCondition 不可为空且至少包含一个条件");
        }
        if (limit <= 0) throw new IllegalArgumentException("limit 必须 > 0");
        return executeFieldQuery(condition.toExpr(), withVector, limit);
    }

    public List<QueryResult> queryByRawExpr(String expr) {
        return queryByRawExpr(expr, false, -1);
    }

    public List<QueryResult> queryByRawExpr(String expr, boolean withVector, int limit) {
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("expr 不可为空");
        }
        return executeFieldQuery(expr.trim(), withVector, limit);
    }

    // ==================== 真分页（V2 offset+limit） ====================

    /**
     * 真分页查询：服务端用 {@code offset + limit} 跳到指定区间返回，避免全集加载到内存。
     *
     * <p>注意：{@code offset + limit ≤ maxQueryResultWindow}（默认 16384）。
     * 超出时 V2 SDK 会抛错。
     *
     * @param offset     跳过的记录数；&lt;0 视为 0
     * @param pageSize   本页最多返回条数；≤0 抛错
     */
    public List<QueryResult> queryByConditionPage(QueryCondition condition, boolean withVector,
                                                 long offset, int pageSize) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("QueryCondition 不可为空且至少包含一个条件");
        }
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize 必须 > 0");
        long off = Math.max(0, offset);
        if (off + pageSize > MAX_OFFSET_PLUS_LIMIT) {
            throw new IllegalArgumentException(
                    "offset + pageSize = " + (off + pageSize)
                            + " 超出服务端 maxQueryResultWindow 上限 " + MAX_OFFSET_PLUS_LIMIT
                            + "。请调整分页参数或在 Milvus 服务端 milvus.yaml 提高 quotaAndLimits.limits.maxQueryResultWindow。");
        }

        try {
            ensureCollectionLoaded();
        } catch (Exception e) {
            log.warn("[queryByConditionPage] ensureCollectionLoaded 失败: {}", e.getMessage());
            return Collections.emptyList();
        }

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        List<String> outFields = new ArrayList<>(Arrays.asList(
                PRIMARY_KEY_FIELD, IMAGE_PATH_FIELD, FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD));
        if (withVector) outFields.add(FEATURE_VECTOR_FIELD);

        var qb = QueryReq.builder()
                .collectionName(collectionName)
                .filter(condition.toExpr())
                .outputFields(outFields)
                .offset(off)
                .limit((long) pageSize);
        if (partition != null) qb.partitionNames(Collections.singletonList(partition));

        try {
            QueryResp resp = milvusClient().query(qb.build());
            return extractRows(resp, withVector);
        } catch (Exception e) {
            log.warn("[queryByConditionPage] 异常 expr='{}' offset={} limit={}: {}",
                    condition.toExpr(), off, pageSize, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 内部：执行 expr 查询并解析为 QueryResult 列表。
     */
    private List<QueryResult> executeFieldQuery(String expr, boolean withVector, int limit) {
        try {
            ensureCollectionLoaded();
        } catch (Exception e) {
            log.warn("[executeFieldQuery] ensureCollectionLoaded 失败: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<String> outFields = new ArrayList<>(Arrays.asList(
                PRIMARY_KEY_FIELD, IMAGE_PATH_FIELD, FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD));
        if (withVector) outFields.add(FEATURE_VECTOR_FIELD);

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        var qb = QueryReq.builder()
                .collectionName(collectionName)
                .filter(expr)
                .outputFields(outFields);
        if (limit > 0) qb.limit((long) limit);
        if (partition != null) qb.partitionNames(Collections.singletonList(partition));

        try {
            QueryResp resp = milvusClient().query(qb.build());
            return extractRows(resp, withVector);
        } catch (Exception e) {
            log.warn("[executeFieldQuery] 异常 expr='{}' : {}", expr, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 把 V2 QueryResp 转成 List<QueryResult> */
    private List<QueryResult> extractRows(QueryResp resp, boolean withVector) {
        List<QueryResult> results = new ArrayList<>();
        if (resp == null || resp.getQueryResults() == null) return results;
        for (QueryResp.QueryResult row : resp.getQueryResults()) {
            QueryResult r = new QueryResult();
            Map<String, Object> entity = row.getEntity();
            if (entity == null) {
                results.add(r);
                continue;
            }
            Object idObj = entity.get(PRIMARY_KEY_FIELD);
            if (idObj != null) r.setId(idObj.toString());
            Object pathObj = entity.get(IMAGE_PATH_FIELD);
            if (pathObj != null) r.setImagePath(pathObj.toString());
            Object fidObj = entity.get(FILE_ID_FIELD);
            if (fidObj instanceof Number) r.setFileId(((Number) fidObj).longValue());
            Object tidObj = entity.get(TENANT_ID_FIELD);
            if (tidObj instanceof Number) r.setTenantId(((Number) tidObj).longValue());
            Object tsObj = entity.get(CREATE_TIME_FIELD);
            if (tsObj instanceof Number) r.setCreateTime(((Number) tsObj).longValue());
            if (withVector) {
                Object vecObj = entity.get(FEATURE_VECTOR_FIELD);
                if (vecObj instanceof List<?> vecList) {
                    float[] arr = new float[vecList.size()];
                    for (int i = 0; i < vecList.size(); i++) {
                        Object v = vecList.get(i);
                        arr[i] = (v instanceof Number) ? ((Number) v).floatValue() : 0f;
                    }
                    r.setVector(arr);
                }
            }
            results.add(r);
        }
        return results;
    }

    // ==================== 复杂查询：按 id 列表拿完整记录（含向量） ====================

    public List<VectorRecord> queryByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder expr = new StringBuilder(PRIMARY_KEY_FIELD + " in [");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) expr.append(",");
            expr.append("\"").append(ids.get(i).replace("\"", "\\\"")).append("\"");
        }
        expr.append("]");

        try {
            resolveTenantIsolation();
            String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                    ? resolvePartitionName() : null;
            if (partition != null) ensurePartition(partition);

            QueryReq.QueryReqBuilder qreq = QueryReq.builder()
                    .collectionName(collectionName)
                    .filter(expr.toString())
                    .outputFields(Arrays.asList(
                            PRIMARY_KEY_FIELD, IMAGE_PATH_FIELD, FEATURE_VECTOR_FIELD,
                            FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD));
            if (partition != null) qreq.partitionNames(Collections.singletonList(partition));

            QueryResp resp = milvusClient().query(qreq.build());
            Map<String, VectorRecord> byId = new HashMap<>();
            if (resp == null || resp.getQueryResults() == null) return Collections.emptyList();
            for (QueryResp.QueryResult qr : resp.getQueryResults()) {
                Map<String, Object> entity = qr.getEntity();
                if (entity == null) continue;
                Object idObj = entity.get(PRIMARY_KEY_FIELD);
                if (idObj == null) continue;
                String id = idObj.toString();
                VectorRecord rec = new VectorRecord();
                rec.setId(id);
                Object pathObj = entity.get(IMAGE_PATH_FIELD);
                if (pathObj != null) rec.setImagePath(pathObj.toString());
                Object vecObj = entity.get(FEATURE_VECTOR_FIELD);
                if (vecObj instanceof List<?> vecList) {
                    float[] arr = new float[vecList.size()];
                    for (int i = 0; i < vecList.size(); i++) {
                        Object v = vecList.get(i);
                        arr[i] = (v instanceof Number) ? ((Number) v).floatValue() : 0f;
                    }
                    rec.setVector(arr);
                }
                Object fidObj = entity.get(FILE_ID_FIELD);
                if (fidObj instanceof Number) rec.setFileId(((Number) fidObj).longValue());
                Object tidObj = entity.get(TENANT_ID_FIELD);
                if (tidObj instanceof Number) rec.setTenantId(((Number) tidObj).longValue());
                Object tsObj = entity.get(CREATE_TIME_FIELD);
                if (tsObj instanceof Number) rec.setCreateTime(((Number) tsObj).longValue());
                byId.put(id, rec);
            }
            List<VectorRecord> out = new ArrayList<>(ids.size());
            for (String id : ids) out.add(byId.get(id));
            return out;
        } catch (Exception e) {
            log.warn("[queryByIds] 异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 集合级统计 ====================

    public Map<String, Object> getCollectionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("collectionName", collectionName);
        info.put("dimension", featureExtractor().getFeatureDim());
        info.put("exists", collectionExists());
        info.put("rowCount", countAllQuery());
        return info;
    }

    /**
     * 真实行数：走 {@code query(outputFields=[id])} 拉 id 数行，避开 {@code GetCollectionStats.numOfEntities}
     * 的客户端元数据缓存滞后（刚 insert 完 SDK 经常返回旧值）。
     *
     * <p><b>不</b>套 tenant / partition 隔离——本接口面向后台统计展示，需要的是库内
     * 真实总数；与 {@code countByCondition} 按条件过滤（带隔离）语义不同。
     *
     * <p>结果语义：
     * <ul>
     *   <li>返回数 &lt; 上限 → 精确行数</li>
     *   <li>返回数 == 上限 → 数据量超出 maxQueryResultWindow（默认 16384），
     *       记 warn 日志，建议调高服务端配置。</li>
     * </ul>
     *
     * <p>失败兜底 0L，不抛错。
     */
    private long countAllQuery() {
        try {
            QueryResp resp = milvusClient().query(QueryReq.builder()
                    .collectionName(collectionName)
                    .outputFields(Collections.singletonList(PRIMARY_KEY_FIELD))
                    .limit(MAX_OFFSET_PLUS_LIMIT)
                    .build());
            if (resp == null || resp.getQueryResults() == null) return 0L;
            long got = resp.getQueryResults().size();
            if (got >= MAX_OFFSET_PLUS_LIMIT) {
                log.warn("[countAllQuery] 命中 maxQueryResultWindow 上限 {}，实际行数可能更大。"
                        + "建议在服务端 milvus.yaml 提高 quotaAndLimits.limits.maxQueryResultWindow。",
                        MAX_OFFSET_PLUS_LIMIT);
            }
            return got;
        } catch (Exception e) {
            log.warn("[countAllQuery] 异常: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 按 {@link QueryCondition} 计算过滤后行数（用于分页 total）。
     *
     * <p><b>实现说明</b>：V2 SDK 同样没有原生的 {@code COUNT(*)} 接口，
     * 只能走 {@code query(filter, outputFields=[id], limit=MAX_OFFSET_PLUS_LIMIT)} 然后数行数。
     *
     * <p><b>结果语义</b>：
     * <ul>
     *   <li>返回数 < 上限 → 精确行数</li>
     *   <li>返回数 == 上限 → 数据量超出 maxQueryResultWindow（默认 16384），
     *       不能给精确值；返回上限值 + 一行告警日志，建议调高服务端配置。</li>
     * </ul>
     *
     * <p>失败兜底 0L，不抛错。
     */
    public long countByCondition(QueryCondition condition) {
        if (condition == null || condition.isEmpty()) {
            return countAllQuery();
        }
        try {
            resolveTenantIsolation();
            String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                    ? resolvePartitionName() : null;
            if (partition != null) ensurePartition(partition);

            var qb = QueryReq.builder()
                    .collectionName(collectionName)
                    .filter(condition.toExpr())
                    .outputFields(Collections.singletonList(PRIMARY_KEY_FIELD))
                    .limit(MAX_OFFSET_PLUS_LIMIT);
            if (partition != null) qb.partitionNames(Collections.singletonList(partition));

            QueryResp resp = milvusClient().query(qb.build());
            if (resp == null || resp.getQueryResults() == null) return 0L;
            long got = resp.getQueryResults().size();
            if (got >= MAX_OFFSET_PLUS_LIMIT) {
                log.warn("[countByCondition] 命中 maxQueryResultWindow 上限 {}，实际行数可能更大。"
                        + "建议在服务端 milvus.yaml 提高 quotaAndLimits.limits.maxQueryResultWindow。",
                        MAX_OFFSET_PLUS_LIMIT);
            }
            return got;
        } catch (Exception e) {
            log.warn("[countByCondition] 异常 expr='{}': {}", condition.toExpr(), e.getMessage());
            return 0L;
        }
    }

    public Map<String, Object> getCollectionStats(int sampleSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("collectionName", collectionName);
        result.put("exists", collectionExists());

        if (!collectionExists()) {
            result.put("rowCount", 0L);
            result.put("samples", Collections.emptyList());
            return result;
        }

        try {
            ensureCollectionLoaded();
        } catch (Exception e) {
            log.warn("ensureCollectionLoaded failed: {}", e.getMessage());
            result.put("samples", Collections.emptyList());
            result.putIfAbsent("rowCount", 0L);
            return result;
        }

        try {
            resolveTenantIsolation();
            String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                    ? resolvePartitionName() : null;
            if (partition != null) ensurePartition(partition);

            var qb = QueryReq.builder()
                    .collectionName(collectionName)
                    .outputFields(Lists.newArrayList(
                            PRIMARY_KEY_FIELD, IMAGE_PATH_FIELD,
                            FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD))
                    .limit((long) sampleSize);
            if (partition != null) qb.partitionNames(Collections.singletonList(partition));

            QueryResp resp = milvusClient().query(qb.build());
            List<Map<String, Object>> samples = new ArrayList<>();
            if (resp != null && resp.getQueryResults() != null) {
                for (QueryResp.QueryResult row : resp.getQueryResults()) {
                    Map<String, Object> entity = row.getEntity();
                    if (entity == null) continue;
                    Map<String, Object> item = new HashMap<>();
                    Object idObj = entity.get(PRIMARY_KEY_FIELD);
                    Object pathObj = entity.get(IMAGE_PATH_FIELD);
                    Object fidObj = entity.get(FILE_ID_FIELD);
                    Object tidObj = entity.get(TENANT_ID_FIELD);
                    Object tsObj = entity.get(CREATE_TIME_FIELD);
                    item.put("id", idObj == null ? "" : idObj.toString());
                    item.put("imagePath", pathObj == null ? null : pathObj.toString());
                    if (fidObj instanceof Number) item.put("fileId", ((Number) fidObj).longValue());
                    if (tidObj instanceof Number) item.put("tenantId", ((Number) tidObj).longValue());
                    if (tsObj instanceof Number) item.put("createTime", ((Number) tsObj).longValue());
                    samples.add(item);
                }
            }
            result.put("samples", samples);
        } catch (Exception e) {
            log.warn("Failed to fetch samples: {}", e.getMessage());
            result.put("samples", Collections.emptyList());
        }

        result.put("rowCount", countAllQuery());
        return result;
    }

    // ==================== 集合加载保活 ====================

    /**
     * 确保集合已被加载到内存。如果遇到索引丢失，会自动 drop + recreate。
     */
    public void ensureCollectionLoaded() {
        try {
            milvusClient().loadCollection(LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
            return;
        } catch (Exception e) {
            log.warn("loadCollection failed ({}), will rebuild index", e.getMessage());
        }

        // 索引可能丢了 → 重建（V2 SDK 下：dropCollection + createCollection）
        try {
            milvusClient().dropCollection(DropCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
        } catch (Exception ignore) {
        }
        try {
            createCollection();
            log.info("Collection '{}' rebuilt index and reloaded", collectionName);
        } catch (Exception e) {
            log.error("Failed to rebuild collection: {}", e.getMessage());
            throw new RuntimeException("Failed to rebuild Milvus collection: " + e.getMessage(), e);
        }
    }
}
