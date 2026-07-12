package com.lz.module.erp.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单统计 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单统计 Response VO")
@Data
public class OrderStatisticsRespVO {

    /**
     * 统计名称
     */
    @Schema(description = "统计名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String name;

    /**
     * 数量
     */
    @Schema(description = "总数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer total;


}
