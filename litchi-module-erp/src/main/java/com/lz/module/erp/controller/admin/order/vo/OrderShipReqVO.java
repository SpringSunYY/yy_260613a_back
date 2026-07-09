package com.lz.module.erp.controller.admin.order.vo;

import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单信息新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单信息新增/修改 Request VO")
@Data
public class OrderShipReqVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "25663")
    private Long id;

    /**
     * 订单名称
     */
    @Schema(description = "订单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @I18nNotEmpty(i18nKey = "erp.order.back.name.notEmpty", message = "订单名称不能为空")
    private String name;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.order.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;


    /**
     * 提货方式
     */
    @Schema(description = "提货方式", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.order.back.pickupMethod.notEmpty", message = "提货方式不能为空")
    private String pickupMethod;

    /**
     * 发货地址
     */
    @Schema(description = "发货地址")
    private String shippingAddress;

    /**
     * 预计发货时间
     */
    @Schema(description = "预计发货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotNull(i18nKey = "erp.order.back.exceptShippingTime.notNull", message = "预计发货时间不能为空")
    private LocalDateTime exceptShippingTime;

    /**
     * 发货订单
     */
    @Schema(description = "发货订单")
    private String shippingNo;

    /**
     * 发货时间
     */
    @Schema(description = "发货时间")
    @I18nNotNull(i18nKey = "erp.order.back.shippingTime.notNull", message = "发货时间不能为空")
    private LocalDateTime shippingTime;

}
