package com.lz.module.erp.controller.admin.orderVector.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 订单向量以图搜图请求 VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单向量以图搜图 Request VO")
@Data
public class OrderVectorSearchReqVO {

    /**
     * Top K 返回条数
     */
    @Schema(description = "Top K", example = "100")
    @NotNull(message = "Top K 不能为空")
    @Min(value = 1, message = "Top K 最小为 1")
    @Max(value = 1000, message = "Top K 最大为 1000")
    private Integer topK = 100;

    /**
     * 查询图（按上传图片搜索时使用）
     */
    @Schema(description = "查询图片文件")
    private MultipartFile file;
}