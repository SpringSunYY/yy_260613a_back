package com.lz.module.erp.controller.admin.order.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.annotation.Sortable;
import com.lz.framework.common.validation.i18n.I18nSize;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.lz.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 订单信息分页 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单信息分页 Request VO")
@Data
public class OrderPageReqVO extends PageParam {

    /**
    * 订单名称
    */
    @Schema(description = "订单名称", example = "李四")
    private String name;

    /**
    * 订单号
    */
    @Schema(description = "订单号")
    private String orderNo;

    /**
    * 下单日期
    */
    @Sortable(value = "order_time")
    @I18nSize(i18nKey = "erp.order.back.orderTime.size", min = 0, max = 2, message = "下单日期长度不能超过2")
    @Schema(description = "下单日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] orderTime;

    /**
    * 订单来源
    */
    @Schema(description = "订单来源")
    private String orderResource;

    /**
    * 订单状态
    */
    @Schema(description = "订单状态", example = "1")
    private String orderStatus;

    /**
    * 审核状态
    */
    @Schema(description = "审核状态", example = "2")
    private String auditStatus;

    /**
    * 当前工序
    */
    @Schema(description = "当前工序")
    private String currentProcess;

    /**
    * 出货日期
    */

    /**
    * 客户
    */
    @Schema(description = "客户")
    private String customer;

    /**
    * 规格
    */
    @Schema(description = "规格")
    private String specification;

    /**
    * 版型
    */
    @Schema(description = "版型")
    private String pattern;

    /**
    * 布料
    */
    @Schema(description = "布料")
    private String fabric;

    /**
    * 数量
    */
    @Sortable(value = "number")
    @I18nSize(i18nKey = "erp.order.back.number.size", min = 0, max = 2, message = "数量长度不能超过2")
    @Schema(description = "数量")
    private Integer[] number;

    /**
    * 提货方式
    */
    @Schema(description = "提货方式")
    private String pickupMethod;

    /**
    * 预计发货时间
    */
    @Sortable(value = "except_shipping_time")
    @I18nSize(i18nKey = "erp.order.back.exceptShippingTime.size", min = 0, max = 2, message = "预计发货时间长度不能超过2")
    @Schema(description = "预计发货时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] exceptShippingTime;

    /**
    * 发货订单
    */
    @Schema(description = "发货订单")
    private String shippingNo;

    /**
    * 发货时间
    */
    @Sortable(value = "shipping_time")
    @I18nSize(i18nKey = "erp.order.back.shippingTime.size", min = 0, max = 2, message = "发货时间长度不能超过2")
    @Schema(description = "发货时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] shippingTime;

    /**
    * 打印状态
    */
    @Schema(description = "打印状态", example = "1")
    private String printStatus;

    /**
    * 创建时间
    */
    @Sortable(value = "create_time")
    @I18nSize(i18nKey = "erp.order.back.createTime.size", min = 0, max = 2, message = "创建时间长度不能超过2")
    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
