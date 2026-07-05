package com.lz.module.infra.service.vector;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.vector.core.pojo.QueryResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.module.infra.controller.admin.vector.vo.BatchUploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.UploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImagePageReqVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImageRespVO;
import org.springframework.web.multipart.MultipartFile;

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
     * 批量上传图片。
     *
     * <p>逐个按文件名查 {@code infra_file}，已存在则跳过、不构建向量；不存在的走
     * {@link #uploadImage(String, byte[], Long)}。单个文件失败不影响其他文件。
     *
     * @param files   上传的文件列表
     * @param moduleType 模块类型（透传给 fileService.createFile）
     * @return 批量结果：成功 / 跳过 / 失败明细
     */
    BatchUploadRespVO batchUpload(List<MultipartFile> files, String moduleType) throws Exception;

    /**
     * 按 URL 列表构建向量（demo 的 /import 风格的轻量版本）。
     *
     * <p>流程：对每个 URL
     * <ol>
     *   <li>用 URL 末段当文件名查 {@code infra_file}，命中则跳过（同时把这条 URL 当作已存在）</li>
     *   <li>未命中则下载字节 → {@code fileService.createFile} → {@link #uploadImage(String, byte[], Long)}</li>
     * </ol>
     * 单条失败不影响其他 URL。
     *
     * @param urls 图片 URL 列表（http(s) 或可访问的相对路径）
     * @return 批量结果
     */
    BatchUploadRespVO uploadImagesByUrls(List<String> urls) throws Exception;

    /**
     * 从本地目录导入图片并构建向量（demo 的 /import 风格）。
     *
     * <p>递归（可选）扫描目录下的图片，对每个文件：
     * <ol>
     *   <li>用文件名查 {@code infra_file}，命中则跳过</li>
     *   <li>未命中则读字节 → {@code fileService.createFile} → {@link #uploadImage(String, byte[], Long)}</li>
     * </ol>
     * 单个文件失败不影响其他文件。
     *
     * @param dir       本地目录（绝对路径或相对路径）
     * @param recursive 是否递归子目录
     * @return 批量结果
     */
    BatchUploadRespVO importFromDirectory(String dir, boolean recursive) throws Exception;

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