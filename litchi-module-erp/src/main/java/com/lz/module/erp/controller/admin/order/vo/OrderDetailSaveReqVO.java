package com.lz.module.erp.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import jakarta.validation.constraints.*;

/**
 * 订单明细新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单明细新增/修改 Request VO")
@Data
public class OrderDetailSaveReqVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6502")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
//    @I18nNotEmpty(i18nKey = "erp.orderDetail.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
     * 名字
     */
    @Schema(description = "名字", example = "赵六")
    private String setName;

    /**
     * 号码
     */
    @Schema(description = "号码")
    private String setNumber;

    /**
     * 尺码
     */
    @Schema(description = "尺码", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderDetail.back.setSize.notEmpty", message = "尺码不能为空")
    private String setSize;

    /**
     * 数量
     */
    @Schema(description = "数量")
    private Integer setQuantity;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "随便")
    private String remark;
}
