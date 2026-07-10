package com.lz.module.erp.controller.admin.order.vo;

import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import com.mzt.logapi.starter.annotation.DiffLogField;
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
public class OrderSaveReqVO {

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
     * 下单日期
     */
    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotNull(i18nKey = "erp.order.back.orderTime.notNull", message = "下单日期不能为空")
    private LocalDateTime orderTime;

    /**
     * 订单来源
     */
    @Schema(description = "订单来源", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.order.back.orderResource.notEmpty", message = "订单来源不能为空")
    private String orderResource;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @I18nNotEmpty(i18nKey = "erp.order.back.orderStatus.notEmpty", message = "订单状态不能为空")
    private String orderStatus;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
//    @I18nNotEmpty(i18nKey = "erp.order.back.auditStatus.notEmpty", message = "审核状态不能为空")
    private String auditStatus;

    /**
     * 当前工序
     */
    @Schema(description = "当前工序", requiredMode = Schema.RequiredMode.REQUIRED)
//    @I18nNotEmpty(i18nKey = "erp.order.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;


    /**
     * 客户
     */
    @Schema(description = "客户")
    private String customer;

    /**
     * 图片
     */
    @Schema(description = "图片")
    private String orderImage;

    /**
     * 二维码
     */
    @Schema(description = "二维码")
    private String qrCode;

    /**
     * 规格
     */
    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED)
//    @I18nNotEmpty(i18nKey = "erp.order.back.specification.notEmpty", message = "规格不能为空")
    private String specification;

    /**
     * 版型
     */
    @Schema(description = "版型")
    private String pattern;

    /**
     * 布料
     */
    @Schema(description = "布料", requiredMode = Schema.RequiredMode.REQUIRED)
//    @I18nNotEmpty(i18nKey = "erp.order.back.fabric.notEmpty", message = "布料不能为空")
    private String fabric;

    /**
     * 数量
     */
    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotNull(i18nKey = "erp.order.back.number.notNull", message = "数量不能为空")
    private Integer number;

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
    private LocalDateTime shippingTime;

    /**
     * 打印状态
     */
    @Schema(description = "打印状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
//    @I18nNotEmpty(i18nKey = "erp.order.back.printStatus.notEmpty", message = "打印状态不能为空")
    private String printStatus;

    /**
     * 补水
     */
    @Schema(description = "补水")
    private String hydration;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "随便")
    private String remark;

    /**
    * 订单明细列表
    */
    @Schema(description = "订单明细列表")
    @Valid
    private List<OrderDetailSaveReqVO> orderDetails;

    /**
    * 订单工序
    */
    @Schema(description = "订单工序")
    @Valid
    private OrderProcessSaveReqVO orderProcess;

}
