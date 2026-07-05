package com.lz.framework.vector.core.vector;

import cn.hutool.core.util.IdUtil;
import com.lz.framework.vector.config.EmbeddingProperties;
import com.lz.framework.vector.config.MilvusProperties;
import com.lz.framework.vector.core.exception.VectorDisabledException;
import com.lz.framework.vector.core.feature.FeatureExtractor;
import com.lz.framework.vector.core.feature.FeatureService;
import com.lz.framework.vector.core.isolation.VectorTenantContext;
import com.lz.framework.vector.core.milvus.MilvusService;
import com.lz.framework.vector.core.pojo.QueryCondition;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.framework.vector.core.pojo.VectorRecord;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图片索引编排服务：协调文件存储、特征提取、向量入库。
 *
 * <p>分层职责：
 * <ul>
 *   <li>{@link FeatureService}    ：按模型路由到 {@link FeatureExtractor} 提取向量</li>
 *   <li>{@link MilvusService}     ：纯 CRUD——接收 {@link VectorRecord} 实体、返回 {@link VectorRecord} / {@link SearchResult}。
 *     MilvusService 知道 schema 字段名（id/image_path/feature_vector/file_id/tenant_id/create_time）但<b>不读文件</b>、<b>不解析业务字段语义</b>。</li>
 *   <li><b>本类</b>                  ：组合上面三者，把"业务数据"组装成 {@link VectorRecord} 交给 MilvusService 持久化；
 *     对外暴露统一的业务入口。</li>
 * </ul>
 *
 * <p>扩展性：换特征提取模型？改 yml 的 {@code embedding.model}。
 * 换向量库？重写 MilvusService，本类签名不变（依赖接口）。
 * 换文件存储后端？重写 FileStorageService，本类签名不变。
 */
@Slf4j
public class ImageIndexService {

    private final FeatureService featureService;
    private final MilvusService milvusService;
    private final EmbeddingProperties embeddingProps;
    private final MilvusProperties milvusProps;
    /**
     * 多租户上下文解析器（懒注入）。
     * <p>
     * 与 {@code litchi-spring-boot-starter-redis / job / mq} 完全一致：
     * 本模块只声明接口，由 {@code litchi-spring-boot-starter-biz-tenant}
     * 反向依赖 vector 后提供基于 {@code TenantContextHolder} 的实现。
     * 没引 biz-tenant 时本字段拿到的是 vector 模块注册的空 Optional，
     * 读不到值，{@link #currentTenantId()} 返回 null —— MilvusService 走 {@code _default}。
     */
    private final ObjectProvider<VectorTenantContext> tenantContextProvider;

    public ImageIndexService(FeatureService featureService,
                             MilvusService milvusService,
                             EmbeddingProperties embeddingProps,
                             MilvusProperties milvusProps,
                             ObjectProvider<VectorTenantContext> tenantContextProvider) {
        this.featureService = featureService;
        this.milvusService = milvusService;
        this.embeddingProps = embeddingProps;
        this.milvusProps = milvusProps;
        this.tenantContextProvider = tenantContextProvider;
    }

    /** 特征提取专用线程池（懒初始化，按 embedding.feature-threads 配置） */
    private volatile ExecutorService featureExecutor;

    /** 上传目录，与 FileStorageService 的 UPLOAD_DIR 保持一致 */
    private static final String UPLOAD_DIR = "images";

    private static final Set<String> IMG_EXT = Set.of("jpg", "jpeg", "png", "webp", "bmp", "gif");

    // ==================== 索引 =====================
    public String index(String fileUrl, byte[] content) throws Exception {
        return index(fileUrl, content, null);
    }

    /**
     * @param fileId 可选：上传场景时是 {@code infra_file.id}；目录导入为 null
     */
    public String index(String fileUrl, byte[] content, Long fileId) throws Exception {
        float[] vector = featureService.extract(content);
        String id = IdUtil.fastSimpleUUID();
        VectorRecord record = new VectorRecord(
                id, fileUrl, vector,
                fileId,
                currentTenantId(),
                System.currentTimeMillis()  // createTime
        );
        return milvusService.insertVector(record);
    }

    /**
     * 从 {@link VectorTenantContext}（由 biz-tenant 模块提供实现）读取当前线程租户编号。
     * <ul>
     *   <li>未启用多租户（没引 biz-tenant 或没启用 {@code litchi.tenant.enable=true}）→ 返回 {@code null}（MilvusService 走 0L 哨兵 + {@code _default} 分区）</li>
     *   <li>{@code @TenantIgnore} → 返回 {@code -1L}（MilvusService 同样识别为 ignore）</li>
     *   <li>正常请求 → 返回真实租户 ID（MilvusService 走 {@code tnt_{tenantId}} 隔离）</li>
     * </ul>
     *
     * <p>与 {@code TenantRedisMessageInterceptor} / {@code TenantRocketMQSendMessageHook}
     * 的姿势完全一致 —— 都是"上游 starter 声明接口、biz-tenant 反向依赖提供实现"。
     */
    private Long currentTenantId() {
        VectorTenantContext ctx = tenantContextProvider.getIfAvailable();
        if (ctx == null) {
            return null;
        }
        return ctx.currentTenantId();
    }

    // ==================== 批量索引 ====================

    /**
     * 从已落盘的路径批量索引。
     */
    public List<String> indexBatch(List<String> imagePaths) throws Exception {
        return indexBatch(imagePaths, milvusProps.getInsertBatchSize(), null);
    }

    /**
     * 从已落盘的路径批量索引，指定批次大小。
     * <p>fileId 默认传 null（无关联 infra_file），目录导入场景适用。
     */
    public List<String> indexBatch(List<String> imagePaths, int batchSize) throws Exception {
        return indexBatch(imagePaths, batchSize, null);
    }

    /**
     * 从已落盘的路径批量索引，指定批次大小和 fileId。
     *
     * @param fileId 可选：所有记录共享同一 fileId（业务场景通常如此）；null 表示目录导入
     */
    public List<String> indexBatch(List<String> imagePaths, int batchSize, Long fileId) throws Exception {
        checkEnabled("indexBatch");
        if (imagePaths == null || imagePaths.isEmpty()) {
            log.info("[批量索引] 0 张，跳过");
            return Collections.emptyList();
        }

        long t0 = System.currentTimeMillis();
        int total = imagePaths.size();
        FeatureExtractor fe = featureService.getExtractor();
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[批量索引] 开始: 总 {} 张 | 模型: {} | 池: {}/{}",
                total, fe.getModelName(),
                fe.getSessionPoolSnapshot()[0], fe.getSessionPoolSnapshot()[0]);
        log.info("[批量索引] 线程池: core=max={}",
                ((ThreadPoolExecutor) ensureExecutor()).getMaximumPoolSize());

        // 1) 并行提特征
        List<float[]> vectors = new ArrayList<>(total);
        for (int i = 0; i < total; i++) vectors.add(null);
        CountDownLatch latch = new CountDownLatch(total);
        AtomicInteger done = new AtomicInteger(0);
        int progressEvery = Math.max(8, total / 8);

        for (int k = 0; k < total; k++) {
            final int idx = k;
            final File f = new File(imagePaths.get(k));
            ensureExecutor().submit(() -> {
                long tt0 = System.currentTimeMillis();
                try {
                    // 调用 extractFeature(File, ExecutorService) 把 featureExecutor 传进去，
                    // extractor 内部看到 "非 null 池子" 就走串行多尺度，避免与外层池互锁 + 减少 invokeAll 框架开销。
                    // 单图搜图场景直接调 extractFeature(File)（不传池）走 commonPool 并发加速。
                    float[] v = fe.extractFeature(f, ensureExecutor());
                    vectors.set(idx, v);
                    if (idx % progressEvery == 0 || idx == total - 1) {
                        log.info("[批量索引] 提特征进度 {}/{} | 最近单张 {}ms",
                                idx + 1, total, System.currentTimeMillis() - tt0);
                    }
                } catch (Throwable e) {
                    log.error("[批量索引] 单张提特征失败: {}", f, e);
                } finally {
                    done.incrementAndGet();
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[批量索引] 提特征被中断", ie);
        }
        long featMs = System.currentTimeMillis() - t0;
        log.info("[批量索引] 提特征完成: {}/{} 张 | 耗时 {}ms (平均 {}ms/张)",
                done.get(), total, featMs, done.get() == 0 ? 0 : featMs / done.get());

        // 2) 组装 VectorRecord 列表（业务组装）
        long now = System.currentTimeMillis();
        List<VectorRecord> records = new ArrayList<>(total);
        for (int k = 0; k < total; k++) {
            float[] v = vectors.get(k);
            if (v == null || v.length == 0) continue;
            String id = generateImageId(imagePaths.get(k));
            records.add(new VectorRecord(
                    id,
                    imagePaths.get(k),
                    v,
                    fileId,
                    currentTenantId(),
                    now));
        }
        if (records.isEmpty()) {
            log.warn("[批量索引] 全部失败，0 张入库");
            return Collections.emptyList();
        }

        // 3) 调 Milvus 入库
        List<String> inserted = milvusService.insertVectors(records, batchSize);
        log.info("[批量索引] 完成: 成功 {} / {} 张 | 总耗时 {}ms",
                inserted.size(), total, System.currentTimeMillis() - t0);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return inserted;
    }


    // ==================== 目录导入 ====================

    /**
     * 从本地目录批量导入图片：复制到 uploads → 提特征 → 入库。
     *
     * <p>去重策略：基于文件层——目标位置已有同名文件则跳过本张图片的入库。
     * 编排层不再去查 Milvus 端"哪些 id 已存在"——这是 Milvus 的纯向量语义，
     * 不该在导入路径里临时做业务去重。
     *
     * @param dir       要扫描的目录（绝对路径）
     * @param recursive 是否递归子目录
     * @param batchSize 每批多少张调一次 Milvus
     */
    public Map<String, Object> importFromDirectory(String dir, boolean recursive, int batchSize) throws Exception {
        File root = new File(dir);
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException("目录不存在: " + dir);
        }
        int batchSizeEff = batchSize <= 0 ? milvusProps.getInsertBatchSize() : batchSize;

        // 1) 扫描图片
        List<File> imageFiles = new ArrayList<>();
        collectImages(root, imageFiles, recursive);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[导入开始] 目录: {} | 递归: {} | 共 {} 张图片", dir, recursive, imageFiles.size());

        // 2) 文件层去重：复用已有同名文件，未命中才复制；都入库
        List<String> toIndex = new ArrayList<>();
        int skipped = 0;
        int failed = 0;
        File uploadDir = new File(UPLOAD_DIR);
        for (File f : imageFiles) {
            try {
                File destFile = new File(uploadDir, f.getName());
                if (destFile.exists() && destFile.length() > 0) {
                    skipped++;
                    log.info("[导入跳过复制] 复用已有文件: {}", f.getName());
                } else {
                    Files.copy(f.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.info("[导入复制] {} -> {}", f.getName(), f.getName());
                }
                toIndex.add(UPLOAD_DIR + "/" + f.getName());
            } catch (IOException e) {
                failed++;
                log.warn("[导入] 文件复制失败: {}", f.getAbsolutePath(), e);
            }
        }

        // 3) 入库
        long t0 = System.currentTimeMillis();
        List<String> ids = indexBatch(toIndex, batchSizeEff);
        long cost = System.currentTimeMillis() - t0;

        log.info("[导入完成] 共扫描:{} | 入库:{} | 跳过:{} | 失败:{} | 耗时:{}ms",
                imageFiles.size(), ids.size(), skipped, failed, cost);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("scanned", imageFiles.size());
        ret.put("inserted", ids.size());
        ret.put("skipped", skipped);
        ret.put("failed", failed);
        ret.put("durationMs", cost);
        ret.put("directory", dir);
        ret.put("recursive", recursive);
        return ret;
    }

    // ==================== 以图搜图编排 ====================

    /**
     * 以图搜图（已落盘的图片路径）。
     *
     * <p>编排层做的事：
     * <ol>
     *   <li>提特征（FeatureService）</li>
     *   <li>调 Milvus 纯向量搜索（MilvusService.searchByVector）</li>
     *   <li>直接返回 {@link SearchResult}——MilvusService 已把 schema 字段
     *       （image_path / create_time）一并带回，编排层不用二次反查</li>
     * </ol>
     */
    public List<SearchResult> searchByPath(String queryImagePath, int topK) throws Exception {
        checkEnabled("searchByPath");
        long t0 = System.currentTimeMillis();
        float[] vec = featureService.extract(new File(queryImagePath));
        List<SearchResult> results = milvusService.searchByVector(vec, topK);
        log.info("[以图搜图] 耗时 {}ms (提特征+Milvus搜索)", System.currentTimeMillis() - t0);
        return results;
    }

    /**
     * 以图搜图（输入流，如上传文件）。
     */
    public List<SearchResult> searchByStream(java.io.InputStream in, int topK) throws Exception {
        checkEnabled("searchByStream");
        long t0 = System.currentTimeMillis();
        float[] vec;
        try (java.io.InputStream closed = in) {
            vec = featureService.extract(closed, featureService.getDefaultModel());
        }
        List<SearchResult> results = milvusService.searchByVector(vec, topK);
        log.info("[以图搜图] 耗时 {}ms (提特征+Milvus搜索)", System.currentTimeMillis() - t0);
        return results;
    }

    // ==================== 字段查询 / 条件查询编排 ====================

    /**
     * 按 id 查单条记录（不带向量）。
     */
    public QueryResult queryById(String id) {
        return milvusService.queryById(id);
    }

    /**
     * 按 id 查单条记录，可选是否带向量。
     */
    public QueryResult queryById(String id, boolean withVector) {
        return milvusService.queryById(id, withVector);
    }

    /**
     * 按图片路径精确查记录。
     */
    public List<QueryResult> queryByImagePath(String imagePath) {
        return milvusService.queryByImagePath(imagePath);
    }

    /**
     * 按图片路径模糊查（LIKE '%path%'）。
     */
    public List<QueryResult> queryByImagePathLike(String substring) {
        return milvusService.queryByImagePathLike(substring);
    }

    /**
     * 按图片路径前缀查（LIKE 'prefix%'）。
     */
    public List<QueryResult> queryByImagePathPrefix(String prefix) {
        return milvusService.queryByImagePathPrefix(prefix);
    }

    /**
     * 按入库时间范围查记录。
     */
    public List<QueryResult> queryByCreatedAtRange(Long fromInclusive, Long toInclusive) {
        return milvusService.queryByCreatedAtRange(fromInclusive, toInclusive);
    }

    /**
     * 按类型化条件查询。
     */
    public List<QueryResult> queryByCondition(QueryCondition condition) {
        return milvusService.queryByCondition(condition);
    }

    public List<QueryResult> queryByCondition(QueryCondition condition, boolean withVector) {
        return milvusService.queryByCondition(condition, withVector);
    }

    public List<QueryResult> queryByCondition(QueryCondition condition, boolean withVector, int limit) {
        return milvusService.queryByCondition(condition, withVector, limit);
    }

    /**
     * 直接传原始 Milvus expr 字符串查询（不走白名单，调用方需自行保证 expr 安全）。
     */
    public List<QueryResult> queryByRawExpr(String expr) {
        return milvusService.queryByRawExpr(expr);
    }

    public List<QueryResult> queryByRawExpr(String expr, boolean withVector, int limit) {
        return milvusService.queryByRawExpr(expr, withVector, limit);
    }

    // ==================== 删除 ====================

    /**
     * 按 id 列表删除向量记录。
     *
     * <p>未启用时抛 {@link VectorDisabledException}。
     *
     * @param ids 主键列表
     * @return 实际删除的记录数
     */
    public int deleteByIds(List<String> ids) {
        checkEnabled("deleteByIds");
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return milvusService.deleteByIds(ids);
    }

    // ==================== 内部辅助 ====================

    /**
     * 生成业务图片 ID：图片名(去扩展名) + 时间戳。
     * 与历史 ID 生成规则保持一致，避免历史数据失效。
     */
    private String generateImageId(String imagePath) {
        String fileName = imagePath;
        int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (slash >= 0) fileName = fileName.substring(slash + 1);
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) fileName = fileName.substring(0, dot);
        fileName = fileName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        return fileName + "_" + ts;
    }

    /** 收集目录下所有图片（按文件名排序） */
    private void collectImages(File dir, List<File> out, boolean recursive) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (File f : files) {
            if (f.isDirectory()) {
                if (recursive) collectImages(f, out, recursive);
            } else if (f.length() > 0) {
                String name = f.getName().toLowerCase();
                int dot = name.lastIndexOf('.');
                if (dot > 0 && IMG_EXT.contains(name.substring(dot + 1))) {
                    out.add(f);
                }
            }
        }
    }

    /** 懒初始化特征提取线程池 */
    private ExecutorService ensureExecutor() {
        ExecutorService e = featureExecutor;
        if (e != null) return e;
        synchronized (this) {
            if (featureExecutor == null) {
                int n = Math.max(1, embeddingProps.getFeatureThreads());
                featureExecutor = Executors.newFixedThreadPool(n, runnable -> {
                    Thread t = new Thread(runnable, "image-index-feature-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                });
            }
            return featureExecutor;
        }
    }

    /** Spring 关闭时释放线程池 */
    @PreDestroy
    public void shutdown() {
        if (featureExecutor == null) return;
        featureExecutor.shutdown();
        try {
            if (!featureExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                featureExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            featureExecutor.shutdownNow();
        }
    }

    /**
     * 入口校验：未启用时抛 {@link VectorDisabledException}，提示用户先在 yml 开启 {@code litchi.vector.enable=true}。
     * 委托给 {@link FeatureService#isEnabled()}，因为 FeatureService 是其他三个 bean 都依赖的中心。
     */
    private void checkEnabled(String operation) {
        if (!featureService.isEnabled()) {
            throw new VectorDisabledException(operation);
        }
    }

    /**
     * 索引结果。
     */
    public record IndexResult(int inserted, int skipped, List<String> ids) {}
}
