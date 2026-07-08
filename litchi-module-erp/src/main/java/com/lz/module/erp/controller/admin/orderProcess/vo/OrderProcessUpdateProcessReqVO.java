package com.lz.module.erp.controller.admin.orderProcess.vo;

import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单工序新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 更新订单工序")
@Data
public class OrderProcessUpdateProcessReqVO {


    /**
     * 当前工序
     */
    @Schema(description = "当前工序", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

}
