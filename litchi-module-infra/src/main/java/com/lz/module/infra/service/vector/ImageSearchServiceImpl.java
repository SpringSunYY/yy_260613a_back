package com.lz.module.infra.service.vector;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.lz.framework.common.exception.ErrorCode;
import com.lz.framework.common.exception.util.ServiceExceptionUtil;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.vector.core.milvus.MilvusService;
import com.lz.framework.vector.core.pojo.QueryCondition;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.framework.vector.core.vector.ImageIndexService;
import com.lz.module.infra.controller.admin.file.vo.file.FileUploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.BatchUploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.UploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImagePageReqVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImageRespVO;
import com.lz.module.infra.enums.ErrorCodeConstants;
import com.lz.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static com.lz.framework.vector.core.vector.ImageIndexService.IMG_EXT;


/**
 * 以图搜图
 *
 * @Project: LZ-Order
 * @Author: YY
 * @CreateTime: 2026-07-05  18:13
 * @Version: 1.0
 */
@Service
@Slf4j
public class ImageSearchServiceImpl implements ImageSearchService {

    @Resource
    private ImageIndexService imageIndexService;

    @Resource
    private MilvusService milvusService;

    @Resource
    private FileService fileService;

    @Override
    public UploadRespVO uploadImage(String fileUrl, byte[] content) throws Exception {
        // 与 3 参重载保持单一入口；不传 fileId 表示"目录导入"或"无 infra_file 关联"语义，
        // 由 MilvusService 的 fileIdOrSentinel 翻译成 0L 哨兵。
        return uploadImage(fileUrl, content, null);
    }

    @Override
    public UploadRespVO uploadImage(String fileUrl, byte[] content, Long fileId) throws Exception {
        String id = imageIndexService.index(fileUrl, content, fileId);
        return UploadRespVO.builder()
                .id(id)
                .url(fileUrl)
                .build();
    }

    @Override
    public BatchUploadRespVO batchUpload(List<MultipartFile> files, String moduleType) throws Exception {
        BatchUploadRespVO.Builder resp = BatchUploadRespVO.builder()
                .total(files == null ? 0 : files.size());
        if (CollectionUtils.isEmpty(files)) {
            return resp.build();
        }
        // 一次性查所有原始文件名是否已存在 infra_file，避免逐文件 SELECT
        List<String> names = new ArrayList<>(files.size());
        for (MultipartFile f : files) {
            names.add(f.getOriginalFilename());
        }
        Set<String> existing = fileService.getExistingFileNames(names);

        // 逐文件：命中跳过，未命中走落盘 + 索引；单条失败不影响其他
        for (MultipartFile file : files) {
            String source = file.getOriginalFilename();
            if (file.isEmpty()) {
                resp.failed(BatchUploadRespVO.FailedItem.of(source,
                        resolveI18nMessage(ErrorCodeConstants.VECTOR_IMAGE_FILE_EMPTY)));
                continue;
            }
            if (existing.contains(source)) {
                resp.skipped(BatchUploadRespVO.SkippedItem.of(source, null));
                continue;
            }
            Long fileId = null;
            try {
                byte[] content = file.getBytes();
                // 先写文件日志：拿到 fileId 和真 url，再用 url 索引 → Milvus.imagePath 才是可访问的 URL
                FileUploadRespVO uploaded = fileService.createFile(content, source,
                        null, file.getContentType(), moduleType);
                fileId = uploaded.getId();
                UploadRespVO indexed = uploadImage(uploaded.getUrl(), content, uploaded.getId());
                resp.inserted(indexed);
            } catch (Exception e) {
                // 写向量失败 → 回滚日志（删 infra_file 行 + 文件存储），避免孤儿日志
                if (fileId != null) {
                    try { fileService.deleteFile(fileId); } catch (Exception ignore) {}
                }
                String msg = e.getMessage() == null ? "" : e.getMessage();
                ErrorCode code = msg.contains("读取失败") || msg.contains("extract")
                        ? ErrorCodeConstants.VECTOR_IMAGE_FEATURE_EXTRACT_FAILED
                        : ErrorCodeConstants.VECTOR_IMAGE_FILE_PROCESS_FAILED;
                resp.failed(BatchUploadRespVO.FailedItem.of(source,
                        resolveI18nMessage(code, msg)));
                log.warn("[batchUpload] 处理失败, source={}, err={}", source, msg);
            }
        }
        return resp.build();
    }

    @Override
    public BatchUploadRespVO uploadImagesByUrls(List<String> urls) throws Exception {
        BatchUploadRespVO.Builder resp = BatchUploadRespVO.builder()
                .total(urls == null ? 0 : urls.size());
        if (CollectionUtils.isEmpty(urls)) {
            return resp.build();
        }
        // 一次性查 URL 末段文件名是否已存在 infra_file
        List<String> fileNames = new ArrayList<>(urls.size());
        for (String url : urls) {
            fileNames.add(extractFileName(url));
        }
        Set<String> existing = fileService.getExistingFileNames(fileNames);

        for (String url : urls) {
            if (StrUtil.isBlank(url)) {
                resp.failed(BatchUploadRespVO.FailedItem.of(url,
                        resolveI18nMessage(ErrorCodeConstants.VECTOR_IMAGE_DIR_EMPTY)));
                continue;
            }
            String fileName = extractFileName(url);
            if (existing.contains(fileName)) {
                resp.skipped(BatchUploadRespVO.SkippedItem.of(url, null));
                continue;
            }
            byte[] content;
            try {
                content = HttpRequest.get(url).timeout(30000).execute().bodyBytes();
            } catch (Exception e) {
                resp.failed(BatchUploadRespVO.FailedItem.of(url,
                        resolveI18nMessage(ErrorCodeConstants.VECTOR_IMAGE_URL_DOWNLOAD_FAILED, e.getMessage())));
                continue;
            }
            Long fileId = null;
            try {
                FileUploadRespVO uploaded = fileService.createFile(content, fileName,
                        null, guessContentType(fileName), "infra");
                fileId = uploaded.getId();
                UploadRespVO indexed = uploadImage(uploaded.getUrl(), content, uploaded.getId());
                resp.inserted(indexed);
            } catch (Exception e) {
                if (fileId != null) {
                    try { fileService.deleteFile(fileId); } catch (Exception ignore) {}
                }
                String msg = e.getMessage() == null ? "" : e.getMessage();
                ErrorCode code = msg.contains("读取失败") || msg.contains("extract")
                        ? ErrorCodeConstants.VECTOR_IMAGE_FEATURE_EXTRACT_FAILED
                        : ErrorCodeConstants.VECTOR_IMAGE_FILE_PROCESS_FAILED;
                resp.failed(BatchUploadRespVO.FailedItem.of(url,
                        resolveI18nMessage(code, msg)));
                log.warn("[uploadImagesByUrls] 处理失败, url={}, err={}", url, msg);
            }
        }
        return resp.build();
    }

    @Override
    public BatchUploadRespVO importFromDirectory(String dir, boolean recursive) {
        if (StrUtil.isBlank(dir)) {
            return BatchUploadRespVO.builder()
                    .total(0)
                    .failed(BatchUploadRespVO.FailedItem.of(dir,
                            resolveI18nMessage(ErrorCodeConstants.VECTOR_IMAGE_DIR_EMPTY)))
                    .build();
        }
        File root = new File(dir);
        if (!root.exists() || !root.isDirectory()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.VECTOR_IMAGE_DIR_NOT_EXISTS, dir);
        }
        // 自己扫目录（不走 framework 的 importFromDirectory：那个版本不查 infra_file，按 name 重复入库）
        List<File> files;
        try {
            files = scanDir(root, recursive);
        } catch (Exception e) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.VECTOR_IMAGE_DIR_SCAN_FAILED, e.getMessage());
        }
        BatchUploadRespVO.Builder resp = BatchUploadRespVO.builder().total(files.size());
        int failedCount = 0;
        if (files.isEmpty()) {
            return resp.build();
        }
        // 一次性按 name 查 infra_file
        List<String> names = new ArrayList<>(files.size());
        for (File f : files) {
            names.add(f.getName());
        }
        Set<String> existing = fileService.getExistingFileNames(names);

        for (File f : files) {
            String name = f.getName();
            if (f.length() == 0) {
                resp.failed(BatchUploadRespVO.FailedItem.of(name,
                        resolveI18nMessage(ErrorCodeConstants.VECTOR_IMAGE_FILE_EMPTY)));
                failedCount++;
                continue;
            }
            if (existing.contains(name)) {
                resp.skipped(BatchUploadRespVO.SkippedItem.of(name, null));
                continue;
            }
            Long fileId = null;
            try {
                byte[] content = FileUtil.readBytes(f);
                FileUploadRespVO uploaded = fileService.createFile(content, name,
                        null, guessContentType(name), "infra");
                fileId = uploaded.getId();
                UploadRespVO indexed = uploadImage(uploaded.getUrl(), content, uploaded.getId());
                resp.inserted(indexed);
            } catch (Exception e) {
                if (fileId != null) {
                    try { fileService.deleteFile(fileId); } catch (Exception ignore) {}
                }
                String msg = e.getMessage() == null ? "" : e.getMessage();
                ErrorCode code = msg.contains("读取失败") || msg.contains("extract")
                        ? ErrorCodeConstants.VECTOR_IMAGE_FEATURE_EXTRACT_FAILED
                        : ErrorCodeConstants.VECTOR_IMAGE_FILE_PROCESS_FAILED;
                resp.failed(BatchUploadRespVO.FailedItem.of(name,
                        resolveI18nMessage(code, msg)));
                failedCount++;
                log.warn("[importFromDirectory] 处理失败, name={}, err={}", name, msg);
            }
        }
        int insertedCount = files.size() - existing.size() - failedCount;
        log.info("[importFromDirectory] 完成 total={}, scanned={}, skipped={}, failed={}, inserted={}",
                files.size(), files.size(), existing.size(), failedCount, insertedCount);
        return resp.build();
    }

    /**
     * 递归或非递归扫描目录，仅保留图片后缀。
     */
    private static List<File> scanDir(File root, boolean recursive) {
        List<File> out = new ArrayList<>();
        File[] children = root.listFiles();
        if (children == null) {
            return out;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                if (recursive) {
                    out.addAll(scanDir(c, true));
                }
                continue;
            }
            if (!c.isFile()) {
                continue;
            }
            String ext = FileUtil.extName(c).toLowerCase();
            if (IMG_EXT.contains(ext)) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * 把 ErrorCode 通过当前请求的 Accept-Language 解析成 message 后再塞进 resp。
     * 用项目自带的 ServiceException.message 通道，确保翻译口径与业务异常一致。
     */
    private static String resolveI18nMessage(ErrorCode errorCode, Object... params) {
        return ServiceExceptionUtil.exception(errorCode, params).getMessage();
    }

    /**
     * 从 URL 末段取文件名（去掉 query / hash）。失败返回 null。
     */
    private static String extractFileName(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        int h = path.indexOf('#');
        if (h >= 0) path = path.substring(0, h);
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return StrUtil.isBlank(name) ? null : name;
    }

    /**
     * 按文件名猜 Content-Type（仅图片）。简化处理，未识别走 application/octet-stream。
     */
    private static String guessContentType(String name) {
        if (name == null) return "application/octet-stream";
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    @Override
    public PageResult<VectorImageRespVO> getImagePage(VectorImagePageReqVO pageReqVO) {
        // 1. 组装类型化查询条件（仅白名单字段：id / image_path / create_time / file_id / tenant_id）
        QueryCondition.Builder builder = QueryCondition.builder();
        boolean hasCondition = false;
        if (StrUtil.isNotEmpty(pageReqVO.getId())) {
            builder.eq("id", pageReqVO.getId().trim());
            hasCondition = true;
        }
        if (StrUtil.isNotEmpty(pageReqVO.getImagePath())) {
            builder.contains("image_path", pageReqVO.getImagePath().trim());
            hasCondition = true;
        }
        if (pageReqVO.getFileId() != null) {
            builder.eq("file_id", pageReqVO.getFileId());
            hasCondition = true;
        }
        if (pageReqVO.getCreateTime() != null && pageReqVO.getCreateTime().length == 2) {
            if (pageReqVO.getCreateTime()[0] != null) {
                builder.gte("create_time", LocalDateTimeUtil.toEpochMilli(pageReqVO.getCreateTime()[0]));
                hasCondition = true;
            }
            if (pageReqVO.getCreateTime()[1] != null) {
                builder.lte("create_time", LocalDateTimeUtil.toEpochMilli(pageReqVO.getCreateTime()[1]));
                hasCondition = true;
            }
        }

        // 2. 没有条件时，构造一个永远为真的 expr（直接用主键大于空字符串）
        //    避免 QueryCondition 空校验导致 IllegalArgumentException
        QueryCondition cond;
        if (hasCondition) {
            cond = builder.build();
        } else {
            cond = QueryCondition.builder().gt("create_time", 0L).build();
        }

        // 3. Milvus 的 v1 SDK 不支持 offset（在 SDK 2.4.5 上 QueryParam 没有 withOffset），
        //    这里采用"条件全量拉取 → 内存 sort + slice"的假分页：
        //      - list.size() 即"过滤后真实总数" total；
        //      - 内存按 create_time desc 排序后做 [offset, offset+pageSize) 切片。
        //    优点：total 与 data 走同一 expr，天然一致，不会出现"全集 21 条 + 过滤后剩 3 条"，
        //          但 total 仍返回 21 这种错误。
        //    缺点：单次 query 把当前条件下的全集都拉回内存；大数据集需要靠 Milvus 数据分片
        //          或迁移到 MilvusClientV2 后再上真分页。
        int pageNo = Math.max(1, pageReqVO.getPageNo());
        int pageSize = pageReqVO.getPageSize() == null || pageReqVO.getPageSize() <= 0
                ? 10 : pageReqVO.getPageSize();
        long offset = (long) (pageNo - 1) * pageSize;

        // 走同一 expr 一次拉全集；list.size() 即过滤后的真实总数。
        // 一次性取全集（拉不动就截断）。limit 上限用 10_000 防 OOM，
        // 如果真实过滤后总数超过 10_000（实际场景极少见），需要切到分批累计或升级 SDK。
        final int HARD_LIMIT = 10_000;
        List<QueryResult> all = milvusService.queryByCondition(cond, false, HARD_LIMIT);
        if (all.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 【修 bug】之前 total = all.size()，但 all 已经按 fetchLimit(=offset+pageSize) 截断，
        // 导致 21 条 + pageSize=20 → total=20。现在 all 是条件全集，size 就是真实过滤后总数。
        long total = all.size();

        // 4. 按 create_time 倒序，再按 id 升序排序，保证分页稳定
        all.sort((a, b) -> {
            long ca = a.getCreateTime();
            long cb = b.getCreateTime();
            if (ca != cb) return Long.compare(cb, ca);
            return String.valueOf(a.getId()).compareTo(String.valueOf(b.getId()));
        });

        // 5. 切片（offset 越界时返回空 list 而不是抛异常）
        if (offset >= all.size()) {
            return new PageResult<>(Collections.emptyList(), total);
        }
        int fromIdx = (int) offset;
        int toIdx = (int) Math.min(fromIdx + pageSize, all.size());
        List<QueryResult> pageList = all.subList(fromIdx, toIdx);

        // 6. 转成 VectorImageRespVO
        List<VectorImageRespVO> list = new ArrayList<>(pageList.size());
        for (QueryResult r : pageList) {
            list.add(VectorImageRespVO.builder()
                    .id(r.getId())
                    .imagePath(r.getImagePath())
                    .fileId(r.getFileId())
                    .tenantId(r.getTenantId())
                    .createTime(r.getCreateTime())
                    .build());
        }
        return new PageResult<>(list, total);
    }

    @Override
    public List<VectorImageRespVO> getImageListByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        // 通过 image_index_service.queryByIds 拿到完整记录（带 vector），这里不需要 vector，
        // 因此复用 page 的逻辑：按 id in [...] 走条件查询（白名单支持）
        QueryCondition cond = QueryCondition.builder().in("id", ids).build();
        List<QueryResult> results = milvusService.queryByCondition(cond, false, ids.size());
        List<VectorImageRespVO> list = new ArrayList<>(results.size());
        for (QueryResult r : results) {
            list.add(VectorImageRespVO.builder()
                    .id(r.getId())
                    .imagePath(r.getImagePath())
                    .fileId(r.getFileId())
                    .tenantId(r.getTenantId())
                    .createTime(r.getCreateTime())
                    .build());
        }
        return list;
    }

    @Override
    public boolean deleteImage(String id) throws Exception {
        if (StrUtil.isEmpty(id)) {
            return false;
        }
        // 先拿到对应记录的 fileId，删除向量后再清理关联文件
        List<Long> fileIds = collectFileIdsByVectorIds(Collections.singletonList(id));
        int cnt = imageIndexService.deleteByIds(Collections.singletonList(id));
        if (cnt > 0) {
            deleteRelatedFiles(fileIds);
        }
        return cnt > 0;
    }

    @Override
    public int deleteImageList(List<String> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        // 先拿到对应记录的 fileId，删除向量后再清理关联文件
        List<Long> fileIds = collectFileIdsByVectorIds(ids);
        int cnt = imageIndexService.deleteByIds(ids);
        if (cnt > 0) {
            deleteRelatedFiles(fileIds);
        }
        return cnt;
    }

    /**
     * 通过向量 id 列表查询对应的 fileId，过滤掉无效的哨兵值并去重
     */
    private List<Long> collectFileIdsByVectorIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        QueryCondition cond = QueryCondition.builder().in("id", ids).build();
        List<QueryResult> records = milvusService.queryByCondition(cond, false, ids.size());
        return extractFileIds(records);
    }

    /**
     * 全量加载集合中所有向量的 fileId（用于 reset 时清理关联文件）。
     * drop 之前必须先做完，否则数据丢了就拿不到了。
     * 分批拉取，每次最多 1000 条，直到拉不到为止。
     */
    private List<Long> loadAllFileIds() {
        List<Long> all = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int batchSize = 1000;
        // 与 getImagePage 同源的"恒真表达式"，避免空条件校验
        QueryCondition cond = QueryCondition.builder().gt("create_time", 0L).build();
        while (true) {
            List<QueryResult> batch = milvusService.queryByCondition(cond, false, batchSize);
            if (CollectionUtils.isEmpty(batch)) {
                break;
            }
            for (QueryResult r : batch) {
                if (r.getId() != null) {
                    seenIds.add(r.getId());
                }
            }
            all.addAll(extractFileIds(batch));
            // 不足 batchSize 说明已经拉完
            if (batch.size() < batchSize) {
                break;
            }
        }
        log.info("[ImageSearchServiceImpl] loadAllFileIds 共扫描 {} 条向量，去重后得到 {} 个 fileId",
                seenIds.size(), all.size());
        return all;
    }

    /**
     * 从一批向量记录里挑出有效 fileId（>0）并去重
     */
    private List<Long> extractFileIds(List<QueryResult> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        Set<Long> fileIdSet = new LinkedHashSet<>();
        for (QueryResult r : records) {
            Long fid = r.getFileId();
            // 0L / null 视为未关联 infra_file，不需要清理
            if (fid != null && fid > 0L) {
                fileIdSet.add(fid);
            }
        }
        return new ArrayList<>(fileIdSet);
    }

    /**
     * 删除向量关联的文件，失败仅记录告警，不影响删除向量的主流程
     */
    private void deleteRelatedFiles(List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }
        try {
            fileService.deleteFileList(fileIds);
        } catch (Exception e) {
            log.warn("[ImageSearchServiceImpl] 删除向量关联文件失败，fileIds={}, err={}",
                    fileIds, e.getMessage());
        }
    }

    @Override
    public List<SearchResult> searchById(String id, int topK) throws Exception {
        if (StrUtil.isEmpty(id)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.VECTOR_IMAGE_NOT_EXISTS);
        }
        // 先拿完整记录（含 vector），再以图搜图
        List<com.lz.framework.vector.core.pojo.VectorRecord> records =
                milvusService.queryByIds(Collections.singletonList(id));
        if (records.isEmpty() || records.get(0) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.VECTOR_IMAGE_NOT_EXISTS, id);
        }
        float[] vec = records.get(0).getVector();
        if (vec == null || vec.length == 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.VECTOR_IMAGE_VECTOR_EMPTY, id);
        }
        return milvusService.searchByVector(vec, topK);
    }

    @Override
    public List<SearchResult> searchByStream(InputStream inputStream, int topK) throws Exception {
        return imageIndexService.searchByStream(inputStream, topK);
    }

    @Override
    public Map<String, Object> getCollectionInfo() {
        return milvusService.getCollectionInfo();
    }

    @Override
    public Map<String, Object> getCollectionStats(int sampleSize) {
        return milvusService.getCollectionStats(sampleSize);
    }

    @Override
    public void resetCollection() throws Exception {
        // drop 之前先把所有 fileId 拉出来，否则数据没了就没法清理关联文件
        List<Long> fileIds = loadAllFileIds();
        milvusService.dropCollection();
        // 这里采用与 demo 一致的做法：drop 后立刻 init，保证下一波写入可用
        try {
            milvusService.initCollection();
        } catch (Exception e) {
            log.warn("[resetCollection] drop 后重新初始化集合失败（可能 litchi.vector.enable=false）: {}",
                    e.getMessage());
        }
        // 集合重建后再删物理文件，避免删除过程中向"已 drop"的集合回写
        deleteRelatedFiles(fileIds);
    }
}
