package com.lz.module.erp.controller.admin.order.vo;

import com.lz.framework.common.annotation.Sortable;
import com.lz.framework.common.validation.i18n.I18nSize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单信息 Response VO")
@Data
public class OrderRespVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "25663")
    private Long id;

    /**
     * 订单名称
     */
    @Schema(description = "订单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String name;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 下单日期
     */
    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime orderTime;

    /**
     * 订单来源
     */
    @Schema(description = "订单来源", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderResource;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String orderStatus;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private String auditStatus;

    /**
     * 当前工序
     */
    @Schema(description = "当前工序", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentProcess;

    /**
     * 贷款
     */
    @Schema(description = "贷款")
    private BigDecimal loan;

    /**
     * 贷款状态
     */
    @Schema(description = "贷款状态", example = "2")
    private String loanStatus;

    /**
     * 邮费
     */
    @Schema(description = "邮费")
    private BigDecimal postage;

    /**
     * 邮费状态
     */
    @Schema(description = "邮费状态", example = "1")
    private String postageStatus;

    /**
     * 打印图片
     */
    @Schema(description = "打印图片")
    private String printImage;

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
    private String fabric;

    /**
     * 数量
     */
    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer number;

    /**
     * 提货方式
     */
    @Schema(description = "提货方式", requiredMode = Schema.RequiredMode.REQUIRED)
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
    private LocalDateTime shippingTime;

    /**
     * 打印状态
     */
    @Schema(description = "打印状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
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
     * 创建人
     */
    @Schema(description = "创建人")
    private String creator;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
