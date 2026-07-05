package com.lz.module.infra.controller.admin.vector;

import cn.hutool.core.io.IoUtil;
import com.lz.framework.common.pojo.CommonResult;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.vector.core.pojo.SearchResult;
import com.lz.module.infra.controller.admin.file.vo.file.FileUploadReqVO;
import com.lz.module.infra.controller.admin.file.vo.file.FileUploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.UploadRespVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImagePageReqVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImageRespVO;
import com.lz.module.infra.controller.admin.vector.vo.VectorImageSearchReqVO;
import com.lz.module.infra.dal.dataobject.file.FileDO;
import com.lz.module.infra.service.file.FileService;
import com.lz.module.infra.service.vector.ImageSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.lz.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 以图搜图")
@RestController
@RequestMapping("/infra/vector/image")
@Validated
@Slf4j
public class VectorImageController {

    @Resource
    private ImageSearchService imageSearchService;

    @Resource
    private FileService fileService;

    /**
     * 上传并索引单张图片
     * <p>走 fileService.createFile 入库到文件存储，写入文件日志，再调 imageSearchService 抽特征 + Milvus 索引。
     */
    @PostMapping("/upload")
    @Operation(summary = "上传并索引单张图片")
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:add')")
    public CommonResult<UploadRespVO> uploadImage(FileUploadReqVO uploadReqVO) throws Exception {
        MultipartFile file = uploadReqVO.getFile();
        byte[] content = IoUtil.readBytes(file.getInputStream());
        //首先根据文件名查询文件是否存在，如果存在就没必要构建了，因为这里只是测试
        FileDO existing = fileService.getFileByFileName(file.getOriginalFilename());
        if (existing != null) {
            return success(new UploadRespVO());
        }
        FileUploadRespVO fileUploadRespVO = fileService.createFile(content, file.getOriginalFilename(),
                uploadReqVO.getDirectory(), file.getContentType(), uploadReqVO.getModuleType());
        return success(imageSearchService.uploadImage(fileUploadRespVO.getUrl(), content, fileUploadRespVO.getId()));
    }

    /**
     * 获得图片分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得以图搜图图片分页")
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:query')")
    public CommonResult<PageResult<VectorImageRespVO>> getImagePage(@Valid VectorImagePageReqVO pageVO) {
        return success(imageSearchService.getImagePage(pageVO));
    }

    /**
     * 获得图片详情（按 id）
     */
    @GetMapping("/get")
    @Operation(summary = "获得以图搜图图片详情")
    @Parameter(name = "id", description = "图片主键", required = true)
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:query')")
    public CommonResult<VectorImageRespVO> getImage(@RequestParam("id") String id) {
        List<VectorImageRespVO> list = imageSearchService.getImageListByIds(List.of(id));
        return success(list.isEmpty() ? null : list.get(0));
    }

    /**
     * 删除图片（按 id）
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除以图搜图图片")
    @Parameter(name = "id", description = "图片主键", required = true)
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:delete')")
    public CommonResult<Boolean> deleteImage(@RequestParam("id") String id) throws Exception {
        return success(imageSearchService.deleteImage(id));
    }

    /**
     * 批量删除图片
     */
    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除以图搜图图片")
    @Parameter(name = "ids", description = "主键列表", required = true)
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:delete')")
    public CommonResult<Integer> deleteImageList(@RequestParam("ids") List<String> ids) throws Exception {
        return success(imageSearchService.deleteImageList(ids));
    }

    /**
     * 以图搜图（按库内 id）
     */
    @GetMapping("/search")
    @Operation(summary = "以图搜图（按库内图片 id）")
    @Parameter(name = "id", description = "库内图片主键", required = true)
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:search')")
    public CommonResult<List<SearchResult>> searchById(@RequestParam("id") String id,
                                                       @Valid VectorImageSearchReqVO reqVO) throws Exception {
        return success(imageSearchService.searchById(id, reqVO.getTopK()));
    }

    /**
     * 以图搜图（按上传的图片）
     */
    @PostMapping("/search/upload")
    @Operation(summary = "以图搜图（按上传图片）")
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:search')")
    public CommonResult<List<SearchResult>> searchByUpload(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "topK", defaultValue = "10") Integer topK)
            throws Exception {
        return success(imageSearchService.searchByStream(file.getInputStream(), topK == null ? 10 : topK));
    }

    /**
     * 获得 Milvus 集合信息
     */
    @GetMapping("/info")
    @Operation(summary = "获得 Milvus 集合信息")
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:query')")
    public CommonResult<Map<String, Object>> getCollectionInfo() {
        return success(imageSearchService.getCollectionInfo());
    }

    /**
     * 获得 Milvus 集合统计信息（记录数 + 样本）
     */
    @GetMapping("/stats")
    @Operation(summary = "获得 Milvus 集合统计信息")
    @Parameter(name = "sampleSize", description = "样本数量")
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:query')")
    public CommonResult<Map<String, Object>> getCollectionStats(
            @RequestParam(value = "sampleSize", defaultValue = "20") Integer sampleSize) {
        return success(imageSearchService.getCollectionStats(sampleSize == null ? 20 : sampleSize));
    }

    /**
     * 重置集合（清空数据并重建）
     */
    @DeleteMapping("/reset")
    @Operation(summary = "重置 Milvus 集合（清空数据并重建）")
    @PreAuthorize("@ss.hasPermission('infra:vectorImage:delete')")
    public CommonResult<Boolean> resetCollection() throws Exception {
        imageSearchService.resetCollection();
        return success(true);
    }

}
