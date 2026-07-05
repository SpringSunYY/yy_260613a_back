package com.lz.module.infra.controller.admin.vector.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理后台 - 以图搜图 Request VO
 *
 * @Project: LZ-Order
 * @Author: YY
 * @CreateTime: 2026-07-05
 * @Version: 1.0
 */
@Schema(description = "管理后台 - 以图搜图 Request VO")
@Data
public class VectorImageSearchReqVO {

    @Schema(description = "Top K 返回条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @Min(value = 1, message = "topK 必须大于 0")
    @Max(value = 1000, message = "topK 不能超过 1000")
    private Integer topK = 10;

}