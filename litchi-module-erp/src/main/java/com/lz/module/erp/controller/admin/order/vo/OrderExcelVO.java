package com.lz.module.erp.controller.admin.order.vo;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息 Excel VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单信息Excel VO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class OrderExcelVO {

    /**
    * 订单名称
    */
    @ExcelProperty("订单名称")
    @ExcelI18n(i18nKey = "erp.order.field.name")
    @I18nNotEmpty(i18nKey = "erp.order.back.name.notEmpty", message = "订单名称不能为空")
    private String name;

    /**
    * 订单号
    */
    @ExcelProperty("订单号")
    @ExcelI18n(i18nKey = "erp.order.field.orderNo")
    @I18nNotEmpty(i18nKey = "erp.order.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
    * 下单日期
    */
    @ExcelProperty(value = "下单日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelI18n(i18nKey = "erp.order.field.orderTime")
    @I18nNotNull(i18nKey = "erp.order.back.orderTime.notNull", message = "下单日期不能为空")
    private LocalDateTime orderTime;

    /**
    * 订单来源
    */
    @ExcelProperty(value = "订单来源", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_resource", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.orderResource")
    @I18nNotEmpty(i18nKey = "erp.order.back.orderResource.notEmpty", message = "订单来源不能为空")
    private String orderResource;

    /**
    * 订单状态
    */
    @ExcelProperty(value = "订单状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.orderStatus")
    @I18nNotEmpty(i18nKey = "erp.order.back.orderStatus.notEmpty", message = "订单状态不能为空")
    private String orderStatus;

    /**
    * 审核状态
    */
    @ExcelProperty(value = "审核状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_audit_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.auditStatus")
    @I18nNotEmpty(i18nKey = "erp.order.back.auditStatus.notEmpty", message = "审核状态不能为空")
    private String auditStatus;

    /**
    * 当前工序
    */
    @ExcelProperty(value = "当前工序", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_current_process", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.currentProcess")
    @I18nNotEmpty(i18nKey = "erp.order.back.currentProcess.notEmpty", message = "当前工序不能为空")
    private String currentProcess;

    /**
     * 贷款
     */
    @ExcelProperty("贷款")
    @ExcelI18n(i18nKey = "erp.order.field.loan")
    @I18nNotNull(i18nKey = "erp.order.back.loan.notNull", message = "贷款不能为空")
    private BigDecimal loan;

    /**
     * 贷款状态
     */
    @ExcelProperty(value = "贷款状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_postage_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.loanStatus")
    @I18nNotEmpty(i18nKey = "erp.order.back.loanStatus.notEmpty", message = "贷款状态不能为空")
    private String loanStatus;

    /**
     * 邮费
     */
    @ExcelProperty("邮费")
    @ExcelI18n(i18nKey = "erp.order.field.postage")
    @I18nNotNull(i18nKey = "erp.order.back.postage.notNull", message = "邮费不能为空")
    private BigDecimal postage;

    /**
     * 邮费状态
     */
    @ExcelProperty(value = "邮费状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_loan_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.postageStatus")
    @I18nNotEmpty(i18nKey = "erp.order.back.postageStatus.notEmpty", message = "邮费状态不能为空")
    private String postageStatus;

    /**
     * 打印图片
     */
    @ExcelProperty("打印图片")
    @ExcelI18n(i18nKey = "erp.order.field.printImage")
    private String printImage;

    /**
    * 客户
    */
    @ExcelProperty("客户")
    @ExcelI18n(i18nKey = "erp.order.field.customer")
    private String customer;

    /**
    * 图片
    */
    @ExcelProperty("图片")
    @ExcelI18n(i18nKey = "erp.order.field.orderImage")
    private String orderImage;

    /**
    * 二维码
    */
    @ExcelProperty("二维码")
    @ExcelI18n(i18nKey = "erp.order.field.qrCode")
    private String qrCode;

    /**
    * 规格
    */
    @ExcelProperty(value = "规格", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_specification", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.specification")
    @I18nNotEmpty(i18nKey = "erp.order.back.specification.notEmpty", message = "规格不能为空")
    private String specification;

    /**
    * 版型
    */
    @ExcelProperty(value = "版型", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_pattern", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.pattern")
    private String pattern;

    /**
    * 布料
    */
    @ExcelProperty(value = "布料", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_fabric", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.fabric")
    @I18nNotEmpty(i18nKey = "erp.order.back.fabric.notEmpty", message = "布料不能为空")
    private String fabric;

    /**
    * 数量
    */
    @ExcelProperty("数量")
    @ExcelI18n(i18nKey = "erp.order.field.number")
    @I18nNotNull(i18nKey = "erp.order.back.number.notNull", message = "数量不能为空")
    private Integer number;

    /**
    * 提货方式
    */
    @ExcelProperty(value = "提货方式", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_pickup_method", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.pickupMethod")
    @I18nNotEmpty(i18nKey = "erp.order.back.pickupMethod.notEmpty", message = "提货方式不能为空")
    private String pickupMethod;

    /**
    * 发货地址
    */
    @ExcelProperty("发货地址")
    @ExcelI18n(i18nKey = "erp.order.field.shippingAddress")
    private String shippingAddress;

    /**
    * 预计发货时间
    */
    @ExcelProperty(value = "预计发货时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelI18n(i18nKey = "erp.order.field.exceptShippingTime")
    @I18nNotNull(i18nKey = "erp.order.back.exceptShippingTime.notNull", message = "预计发货时间不能为空")
    private LocalDateTime exceptShippingTime;

    /**
    * 发货订单
    */
    @ExcelProperty("发货订单")
    @ExcelI18n(i18nKey = "erp.order.field.shippingNo")
    private String shippingNo;

    /**
    * 发货时间
    */
    @ExcelProperty(value = "发货时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelI18n(i18nKey = "erp.order.field.shippingTime")
    private LocalDateTime shippingTime;

    /**
    * 打印状态
    */
    @ExcelProperty(value = "打印状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_print_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.order.field.printStatus")
    @I18nNotEmpty(i18nKey = "erp.order.back.printStatus.notEmpty", message = "打印状态不能为空")
    private String printStatus;

    /**
    * 补水
    */
    @ExcelProperty("补水")
    @ExcelI18n(i18nKey = "erp.order.field.hydration")
    private String hydration;

    /**
    * 备注
    */
    @ExcelProperty("备注")
    @ExcelI18n(i18nKey = "erp.order.field.remark")
    private String remark;

}
