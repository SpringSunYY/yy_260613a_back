package com.lz.module.erp.controller.admin.orderProcess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单工序 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序 Page Response VO")
@Data
public class OrderProcessPageRespVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32756")
    private Long id;

    /**
     * 打印图片
     */
    @Schema(description = "打印图片")
    private String printImage;

    /**
     * 当前工序
     */
    @Schema(description = "当前工序", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentProcess;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态", example = "1")
    private String orderStatus;


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
     * 规格
     */
    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED)
    private String specification;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    /**
     * 客户
     */
    @Schema(description = "客户")
    private String customer;

    /**
     * 数量
     */
    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer number;
    /**
     * 发货地址
     */
    @Schema(description = "发货地址")
    private String shippingAddress;

    /**
     * 提货方式
     */
    @Schema(description = "提货方式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pickupMethod;

    /**
     * 预计发货时间
     */
    @Schema(description = "预计发货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime exceptShippingTime;

}
