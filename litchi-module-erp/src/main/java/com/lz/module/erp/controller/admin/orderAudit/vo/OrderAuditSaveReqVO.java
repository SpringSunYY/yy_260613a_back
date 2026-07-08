package com.lz.module.erp.controller.admin.orderAudit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

/**
 * 订单审核记录新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单审核记录新增/修改 Request VO")
@Data
public class OrderAuditSaveReqVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "831")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
     * 原状态
     */
//    @Schema(description = "原状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
//    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.oldAuditStatus.notEmpty", message = "原状态不能为空")
//    private String oldAuditStatus;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.auditStatus.notEmpty", message = "审核状态不能为空")
    private String auditStatus;


    /**
     * 审核意见
     */
    @Schema(description = "审核意见", example = "你猜")
    private String auditRemark;

}
