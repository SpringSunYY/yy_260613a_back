package com.lz.module.erp.controller.admin.orderProcessHistory.vo;

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
 * 订单工序记录分页 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序记录分页 Request VO")
@Data
public class OrderProcessHistoryPageReqVO extends PageParam {

    /**
    * 订单号
    */
    @Schema(description = "订单号")
    private String orderNo;

    /**
    * 原工序
    */
    @Schema(description = "原工序")
    private String oldProcess;

    /**
    * 当前工序
    */
    @Schema(description = "当前工序")
    private String currentProcess;

    /**
    * 创建时间
    */
    @Sortable(value = "create_time")
    @I18nSize(i18nKey = "erp.orderProcessHistory.back.createTime.size", min = 0, max = 2, message = "创建时间长度不能超过2")
    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}