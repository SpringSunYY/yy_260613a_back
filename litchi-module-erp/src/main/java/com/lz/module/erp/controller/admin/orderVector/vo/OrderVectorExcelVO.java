package com.lz.module.erp.controller.admin.orderVector.vo;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lz.framework.common.validation.i18n.I18nNotEmpty;
import com.lz.framework.common.validation.i18n.I18nNotNull;
import com.lz.framework.excel.core.annotations.ExcelColumnSelect;
import com.lz.framework.excel.core.annotations.ExcelI18n;
import com.lz.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 订单向量 Excel VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单向量Excel VO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class OrderVectorExcelVO {

    /**
    * 订单号
    */
    @ExcelProperty("订单号")
    @ExcelI18n(i18nKey = "erp.orderVector.field.orderNo")
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
    * 向量编号
    */
    @ExcelProperty("向量编号")
    @ExcelI18n(i18nKey = "erp.orderVector.field.vectorId")
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.vectorId.notEmpty", message = "向量编号不能为空")
    private String vectorId;

    /**
    * 图片地址
    */
    @ExcelProperty("图片地址")
    @ExcelI18n(i18nKey = "erp.orderVector.field.imageUrl")
    @I18nNotEmpty(i18nKey = "erp.orderVector.back.imageUrl.notEmpty", message = "图片地址不能为空")
    private String imageUrl;

}