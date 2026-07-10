package com.lz.module.erp.controller.admin.order.vo;

import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

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
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32756")
    private Long id;

    /**
     * 当前工序
     */
    @Schema(description = "当前工序", requiredMode = Schema.RequiredMode.REQUIRED)
//    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
     * 排版人
     */
    @Schema(description = "排版人")
    private String layoutPerson;

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
     * 版型
     */
    @Schema(description = "版型")
    private String pattern;

    /**
     * 布料
     */
    @Schema(description = "布料", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fabric;

    /**
     * 品类
     */
    @Schema(description = "品类", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    /**
     * 规格
     */
    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED)
    private String specification;

    /**
     * 开叉与否
     */
    @Schema(description = "开叉与否", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hasForked;

    /**
     * 衫脚
     */
    @Schema(description = "衫脚", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shirtHem;

    /**
     * 口袋
     */
    @Schema(description = "口袋", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pocket;

    /**
     * 领口
     */
    @Schema(description = "领口", requiredMode = Schema.RequiredMode.REQUIRED)
    private String neckline;

    /**
     * 包装要求
     */
    @Schema(description = "包装要求")
    private String packagingRequirements;

    /**
     * 车间要求
     */
    @Schema(description = "车间要求")
    private String workshopRequirements;

    /**
     * 特别备注
     */
    @Schema(description = "特别备注", example = "你说的对")
    private String remark;


    /**
     * 订单名称
     */
    @Schema(description = "订单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String name;


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
