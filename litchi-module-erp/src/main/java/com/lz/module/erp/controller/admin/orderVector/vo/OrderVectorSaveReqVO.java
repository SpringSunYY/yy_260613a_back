package com.lz.module.erp.controller.admin.orderVector.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import jakarta.validation.constraints.*;

/**
 * 订单向量新增/修改 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单向量新增/修改 Request VO")
@Data
public class OrderVectorSaveReqVO {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "23979")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
     * 向量编号
     */
    @Schema(description = "向量编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13840")
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.vectorId.notEmpty", message = "向量编号不能为空")
    private String vectorId;

    /**
     * 图片地址
     */
    @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.imageUrl.notEmpty", message = "图片地址不能为空")
    private String imageUrl;

}