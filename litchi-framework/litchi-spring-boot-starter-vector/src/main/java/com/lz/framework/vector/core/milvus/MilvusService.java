package com.lz.framework.vector.core.milvus;

import com.google.common.collect.Lists;
import com.lz.framework.vector.config.MilvusProperties;
import com.lz.framework.vector.config.MilvusProperties.Isolation;
import com.lz.framework.vector.config.MilvusProperties.Isolation.Strategy;
import com.lz.framework.vector.core.feature.FeatureExtractor;
import com.lz.framework.vector.core.isolation.VectorTenantContext;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.framework.vector.core.pojo.VectorRecord;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.QueryCondition;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;

import java.util.*;

/**
 * Milvus 向量数据库服务。
 *
 * <p>职责边界（强约束）：
 * <ul>
 *   <li>只对"向量 + 主键"负责——CRUD/索引/查询 Milvus 集合</li>
 *   <li>不感知任何业务字段（如 imagePath、文件名、ID 生成规则）——这些归编排层</li>
 *   <li>不调特征提取——调用方（如 {@link ImageIndexService}）应先把图片提为向量</li>
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
 * <p><b>生命周期</b>：本类作为编排层 bean 始终注册（{@link com.lz.framework.vector.config.VectorAutoConfiguration}
 * 不再用类级 {@code @ConditionalOnProperty} 控制）。它依赖的 {@link MilvusServiceClient}
 * 和 {@link FeatureExtractor} 关闭时（{@code litchi.vector.enable=false}）不会被创建，
 * 通过 {@link ObjectProvider} 懒注入：开启时 {@link ObjectProvider#getObject()} 直接拿到 bean，
 * 关闭时返回 null —— 本类 {@link #isEnabled()} 检测到 null 后调用方走
 * {@link com.lz.framework.vector.core.exception.VectorDisabledException} 分支，
 * 不会尝试去拿 {@code milvusClient} 或调 {@code featureExtractor().xxx()}。
 */
@Slf4j
public class MilvusService {

    /** gRPC Milvus 客户端（懒注入；关闭时为 null） */
    private final ObjectProvider<MilvusServiceClient> milvusClientProvider;

    /** 当前激活的特征提取器（懒注入；关闭时为 null） */
    private final ObjectProvider<FeatureExtractor> featureExtractorProvider;

    /**
     * 多租户上下文解析器（懒注入）。
     * <p>
     * 本字段由 {@link com.lz.framework.vector.config.VectorAutoConfiguration}
     * 通过 {@link ObjectProvider} 注入，对应的 bean 由 {@code litchi-spring-boot-starter-biz-tenant}
     * 反向依赖本模块后提供（{@code TenantVectorTenantContext implements VectorTenantContext}）。
     * 未引入 biz-tenant 时本字段无实现，本类的 {@link #currentTenantId()} 返回 null，
     * 走 {@code _default} partition/database —— 与 {@code litchi-spring-boot-starter-redis / job / mq}
     * 完全相同的反向桥接模式。
     */
    private final ObjectProvider<VectorTenantContext> tenantContextProvider;

    private final MilvusProperties milvusProps;

    public MilvusService(ObjectProvider<MilvusServiceClient> milvusClientProvider,
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

    // ============ 多租户隔离解析 ============

    /**
     * 拿到当前线程的租户 ID。与 {@link com.lz.framework.vector.core.isolation.VectorTenantContext}
     * 协作：本模块只声明这个接口，由 {@code litchi-spring-boot-starter-biz-tenant} 反向依赖
     * vector 后提供基于 {@code TenantContextHolder} 的实现。
     * 对齐 {@code litchi-spring-boot-starter-redis / job / mq} —— starter 自己不引 biz-tenant，
     * 由 biz-tenant 反向依赖并提供租户拦截器/适配器。
     * <ul>
     *   <li>{@code null}：当前线程没有设置租户 → 走 {@code _default} partition / database</li>
     *   <li>{@code -1L}：{@code @TenantIgnore} 显式忽略</li>
     *   <li>其它正数：真实租户 ID</li>
     * </ul>
     */
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
     * <p><b>为什么需要这层映射</b>：milvus-sdk-java 2.4.5 的
     * {@code io.milvus.param.ParamUtils.checkFieldData} 对 Int64 列的实现是
     * {@code if (!(value instanceof Long)) throw new ParamException(...)}，
     * {@code null instanceof Long} 返回 {@code false}，因此单条 list 里塞 {@code null} 必被拒。
     * 见 https://github.com/milvus-io/milvus-sdk-java/blob/v2.4.5/src/main/java/io/milvus/param/ParamUtils.java#L182-L188
     *
     * <p>并且该 SDK 版本的 {@code FieldType.Builder} 没有 {@code withNullable(boolean)}（2.5.0 才加），
     * 所以 Int64 列在 schema 层就是非空的，无法在 schema 上声明 nullable=true 来"豁免"校验。
     * 因此本方法把 {@code null} 替换为 {@code 0L} 哨兵。
     *
     * <p>未来升级 SDK 到 2.5+（修复了 nullable 校验 + 加了 {@code withNullable}）后，可在本方法里
     * 直接返回 nullable，并把 schema 字段加 {@code .withNullable(true)}，业务调用方代码完全无需改动。
     */
    private static long fileIdOrSentinel(Long nullable) {
        return nullable == null ? 0L : nullable;
    }

    /**
     * 解析当前线程对应的 Milvus partition 名。
     * <p>
     *   <ul>
     *     <li>无 tenant / 已 ignore：返回 {@code _default}（始终固定，不取决于 prefix 配置）</li>
     *     <li>有 tenant：返回 {@code t_{tenantId}}（partitionPrefix 可配）</li>
     *   </ul>
     */
    String resolvePartitionName() {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId == -1L) {
            return "_default";
        }
        return milvusProps.getIsolation().getPartitionPrefix() + tenantId;
    }

    /**
     * 解析当前线程对应的 Milvus database 名。
     * <p>
     *   <ul>
     *     <li>无 tenant / 已 ignore：返回 Milvus 内置的 {@code default}</li>
     *     <li>有 tenant：返回 {@code tnt_{tenantId}}（databasePrefix 可配）</li>
     *   </ul>
     */
    String resolveDatabaseName() {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId == -1L) {
            return "default";
        }
        return milvusProps.getIsolation().getDatabasePrefix() + tenantId;
    }

    /**
     * 首次访问某 tenant 的 partition 时调一次（若已存在则忽略异常）。
     */
    private void ensurePartition(String partitionName) {
        if (partitionName == null) return;
        try {
            R<RpcStatus> r = milvusClient().createPartition(
                    CreatePartitionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withPartitionName(partitionName)
                            .build()
            );
            if (r.getStatus() != R.Status.Success.getCode()
                    && r.getMessage() != null
                    && !r.getMessage().toLowerCase().contains("already")) {
                log.warn("[partition] createPartition('{}') 失败: {}", partitionName, r.getMessage());
            } else {
                log.info("[partition] createPartition('{}') ok", partitionName);
            }
        } catch (Exception e) {
            log.debug("[partition] createPartition('{}') 异常（可能已存在）: {}", partitionName, e.getMessage());
        }
    }

    /**
     * 切换当前 Milvus 客户端到指定 database。
     *
     * <p><b>当前 v1 SDK 限制</b>：项目使用的 milvus-sdk-java 2.4.5 中，
     * {@code MilvusServiceClient} 没有 {@code useDatabase()} 方法——
     * 该方法只在 v2 客户端（{@code MilvusClientV2}，随 milvus-sdk-java 2.5+ 引入）上可用。
     * 而 v1 客户端的 database 必须在 {@code ConnectParam} 构造时一次性指定，运行时无法切换。
     *
     * <p>本方法在 v1 SDK 下仅记录警告并直接抛错，引导用户：
     * <ul>
     *   <li>方案 A：升级到 milvus-sdk-java 2.5+，切到 {@code MilvusClientV2}，本方法改为调 {@code useDatabase}</li>
     *   <li>方案 B：保持当前 SDK + 切回 partition 模式（默认），同样能实现多租户隔离</li>
     *   <li>方案 C：每租户构造独立的 {@code MilvusServiceClient}（开销大，复杂）</li>
     * </ul>
     */
    private void activateDatabase(String dbName) {
        if (dbName == null) return;
        if (dbName.equals(activeDatabase)) return;

        // auto-create 关闭时，假定 db 已经预创建；开启则尝试创建（v1 SDK 支持）
        if (milvusProps.getIsolation().isAutoCreate() && !"default".equals(dbName)) {
            try {
                R<RpcStatus> r = milvusClient().createDatabase(
                        io.milvus.param.collection.CreateDatabaseParam.newBuilder()
                                .withDatabaseName(dbName)
                                .build()
                );
                if (r.getStatus() == R.Status.Success.getCode()) {
                    log.info("[database] createDatabase('{}') ok", dbName);
                } else if (r.getMessage() != null && r.getMessage().toLowerCase().contains("already")) {
                    log.debug("[database] createDatabase('{}') 已存在", dbName);
                } else {
                    log.warn("[database] createDatabase('{}') 失败: {}", dbName, r.getMessage());
                }
            } catch (Exception e) {
                log.debug("[database] createDatabase('{}') 异常（可能已存在）: {}", dbName, e.getMessage());
            }
        }

        // 尝试调用 v2 客户端的 useDatabase——v1 SDK 上不存在，本块通过 catch NoSuchMethodError 兼容。
        // 为了让代码在 v1 SDK 下也能编译通过，这里不再直接调 milvusClient().useDatabase(dbName)，
        // 而是显式抛出"当前 SDK 不支持"的错误。
        throw new UnsupportedOperationException(
                "Milvus 数据库隔离模式在当前 SDK 版本不可用。\n"
                        + "原因：milvus-sdk-java 2.4.5 的 v1 客户端 MilvusServiceClient 不支持 useDatabase()。\n"
                        + "修复方式（二选一）：\n"
                        + "  1) 把 litchi.vector.milvus.isolation.strategy 改回 partition（默认，推荐）\n"
                        + "  2) 升级 milvus-sdk-java 到 2.5+ 并迁移到 io.milvus.v2.client.MilvusClientV2");
    }

    /**
     * 在每次涉及集合的 RPC 之前调用：保证 partition / database 已就绪。
     */
    private void resolveTenantIsolation() {
        // 始终跑——TenantContextHolder.getTenantId() == null 时走 _default 即可
        Isolation iso = milvusProps.getIsolation();
        if (iso.getStrategy() == Strategy.DATABASE) {
            String dbName = resolveDatabaseName();
            // "default" 是 Milvus 内置数据库，连接时默认就是它；无需切换
            if (!"default".equals(dbName)) {
                activateDatabase(dbName);
            }
        }
    }

    /**
     * 取当前激活的 gRPC 客户端。关闭时抛 {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     */
    private MilvusServiceClient milvusClient() {
        MilvusServiceClient c = milvusClientProvider.getIfAvailable();
        if (c == null) {
            throw new com.lz.framework.vector.core.exception.VectorDisabledException("milvusClient");
        }
        return c;
    }

    /**
     * 取当前激活的特征提取器。关闭时抛 {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     */
    private FeatureExtractor featureExtractor() {
        FeatureExtractor f = featureExtractorProvider.getIfAvailable();
        if (f == null) {
            throw new com.lz.framework.vector.core.exception.VectorDisabledException("featureExtractor");
        }
        return f;
    }

    /**
     * 初始化：解析集合名 + 创建集合（如果不存在或 schema 不兼容则重建）。
     *
     * <p>关闭时（{@code litchi.vector.enable=false}）：{@link FeatureExtractor} 和
     * {@link MilvusServiceClient} 都不会被注册，{@link #initCollection()} 直接返回，
     * 不做任何 Milvus 调用 —— 业务方注入本类时不会阻塞启动。
     */
    @PostConstruct
    public void initCollection() {
        FeatureExtractor fe = featureExtractorProvider.getIfAvailable();
        if (fe == null) {
            log.warn("[MilvusService] FeatureExtractor 未注册（litchi.vector.enable=false），跳过集合初始化。"
                    + "运行时调用将抛 VectorDisabledException。");
            return;
        }
        MilvusServiceClient client = milvusClientProvider.getIfAvailable();
        if (client == null) {
            log.warn("[MilvusService] MilvusServiceClient 未注册（litchi.vector.enable=false），跳过集合初始化。");
            return;
        }

        // 集合名初始化：先用配置的 base name
        this.collectionName = milvusProps.getCollectionName();
        // 动态集合名：开启时把 collectionName 拼上当前模型名，实现多模型数据隔离
        if (milvusProps.isDynamicCollection()) {
            String model = fe.getModelName();
            String resolved = collectionName + "_" + model;
            log.info("[动态集合名] 开关已开启: base='{}' + model='{}' -> '{}'", collectionName, model, resolved);
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
            // partition 模式下预创建 _default 分区，避免首次 access 时多一次 RPC 阻塞
            // （无论是否启用多租户，_default 分区都建议预创建）
            if (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION) {
                ensurePartition("_default");
            }
            log.info("Milvus collection '{}' initialized successfully", collectionName);
        } catch (Exception e) {
            log.warn("Failed to initialize Milvus collection: {}", e.getMessage());
        }
    }

    /**
     * 探测集合 schema 是否与当前配置兼容。
     * 1. 能查询（主键字段存在）
     * 2. 向量维度与 featureExtractor 实际输出一致
     */
    private boolean isSchemaCompatible() {
        try {
            // 1. 检查能否查询（主键字段存在）
            R<io.milvus.grpc.QueryResults> r = milvusClient().query(
                    QueryParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withOutFields(Collections.singletonList(PRIMARY_KEY_FIELD))
                            .withLimit(1L)
                            .build()
            );
            if (r.getStatus() != R.Status.Success.getCode()) {
                log.warn("[Schema检查] 查询失败，判定不兼容: {}", r.getMessage());
                return false;
            }

            // 2. 校验向量维度
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

    /** 查询集合中向量字段的实际维度（通过 gRPC 原生 API） */
    private int getCollectionVectorDimension() {
        try {
            R<io.milvus.grpc.DescribeCollectionResponse> r = milvusClient().describeCollection(
                    DescribeCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (r.getStatus() != R.Status.Success.getCode()) return -1;
            CollectionSchema schema = r.getData().getSchema();
            for (FieldSchema field : schema.getFieldsList()) {
                if (field.getName().equals(FEATURE_VECTOR_FIELD)) {
                    for (KeyValuePair kvp : field.getTypeParamsList()) {
                        if ("dim".equals(kvp.getKey())) {
                            return Integer.parseInt(kvp.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[Schema检查] 获取集合维度失败: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * 向量模块是否已启用。
     * <p>关闭时（{@code litchi.vector.enable=false}）：{@link FeatureExtractor} / {@link MilvusServiceClient}
     * 都不会被注册，本方法返回 false —— 调用方入口处的 {@link #checkEnabled(String)} 会抛
     * {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     */
    public boolean isEnabled() {
        return milvusClientProvider.getIfAvailable() != null
                && featureExtractorProvider.getIfAvailable() != null;
    }

    /**
     * 入口校验：未启用时抛 {@link com.lz.framework.vector.core.exception.VectorDisabledException}。
     * 各 public 方法在入口处调用，业务方也可在调用前预检避免异常路径。
     */
    public void checkEnabled(String operation) {
        if (!isEnabled()) {
            throw new com.lz.framework.vector.core.exception.VectorDisabledException(operation);
        }
    }

    /**
     * 检查集合是否存在
     */
    public boolean collectionExists() {
        try {
            R<Boolean> response = milvusClient().hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            return response.getData();
        } catch (Exception e) {
            log.error("Error checking collection existence", e);
            return false;
        }
    }

    /**
     * 创建集合
     */
    public void createCollection() {
        // schema 字段：id + image_path + feature_vector + file_id + tenant_id + create_time
        // MilvusService 知道这些是协议字段，但不解释 imagePath 的业务含义、不读这个文件。
        List<FieldType> fields = new ArrayList<>();

        // 主键字段（业务生成：图片名+时间戳，由编排层传入）
        fields.add(FieldType.newBuilder()
                .withName(PRIMARY_KEY_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(200)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build());

        // 业务字段：图片路径（字符串，MilvusService 不解析、不读文件）
        fields.add(FieldType.newBuilder()
                .withName(IMAGE_PATH_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(500)
                .build());

        // 向量字段
        fields.add(FieldType.newBuilder()
                .withName(FEATURE_VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(featureExtractor().getFeatureDim())
                .build());

        // 业务字段：关联的 infra_file.id（0L 表示"无关联文件"，如目录导入场景）
        // 注：milvus-sdk-java 2.4.5 的 FieldType.Builder 没有 withNullable(boolean)（该方法在 2.5.0 才加入），
        //    且 ParamUtils.checkFieldData 对 Int64 列的实现是 if (!(value instanceof Long))，
        //    null 必被拒。因此在 2.4.5 下只能约定 0L 作为"无关联"哨兵值（业务方在查询侧需把 0L 当作 null 处理）。
        //    升级到 2.5+ SDK 后，FieldType.Builder.withNullable(true) 可用且 checkFieldData 修复了 nullable 校验，
        //    即可把哨兵换回真正的 null。
        fields.add(FieldType.newBuilder()
                .withName(FILE_ID_FIELD)
                .withDataType(DataType.Int64)
                .build());

        // 业务字段：租户编号（由 MilvusService 内部按线程上下文填充；0L 表示"无租户上下文"或"显式 ignore"）
        // 同上：2.4.5 SDK 不支持 nullable，约定 0L 作为哨兵。
        fields.add(FieldType.newBuilder()
                .withName(TENANT_ID_FIELD)
                .withDataType(DataType.Int64)
                .build());

        // 业务字段：入库时间戳（毫秒，Int64）
        fields.add(FieldType.newBuilder()
                .withName(CREATE_TIME_FIELD)
                .withDataType(DataType.Int64)
                .build());

        // 2. 创建集合参数
        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Image search collection - vector + business metadata")
                .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                .addFieldType(fields.get(0))
                .addFieldType(fields.get(1))
                .addFieldType(fields.get(2))
                .addFieldType(fields.get(3))
                .addFieldType(fields.get(4))
                .addFieldType(fields.get(5))
                .build();

        // 3. 执行创建
        R<RpcStatus> response = milvusClient().createCollection(createCollectionParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create collection: " + response.getMessage());
        }

        // 4. 创建索引
        createIndex();

        // 5. 预加载，使索引立即可用（空集合也安全）
        try {
            milvusClient().loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Load after createCollection failed (non-fatal): {}", e.getMessage());
        }

        log.info("Collection '{}' created successfully with dimension {}",
                collectionName, featureExtractor().getFeatureDim());
    }

    /**
     * 创建向量字段索引
     * HNSW：基于图的近似最近邻搜索，召回率高，适合百万级数据
     * 参数说明：
     *   M=16：每层最多 16 个邻居，控制召回率与内存 trade-off
     *   efConstruction=200：构建时动态候选列表大小，越大越准但越慢
     */
    public void createIndex() {
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(FEATURE_VECTOR_FIELD)
                .withIndexType(IndexType.HNSW)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"M\":16,\"efConstruction\":200}")
                .build();

        milvusClient().createIndex(indexParam);
        log.info("HNSW index created for field '{}' (M=16, efConstruction=200)", FEATURE_VECTOR_FIELD);
    }

    /**
     * 插入单条向量记录到 Milvus。
     *
     * <p>MilvusService 看到这个 {@link VectorRecord} 实体，把它整体持久化到 Milvus；
     * 不解析 imagePath、不读文件、不解释业务语义——只负责 CRUD。
     *
     * @param record 业务组装好的向量记录实体
     */
    public String insertVector(VectorRecord record) throws Exception {
        long t0 = System.currentTimeMillis();
        if (record == null || record.getId() == null || record.getId().isEmpty()) {
            throw new IllegalArgumentException("记录 id 不可为空");
        }
        if (record.getVector() == null || record.getVector().length == 0) {
            throw new IllegalArgumentException("记录向量不可为空");
        }
        // 注意：MilvusService 不"兜底" tenant_id —— 谁传谁负责。
        // 调用方（ImageIndexService）已经直接从 TenantContextHolder 读好值塞进 VectorRecord.tenantId。
        // 业务层传 null 时，fileIdOrSentinel 会把 tenant_id 落成 0L 哨兵。

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        InsertParam.Builder builder = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                        new InsertParam.Field(PRIMARY_KEY_FIELD, Collections.singletonList(record.getId())),
                        new InsertParam.Field(IMAGE_PATH_FIELD, Collections.singletonList(record.getImagePath())),
                        new InsertParam.Field(FEATURE_VECTOR_FIELD,
                                Collections.singletonList(floatArrayToList(record.getVector()))),
                        new InsertParam.Field(FILE_ID_FIELD,
                                Collections.singletonList(fileIdOrSentinel(record.getFileId()))),
                        new InsertParam.Field(TENANT_ID_FIELD,
                                Collections.singletonList(fileIdOrSentinel(record.getTenantId()))),
                        new InsertParam.Field(CREATE_TIME_FIELD,
                                Collections.singletonList(record.getCreateTime()))
                ));
        if (partition != null) builder.withPartitionName(partition);

        R<MutationResult> response = milvusClient().insert(builder.build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to insert vector: " + response.getMessage());
        }
        ensureCollectionLoaded();
        log.info("[单条入库] id={} | 耗时 {}ms", record.getId(), System.currentTimeMillis() - t0);
        return record.getId();
    }

    /**
     * 批量插入向量记录。
     *
     * <p>职责：本方法只负责把外部准备好的 {@link VectorRecord} 列表写入 Milvus。
     * <ul>
     *   <li>不调特征提取——record.vector 由编排层填好</li>
     *   <li>不生成 ID / imagePath / createTime——这些由编排层填好</li>
     *   <li>不解析业务字段——record 是什么就存什么</li>
     *   <li>默认按 {@link MilvusProperties#getInsertBatchSize()} 拆批打 RPC</li>
     * </ul>
     */
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
        int lastPct = -1;

        // 同样地：批量场景下也不再"兜底" tenant_id。
        // 谁传谁负责；缺省走 0L 哨兵（由 fileIdOrSentinel 翻译）。

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[批量入库] 开始: 总 {} 条 / 批 {} 条 | 模型: {} | partition={}",
                total, batchSizeEff, featureExtractor().getModelName(), partition);

        for (int start = 0; start < total; start += batchSizeEff) {
            int end = Math.min(start + batchSizeEff, total);
            int subSize = end - start;

            // 1) 组装本批的 fields
            List<String> subIds = new ArrayList<>(subSize);
            List<String> subPaths = new ArrayList<>(subSize);
            List<List<Float>> subVecs = new ArrayList<>(subSize);
            List<Long> subFileIds = new ArrayList<>(subSize);
            List<Long> subTenantIds = new ArrayList<>(subSize);
            List<Long> subCreateTimes = new ArrayList<>(subSize);
            for (int k = start; k < end; k++) {
                VectorRecord r = records.get(k);
                if (r == null || r.getVector() == null || r.getVector().length == 0) continue;
                subIds.add(r.getId());
                subPaths.add(r.getImagePath());
                subVecs.add(floatArrayToList(r.getVector()));
                subFileIds.add(fileIdOrSentinel(r.getFileId()));
                subTenantIds.add(fileIdOrSentinel(r.getTenantId()));
                subCreateTimes.add(r.getCreateTime());
            }
            if (subIds.isEmpty()) continue;

            // 2) 调 Milvus insert
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field(PRIMARY_KEY_FIELD, subIds));
            fields.add(new InsertParam.Field(IMAGE_PATH_FIELD, subPaths));
            fields.add(new InsertParam.Field(FEATURE_VECTOR_FIELD, subVecs));
            fields.add(new InsertParam.Field(FILE_ID_FIELD, subFileIds));
            fields.add(new InsertParam.Field(TENANT_ID_FIELD, subTenantIds));
            fields.add(new InsertParam.Field(CREATE_TIME_FIELD, subCreateTimes));

            InsertParam.Builder ipb = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields);
            if (partition != null) ipb.withPartitionName(partition);

            long rpcT0 = System.currentTimeMillis();
            R<MutationResult> resp = milvusClient().insert(ipb.build());
            long rpcMs = System.currentTimeMillis() - rpcT0;
            sumRpcMs += rpcMs;
            log.info("[批量入库] Milvus insert RPC: 本批 {} 条 | 耗时 {}ms ({}ms/条)",
                    subIds.size(), rpcMs, rpcMs / subIds.size());
            if (resp.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Milvus insert 失败 (本批 " + subIds.size() + " 条): " + resp.getMessage());
            }
            insertedIds.addAll(subIds);

            // 3) 进度日志：每 10% 一行
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

    /**
     * 批量插入向量记录（默认批次大小）。
     */
    public List<String> insertVectors(List<VectorRecord> records) throws Exception {
        return insertVectors(records, 0);
    }

    /**
     * 以图搜图（直接传向量，跳过特征提取步骤）
     * 用于查询图只活在内存里（上传的 MultipartFile 等）不想落盘的场景
     *
     * <p>返回结果包含 schema 字段（id / imagePath / createTime）+ 相似度。
     * 不返回 vector（搜索结果不需要向量本身，避免冗余传输）。
     */
    public List<SearchResult> searchByVector(float[] queryVector, int topK) throws Exception {
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("查询向量为空");
        }
        List<List<Float>> searchVectors = Collections.singletonList(floatArrayToList(queryVector));

        // 1.5 确保集合已加载（如果索引丢了，自动重建）
        ensureCollectionLoaded();

        resolveTenantIsolation();
        String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                ? resolvePartitionName() : null;
        if (partition != null) ensurePartition(partition);

        // 2. 构建搜索参数：把 schema 字段一起带回来供编排层用
        SearchParam.Builder spb = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName(FEATURE_VECTOR_FIELD)
                .withFloatVectors(searchVectors)
                .withTopK(topK)
                .withParams("{\"ef\":256}")
                .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                .withOutFields(Lists.newArrayList(
                        IMAGE_PATH_FIELD, FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD));
        if (partition != null) spb.withPartitionNames(Collections.singletonList(partition));

        // 3. 执行搜索
        R<io.milvus.grpc.SearchResults> response = milvusClient().search(spb.build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to search: " + response.getMessage());
        }

        // 4. 解析结果
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);

        List<SearchResult> results = new ArrayList<>();
        for (SearchResultsWrapper.IDScore score : scores) {
            SearchResult result = new SearchResult();
            result.setId(score.getStrID());
            float rawScore = score.getScore();
            result.setScore(rawScore);
            // COSINE ∈ [-1, 1] → 0~100%；归一化：(score + 1) / 2 ∈ [0, 1]
            float similarity = Math.clamp((rawScore + 1f) / 2f, 0f, 1f) * 100f;
            result.setSimilarity(String.format("%.2f%%", similarity));
            Object imagePath = score.get(IMAGE_PATH_FIELD);
            if (imagePath != null) result.setImagePath(imagePath.toString());
            Object fileId = score.get(FILE_ID_FIELD);
            if (fileId instanceof Number) result.setFileId(((Number) fileId).longValue());
            Object tenantId = score.get(TENANT_ID_FIELD);
            if (tenantId instanceof Number) result.setTenantId(((Number) tenantId).longValue());
            Object ts = score.get(CREATE_TIME_FIELD);
            if (ts instanceof Number) result.setCreateTime(((Number) ts).longValue());
            results.add(result);
        }

        return results;
    }

    /**
     * 删除集合
     */
    public void dropCollection() {
        milvusClient().dropCollection(
                DropCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
        log.info("Collection '{}' dropped", collectionName);
    }

    // ==================== 删除 ====================

    /**
     * 按主键列表删除向量记录。
     *
     * <p>Milvus 2.4.x SDK 仅支持 {@code pk_field in [...]} 形式的 expr 删除
     * （参见官方文档：{@code DeleteParam.withExpr} 的格式约束）。
     * 主键是 VarChar，所以列表里的 id 需要用双引号包起来、内部双引号反斜杠转义。
     *
     * @param ids 要删除的主键列表；空列表直接返回 0
     * @return 实际删除的记录数（取自 {@link MutationResult#getDeleteCnt()}）；失败返回 -1
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

            DeleteParam.Builder dpb = DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(expr.toString());
            if (partition != null) dpb.withPartitionName(partition);

            R<MutationResult> resp = milvusClient().delete(dpb.build());
            if (resp.getStatus() != R.Status.Success.getCode()) {
                log.warn("[deleteByIds] 失败: ids={} msg={}", ids, resp.getMessage());
                return -1;
            }
            int cnt = resp.getData() == null ? 0 : (int) resp.getData().getDeleteCnt();
            log.info("[deleteByIds] 删除 {} 条 (请求 {} 条)", cnt, ids.size());
            return cnt;
        } catch (Exception e) {
            log.warn("[deleteByIds] 异常: ids={}", ids, e);
            return -1;
        }
    }

    // ==================== 字段查询 / 条件查询 ====================

    /**
     * 按单个 id 查询单条记录（不带向量）。
     *
     * @param id 主键
     * @return 找到则返回记录；找不到返回 null
     */
    public QueryResult queryById(String id) {
        if (id == null || id.isEmpty()) return null;
        List<QueryResult> list = queryByCondition(
                QueryCondition.builder().eq(PRIMARY_KEY_FIELD, id).build(),
                false);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按单个 id 查询单条记录，可选是否带向量。
     */
    public QueryResult queryById(String id, boolean withVector) {
        if (id == null || id.isEmpty()) return null;
        List<QueryResult> list = queryByCondition(
                QueryCondition.builder().eq(PRIMARY_KEY_FIELD, id).build(),
                withVector);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按图片路径精确匹配查询记录（不带向量）。
     * 注意：image_path 在 schema 里没有建索引，数据量大时可能慢。
     */
    public List<QueryResult> queryByImagePath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return Collections.emptyList();
        return queryByCondition(
                QueryCondition.builder().eq(IMAGE_PATH_FIELD, imagePath).build(),
                false);
    }

    /**
     * 按图片路径模糊匹配（LIKE '%path%'），不带向量。
     */
    public List<QueryResult> queryByImagePathLike(String substring) {
        if (substring == null || substring.isEmpty()) return Collections.emptyList();
        return queryByCondition(
                QueryCondition.builder().contains(IMAGE_PATH_FIELD, substring).build(),
                false);
    }

    /**
     * 按图片路径前缀匹配（LIKE 'prefix%'），不带向量。
     */
    public List<QueryResult> queryByImagePathPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        return queryByCondition(
                QueryCondition.builder().startsWith(IMAGE_PATH_FIELD, prefix).build(),
                false);
    }

    /**
     * 按入库时间范围查询记录。
     *
     * @param fromInclusive 起始时间戳（毫秒，包含）；null 表示不限
     * @param toInclusive   结束时间戳（毫秒，包含）；null 表示不限
     */
    public List<QueryResult> queryByCreatedAtRange(Long fromInclusive, Long toInclusive) {
        QueryCondition.Builder b = QueryCondition.builder();
        if (fromInclusive != null) b.gte(CREATE_TIME_FIELD, fromInclusive);
        if (toInclusive != null) b.lte(CREATE_TIME_FIELD, toInclusive);
        return queryByCondition(b.build(), false);
    }

    /**
     * 按 {@link QueryCondition}（类型化条件构造器）查询多条记录。
     *
     * <p>默认不返回向量。如需向量请显式传 {@code withVector=true}。
     */
    public List<QueryResult> queryByCondition(QueryCondition condition) {
        return queryByCondition(condition, false);
    }

    /**
     * 按 {@link QueryCondition} 查询，可选是否返回向量。
     */
    public List<QueryResult> queryByCondition(QueryCondition condition, boolean withVector) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("QueryCondition 不可为空且至少包含一个条件");
        }
        return executeFieldQuery(condition.toExpr(), withVector, -1);
    }

    /**
     * 按 {@link QueryCondition} 查询并限制返回条数。
     */
    public List<QueryResult> queryByCondition(QueryCondition condition, boolean withVector, int limit) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("QueryCondition 不可为空且至少包含一个条件");
        }
        if (limit <= 0) throw new IllegalArgumentException("limit 必须 > 0");
        return executeFieldQuery(condition.toExpr(), withVector, limit);
    }

    /**
     * 直接传原始 Milvus expr 字符串查询（不走白名单）。
     *
     * <p>适用场景：QueryCondition 表达不了的复杂查询（OR、子查询、JSON 字段等）。
     * <b>注意</b>：本方法不做 expr 注入防护，调用方需自行保证 expr 安全。
     */
    public List<QueryResult> queryByRawExpr(String expr) {
        return queryByRawExpr(expr, false, -1);
    }

    public List<QueryResult> queryByRawExpr(String expr, boolean withVector, int limit) {
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("expr 不可为空");
        }
        return executeFieldQuery(expr.trim(), withVector, limit);
    }

    /**
     * 内部：实际执行 expr 查询并解析为 QueryResult。
     *
     * @param expr        Milvus 过滤表达式
     * @param withVector  是否把向量字段带回
     * @param limit       返回条数上限；<=0 表示不限制
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
        if (withVector) {
            outFields.add(FEATURE_VECTOR_FIELD);
        }

        try {
            resolveTenantIsolation();
            String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                    ? resolvePartitionName() : null;
            if (partition != null) ensurePartition(partition);

            QueryParam.Builder qb = QueryParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withOutFields(outFields)
                    .withExpr(expr);
            if (limit > 0) {
                qb.withLimit((long) limit);
            }
            if (partition != null) qb.withPartitionNames(Collections.singletonList(partition));

            R<io.milvus.grpc.QueryResults> qr = milvusClient().query(qb.build());
            if (qr.getStatus() != R.Status.Success.getCode()) {
                log.warn("[executeFieldQuery] 失败 expr='{}' : {}", expr, qr.getMessage());
                return Collections.emptyList();
            }
            QueryResultsWrapper qw = new QueryResultsWrapper(qr.getData());
            List<QueryResult> results = new ArrayList<>();
            for (QueryResultsWrapper.RowRecord row : qw.getRowRecords()) {
                QueryResult r = new QueryResult();
                Object idObj = row.get(PRIMARY_KEY_FIELD);
                if (idObj != null) r.setId(idObj.toString());
                Object pathObj = row.get(IMAGE_PATH_FIELD);
                if (pathObj != null) r.setImagePath(pathObj.toString());
                Object fidObj = row.get(FILE_ID_FIELD);
                if (fidObj instanceof Number) r.setFileId(((Number) fidObj).longValue());
                Object tidObj = row.get(TENANT_ID_FIELD);
                if (tidObj instanceof Number) r.setTenantId(((Number) tidObj).longValue());
                Object tsObj = row.get(CREATE_TIME_FIELD);
                if (tsObj instanceof Number) r.setCreateTime(((Number) tsObj).longValue());
                if (withVector) {
                    Object vecObj = row.get(FEATURE_VECTOR_FIELD);
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
        } catch (Exception e) {
            log.warn("[executeFieldQuery] 异常 expr='{}' : {}", expr, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 按 id 列表查询完整的 {@link VectorRecord} 实体（含 imagePath、vector、createTime）。
     *
     * <p>典型用法：搜索后编排层拿 id 列表 → 调这个方法拿到 imagePath → 拼装业务结果回显。
     *
     * @param ids 要查询的主键列表
     * @return 与 ids 对齐的 record 列表；找不到的 id 对应 record 为 null
     */
    public List<VectorRecord> queryByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        // 用 expr 表达 "id in [...]"——Milvus Java SDK 的 QueryParam 没有 withIds
        // 字符串值需双引号包起来，内部双引号需转义
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

            QueryParam.Builder qpb = QueryParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withOutFields(Arrays.asList(
                            PRIMARY_KEY_FIELD,
                            IMAGE_PATH_FIELD,
                            FEATURE_VECTOR_FIELD,
                            FILE_ID_FIELD,
                            TENANT_ID_FIELD,
                            CREATE_TIME_FIELD))
                    .withExpr(expr.toString());
            if (partition != null) qpb.withPartitionNames(Collections.singletonList(partition));

            R<io.milvus.grpc.QueryResults> qr = milvusClient().query(qpb.build());
            if (qr.getStatus() != R.Status.Success.getCode()) {
                log.warn("[queryByIds] 失败: {}", qr.getMessage());
                return Collections.emptyList();
            }
            QueryResultsWrapper qw = new QueryResultsWrapper(qr.getData());
            Map<String, VectorRecord> byId = new HashMap<>();
            for (QueryResultsWrapper.RowRecord row : qw.getRowRecords()) {
                Object idObj = row.get(PRIMARY_KEY_FIELD);
                if (idObj == null) continue;
                String id = idObj.toString();
                VectorRecord rec = new VectorRecord();
                rec.setId(id);
                Object pathObj = row.get(IMAGE_PATH_FIELD);
                rec.setImagePath(pathObj == null ? null : pathObj.toString());
                Object vecObj = row.get(FEATURE_VECTOR_FIELD);
                if (vecObj instanceof List<?> vecList) {
                    float[] arr = new float[vecList.size()];
                    for (int i = 0; i < vecList.size(); i++) {
                        Object v = vecList.get(i);
                        arr[i] = (v instanceof Number) ? ((Number) v).floatValue() : 0f;
                    }
                    rec.setVector(arr);
                }
                Object fidObj = row.get(FILE_ID_FIELD);
                if (fidObj instanceof Number) rec.setFileId(((Number) fidObj).longValue());
                Object tidObj = row.get(TENANT_ID_FIELD);
                if (tidObj instanceof Number) rec.setTenantId(((Number) tidObj).longValue());
                Object tsObj = row.get(CREATE_TIME_FIELD);
                if (tsObj instanceof Number) {
                    rec.setCreateTime(((Number) tsObj).longValue());
                }
                byId.put(id, rec);
            }
            // 按输入 ids 顺序对齐返回
            List<VectorRecord> out = new ArrayList<>(ids.size());
            for (String id : ids) out.add(byId.get(id));
            return out;
        } catch (Exception e) {
            log.warn("[queryByIds] 异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取集合信息
     */
    public Map<String, Object> getCollectionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("collectionName", collectionName);
        info.put("dimension", featureExtractor().getFeatureDim());
        info.put("exists", collectionExists());
        return info;
    }

    /**
     * 获取集合统计信息（行数 + 样本图片）
     */
    public Map<String, Object> getCollectionStats(int sampleSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("collectionName", collectionName);
        result.put("exists", collectionExists());

        if (!collectionExists()) {
            result.put("rowCount", 0L);
            result.put("samples", Collections.emptyList());
            return result;
        }

        // 先保证集合被加载，避免 cold collection 时 query 失败
        try {
            ensureCollectionLoaded();
        } catch (Exception e) {
            log.warn("ensureCollectionLoaded failed: {}", e.getMessage());
            result.put("samples", Collections.emptyList());
            result.putIfAbsent("rowCount", 0L);
            return result;
        }

        long rowCount = 0;
        try {
            resolveTenantIsolation();
            String partition = (milvusProps.getIsolation().getStrategy() == Strategy.PARTITION)
                    ? resolvePartitionName() : null;
            if (partition != null) ensurePartition(partition);

            QueryParam.Builder qb = QueryParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withOutFields(Lists.newArrayList(
                            PRIMARY_KEY_FIELD, IMAGE_PATH_FIELD,
                            FILE_ID_FIELD, TENANT_ID_FIELD, CREATE_TIME_FIELD))
                    .withLimit((long) sampleSize);
            if (partition != null) qb.withPartitionNames(Collections.singletonList(partition));

            R<io.milvus.grpc.QueryResults> qr = milvusClient().query(qb.build());
            if (qr.getStatus() == R.Status.Success.getCode()) {
                QueryResultsWrapper qw = new QueryResultsWrapper(qr.getData());
                List<QueryResultsWrapper.RowRecord> rows = qw.getRowRecords();
                List<Map<String, Object>> samples = new ArrayList<>();
                for (QueryResultsWrapper.RowRecord row : rows) {
                    Map<String, Object> item = new HashMap<>();
                    Object idObj = row.get(PRIMARY_KEY_FIELD);
                    Object pathObj = row.get(IMAGE_PATH_FIELD);
                    Object fidObj = row.get(FILE_ID_FIELD);
                    Object tidObj = row.get(TENANT_ID_FIELD);
                    Object tsObj = row.get(CREATE_TIME_FIELD);
                    item.put("id", idObj == null ? "" : idObj.toString());
                    item.put("imagePath", pathObj == null ? null : pathObj.toString());
                    if (fidObj instanceof Number) {
                        item.put("fileId", ((Number) fidObj).longValue());
                    }
                    if (tidObj instanceof Number) {
                        item.put("tenantId", ((Number) tidObj).longValue());
                    }
                    if (tsObj instanceof Number) {
                        item.put("createTime", ((Number) tsObj).longValue());
                    }
                    samples.add(item);
                }
                result.put("samples", samples);
            } else {
                result.put("samples", Collections.emptyList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch samples: {}", e.getMessage());
            result.put("samples", Collections.emptyList());
        }

        // 行数：尝试拿统计
        try {
            R<io.milvus.grpc.GetCollectionStatisticsResponse> stats = milvusClient().getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (stats.getStatus() == R.Status.Success.getCode()) {
                for (KeyValuePair kv : stats.getData().getStatsList()) {
                    if ("row_count".equals(kv.getKey())) {
                        rowCount = Long.parseLong(kv.getValue());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch row count: {}", e.getMessage());
        }
        result.put("rowCount", rowCount);
        return result;
    }

    /**
     * 确保集合已被加载到内存。如果遇到索引丢失，会自动重建索引并重新加载。
     * 供编排层（如 ImageIndexService）在批量入库前显式调用。
     */
    public void ensureCollectionLoaded() {
        try {
            R<RpcStatus> r = milvusClient().loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (r.getStatus() == R.Status.Success.getCode()) {
                return;
            }
            log.warn("loadCollection returned non-success: {} — will rebuild index", r.getMessage());
        } catch (Exception e) {
            log.warn("loadCollection failed ({}), attempting to rebuild index", e.getMessage());
        }

        // 索引可能丢了 → 重建索引并加载
        try {
            milvusClient().dropIndex(
                    io.milvus.param.index.DropIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withIndexName(FEATURE_VECTOR_FIELD)
                            .build()
            );
        } catch (Exception ignore) {
        }
        try {
            createIndex();
        } catch (Exception e) {
            log.error("Failed to rebuild index: {}", e.getMessage());
            throw new RuntimeException("Failed to rebuild Milvus index: " + e.getMessage(), e);
        }
        // createIndex 是异步的，需要 flush 等索引构建完才能 load
        try {
            milvusClient().flush(
                    FlushParam.newBuilder()
                            .withCollectionNames(Collections.singletonList(collectionName))
                            .build()
            );
        } catch (Exception e) {
            log.warn("Flush after rebuild index failed (non-fatal): {}", e.getMessage());
        }
        try {
            milvusClient().loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            log.info("Collection '{}' rebuilt index and reloaded", collectionName);
        } catch (Exception e) {
            log.error("Reload after rebuild failed: {}", e.getMessage());
            throw new RuntimeException("Failed to reload Milvus collection: " + e.getMessage(), e);
        }
    }

    // ============ 辅助方法 ============

    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }
}
