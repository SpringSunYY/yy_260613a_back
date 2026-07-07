package com.lz.module.erp.controller.admin.orderProcess.vo;

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
 * 订单工序分页 Request VO
 *
 * @author 荔枝软件
 */
@Schema(description = "管理后台 - 订单工序分页 Request VO")
@Data
public class OrderProcessPageReqVO extends PageParam {

    /**
    * 当前工序
    */
    @Schema(description = "当前工序")
    private String currentProcess;

    /**
    * 订单号
    */
    @Schema(description = "订单号")
    private String orderNo;

    /**
    * 排版人
    */
    @Schema(description = "排版人")
    private String layoutPerson;

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
    * 品类
    */
    @Schema(description = "品类")
    private String category;

    /**
    * 规格
    */
    @Schema(description = "规格")
    private String specification;

    /**
    * 开叉与否
    */
    @Schema(description = "开叉与否")
    private String hasForked;

    /**
    * 衫脚
    */
    @Schema(description = "衫脚")
    private String shirtHem;

    /**
    * 口袋
    */
    @Schema(description = "口袋")
    private String pocket;

    /**
    * 领口
    */
    @Schema(description = "领口")
    private String neckline;

    /**
    * 创建时间
    */
    @Sortable(value = "create_time")
    @I18nSize(i18nKey = "erp.orderProcess.back.createTime.size", min = 0, max = 2, message = "创建时间长度不能超过2")
    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}