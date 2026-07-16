package com.lz.module.erp.controller.admin.orderProcess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单工序 Response VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序 Response VO")
@Data
public class OrderProcessRespVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32756")
    private Long id;

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
