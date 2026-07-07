package com.lz.module.erp.controller.admin.orderAudit.vo;
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
import java.time.LocalDateTime;

/**
 * 订单审核记录 Excel VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单审核记录Excel VO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class OrderAuditExcelVO {

    /**
    * 订单号
    */
    @ExcelProperty("订单号")
    @ExcelI18n(i18nKey = "erp.orderAudit.field.orderNo")
    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.orderNo.notEmpty", message = "订单号不能为空")
    private String orderNo;

    /**
    * 原状态
    */
    @ExcelProperty(value = "原状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_audit_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderAudit.field.oldAuditStatus")
    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.oldAuditStatus.notEmpty", message = "原状态不能为空")
    private String oldAuditStatus;

    /**
    * 审核状态
    */
    @ExcelProperty(value = "审核状态", converter = DictConvert.class)
    @ExcelColumnSelect(dictType = "erp_order_audit_status", i18n = true)
    @ExcelI18n(i18nKey = "erp.orderAudit.field.auditStatus")
    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.auditStatus.notEmpty", message = "审核状态不能为空")
    private String auditStatus;

    /**
    * 审核人
    */
    @ExcelProperty("审核人")
    @ExcelI18n(i18nKey = "erp.orderAudit.field.auditPerson")
    @I18nNotEmpty(i18nKey = "erp.orderAudit.back.auditPerson.notEmpty", message = "审核人不能为空")
    private String auditPerson;

    /**
    * 审核时间
    */
    @ExcelProperty(value = "审核时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelI18n(i18nKey = "erp.orderAudit.field.auditTime")
    @I18nNotNull(i18nKey = "erp.orderAudit.back.auditTime.notNull", message = "审核时间不能为空")
    private LocalDateTime auditTime;

    /**
    * 审核意见
    */
    @ExcelProperty("审核意见")
    @ExcelI18n(i18nKey = "erp.orderAudit.field.auditRemark")
    private String auditRemark;

}