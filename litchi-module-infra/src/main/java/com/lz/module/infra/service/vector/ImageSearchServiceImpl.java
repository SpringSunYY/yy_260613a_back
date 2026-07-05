package com.lz.module.infra.service.vector;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.vector.core.milvus.MilvusService;
import com.lz.framework.vector.core.pojo.QueryCondition;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.framework.vector.core.vector.ImageIndexService;
import com.lz.module.infra.controller.admin.vector.vo.UploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImagePageReqVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImageRespVO;
import com.lz.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // 3. Milvus 的 limit 由 pageSize 决定；分页 = 全量拉一批然后在内存里切片
        //    （Milvus 不支持 offset，详见 MilvusService.queryByCondition）
        int pageNo = Math.max(1, pageReqVO.getPageNo());
        int pageSize = pageReqVO.getPageSize() == null || pageReqVO.getPageSize() <= 0
                ? 10 : pageReqVO.getPageSize();
        long offset = (long) (pageNo - 1) * pageSize;
        long fetchLimit = offset + pageSize;

        List<QueryResult> all = milvusService.queryByCondition(cond, false, (int) Math.min(fetchLimit, 1000L));
        if (all.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 4. 按 create_time 倒序，再按 id 升序排序，保证分页稳定
        all.sort((a, b) -> {
            long ca = a.getCreateTime();
            long cb = b.getCreateTime();
            if (ca != cb) return Long.compare(cb, ca);
            return String.valueOf(a.getId()).compareTo(String.valueOf(b.getId()));
        });

        // 5. 切片
        long total = all.size();
        int fromIdx = (int) Math.min(offset, all.size());
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
            throw new IllegalArgumentException("图片 id 不能为空");
        }
        // 先拿完整记录（含 vector），再以图搜图
        List<com.lz.framework.vector.core.pojo.VectorRecord> records =
                milvusService.queryByIds(Collections.singletonList(id));
        if (records.isEmpty() || records.get(0) == null) {
            throw new IllegalArgumentException("图片 id 不存在: " + id);
        }
        float[] vec = records.get(0).getVector();
        if (vec == null || vec.length == 0) {
            throw new IllegalStateException("图片向量为空，id=" + id);
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