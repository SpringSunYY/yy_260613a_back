package com.lz.module.erp.controller.admin.orderProcessHistory.vo;
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
 * 订单工序记录 Excel VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序记录Excel VO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class OrderProcessHistoryExcelVO {

    /**
    * 订单号
    */
    @ExcelProperty("订单号")
    @ExcelI18n(i18nKey = "erp.orderProcessHistory.field.orderNo")
    @I18nNotEmpty(i18nKey = "erp.orderProcessHistory.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
    * 原工序
    */
    @ExcelProperty(value = "原工序", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_current_process", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcessHistory.field.oldProcess")
    private String oldProcess;

    /**
    * 当前工序
    */
    @ExcelProperty(value = "当前工序", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_current_process", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcessHistory.field.currentProcess")
    @I18nNotEmpty(i18nKey = "erp.orderProcessHistory.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;

}