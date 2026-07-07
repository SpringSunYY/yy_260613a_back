package com.lz.module.erp.controller.admin.orderAudit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单审核记录 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单审核记录 Response VO")
@Data
public class OrderAuditRespVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "831")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 原状态
     */
    @Schema(description = "原状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private String oldAuditStatus;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private String auditStatus;

    /**
     * 审核人
     */
    @Schema(description = "审核人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String auditPerson;

    /**
     * 审核时间
     */
    @Schema(description = "审核时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime auditTime;

    /**
     * 审核意见
     */
    @Schema(description = "审核意见", example = "你猜")
    private String auditRemark;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
