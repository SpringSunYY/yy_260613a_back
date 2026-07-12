package com.lz.module.erp.controller.admin.orderVector.vo;

import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单向量重置 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单向量重置 Request VO")
@Data
public class OrderVectorRestReqVO {



    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

}
