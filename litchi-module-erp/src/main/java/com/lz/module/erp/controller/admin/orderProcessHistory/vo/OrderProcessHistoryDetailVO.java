package com.lz.module.erp.controller.admin.orderProcessHistory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单工序记录 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序记录 订单详情 Response VO")
@Data
public class OrderProcessHistoryDetailVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10697")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 原工序
     */
    @Schema(description = "原工序")
    private String oldProcess;

    /**
     * 当前工序
     */
    @Schema(description = "当前工序", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentProcess;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String creator;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
