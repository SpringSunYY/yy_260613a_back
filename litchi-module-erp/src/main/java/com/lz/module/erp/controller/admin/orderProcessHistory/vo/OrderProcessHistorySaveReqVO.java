package com.lz.module.erp.controller.admin.orderProcessHistory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import jakarta.validation.constraints.*;

/**
 * 订单工序记录新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序记录新增/修改 Request VO")
@Data
public class OrderProcessHistorySaveReqVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10697")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcessHistory.back.orderNo.notEmpty", message = "订单号不能为空")
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
    @I18nNotEmpty(i18nKey = "erp.orderProcessHistory.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;

}