package com.lz.module.infra.service.vector;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.module.infra.controller.admin.vector.vo.UploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImagePageReqVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImageRespVO;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ImageSearchService {

    /**
     * 上传图片并写入 Milvus 向量库
     *
     * @param fileUrl 文件访问 URL（通过 {@code fileService.createFile(...)} 得到）
     * @param content 文件原始字节（用于特征提取）
     * @return 上传结果（id + url）
     */
    UploadRespVO uploadImage(String fileUrl, byte[] content) throws Exception;

    /**
     * 上传图片并写入 Milvus 向量库，并把这次上传关联到 {@code infra_file.id}。
     *
     * <p>与 2 参重载的区别：传 {@code fileId} 时，写入 Milvus 的 {@code file_id} 列就是真实 ID，
     * 后续可按它反查"这张图是从哪个文件日志上传的"。不传 / 传 {@code null} 表示"目录导入"或"无文件关联"，
     * 由 MilvusService 翻译成哨兵值。
     *
     * @param fileUrl 文件访问 URL
     * @param content 文件原始字节
     * @param fileId  关联的 {@code infra_file.id}；可为 {@code null}
     */
    UploadRespVO uploadImage(String fileUrl, byte[] content, Long fileId) throws Exception;

    /**
     * 获得图片分页（基于 Milvus 条件查询，offset/limit 实现分页）
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<VectorImageRespVO> getImagePage(VectorImagePageReqVO pageReqVO);

    /**
     * 按主键集合查询图片
     *
     * @param ids 主键列表
     * @return 图片列表
     */
    List<VectorImageRespVO> getImageListByIds(List<String> ids);

    /**
     * 删除图片（按 id）
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteImage(String id) throws Exception;

    /**
     * 批量删除图片（按 id 列表）
     *
     * @param ids 主键列表
     * @return 删除成功的数量
     */
    int deleteImageList(List<String> ids) throws Exception;

    /**
     * 以图搜图（按 id）
     *
     * @param id   库内已索引的图片 id
     * @param topK 返回 Top K
     * @return 相似结果（不含自身）
     */
    List<SearchResult> searchById(String id, int topK) throws Exception;

    /**
     * 以图搜图（按上传的输入流，如 multipart file）
     *
     * @param inputStream 查询图输入流（方法内部会关闭）
     * @param topK        返回 Top K
     * @return 相似结果
     */
    List<SearchResult> searchByStream(InputStream inputStream, int topK) throws Exception;

    /**
     * 获得 Milvus 集合信息（集合名 / 维度 / 是否存在）
     */
    Map<String, Object> getCollectionInfo();

    /**
     * 获得 Milvus 集合统计（记录数 + 样本）
     *
     * @param sampleSize 样本数量
     */
    Map<String, Object> getCollectionStats(int sampleSize);

    /**
     * 重置集合（清空数据并重建）
     */
    void resetCollection() throws Exception;
}