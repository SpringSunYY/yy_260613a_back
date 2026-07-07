package com.lz.module.erp.controller.admin.orderVector.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单向量 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单向量 Response VO")
@Data
public class OrderVectorRespVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "23979")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 向量编号
     */
    @Schema(description = "向量编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13840")
    private String vectorId;

    /**
     * 图片地址
     */
    @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    private String imageUrl;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
