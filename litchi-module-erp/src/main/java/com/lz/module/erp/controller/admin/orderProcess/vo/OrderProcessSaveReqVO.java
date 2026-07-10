package com.lz.module.erp.controller.admin.orderProcess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import com.mzt.logapi.starter.annotation.DiffLogField;
import jakarta.validation.constraints.*;

/**
 * 订单工序新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序新增/修改 Request VO")
@Data
public class OrderProcessSaveReqVO {

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
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.fabric.notEmpty", message = "布料不能为空")
    private String fabric;

    /**
     * 品类
     */
    @Schema(description = "品类", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.category.notEmpty", message = "品类不能为空")
    private String category;

    /**
     * 规格
     */
    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.specification.notEmpty", message = "规格不能为空")
    private String specification;

    /**
     * 开叉与否
     */
    @Schema(description = "开叉与否", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.hasForked.notEmpty", message = "开叉与否不能为空")
    private String hasForked;

    /**
     * 衫脚
     */
    @Schema(description = "衫脚", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.shirtHem.notEmpty", message = "衫脚不能为空")
    private String shirtHem;

    /**
     * 口袋
     */
    @Schema(description = "口袋", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.pocket.notEmpty", message = "口袋不能为空")
    private String pocket;

    /**
     * 领口
     */
    @Schema(description = "领口", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.neckline.notEmpty", message = "领口不能为空")
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

}
