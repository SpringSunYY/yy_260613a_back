package com.lz.module.erp.controller.admin.orderProcess.vo;
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
 * 订单工序 Excel VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序Excel VO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class OrderProcessExcelVO {

    /**
    * 当前工序
    */
    @ExcelProperty(value = "当前工序", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_current_process", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.currentProcess")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;

    /**
    * 订单号
    */
    @ExcelProperty("订单号")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.orderNo")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
     * 订单状态
     */
    @ExcelProperty(value = "订单状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.orderStatus")
    private String orderStatus;


    /**
    * 排版人
    */
    @ExcelProperty("排版人")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.layoutPerson")
    private String layoutPerson;

    /**
    * 图片
    */
    @ExcelProperty("图片")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.orderImage")
    private String orderImage;

    /**
    * 二维码
    */
    @ExcelProperty("二维码")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.qrCode")
    private String qrCode;

    /**
    * 版型
    */
    @ExcelProperty(value = "版型", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_pattern", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.pattern")
    private String pattern;

    /**
    * 布料
    */
    @ExcelProperty(value = "布料", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_fabric", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.fabric")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.fabric.notEmpty", message = "布料不能为空")
    private String fabric;

    /**
    * 品类
    */
    @ExcelProperty(value = "品类", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_category", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.category")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.category.notEmpty", message = "品类不能为空")
    private String category;

    /**
    * 规格
    */
    @ExcelProperty(value = "规格", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_specification", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.specification")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.specification.notEmpty", message = "规格不能为空")
    private String specification;

    /**
    * 开叉与否
    */
    @ExcelProperty(value = "开叉与否", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_has_forked", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.hasForked")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.hasForked.notEmpty", message = "开叉与否不能为空")
    private String hasForked;

    /**
    * 衫脚
    */
    @ExcelProperty(value = "衫脚", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_shirt_hem", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.shirtHem")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.shirtHem.notEmpty", message = "衫脚不能为空")
    private String shirtHem;

    /**
    * 口袋
    */
    @ExcelProperty(value = "口袋", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_pocket", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.pocket")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.pocket.notEmpty", message = "口袋不能为空")
    private String pocket;

    /**
    * 领口
    */
    @ExcelProperty(value = "领口", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_neckline", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderProcess.field.neckline")
    @I18nNotEmpty(i18nKey = "erp.orderProcess.back.neckline.notEmpty", message = "领口不能为空")
    private String neckline;

    /**
    * 包装要求
    */
    @ExcelProperty("包装要求")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.packagingRequirements")
    private String packagingRequirements;

    /**
    * 车间要求
    */
    @ExcelProperty("车间要求")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.workshopRequirements")
    private String workshopRequirements;

    /**
    * 特别备注
    */
    @ExcelProperty("特别备注")
    @ExcelI18n(i18nKey = "erp.orderProcess.field.remark")
    private String remark;

}
