package com.lz.module.erp.dal.dataobject.order;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.lz.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单信息 DO
 *
 * @author 荔枝软件
 */
@TableName("erp_order")
@KeySequence("erp_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 订单名称
     */
    private String name;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 下单日期
     */
    private LocalDateTime orderTime;
    /**
     * 订单来源
     *
     * 枚举 {@link TODO erp_order_resource 对应的类}
     */
    private String orderResource;
    /**
     * 订单状态
     *
     * 枚举 {@link TODO erp_order_status 对应的类}
     */
    private String orderStatus;
    /**
     * 审核状态
     *
     * 枚举 {@link TODO erp_order_audit_status 对应的类}
     */
    private String auditStatus;
    /**
     * 当前工序
     *
     * 枚举 {@link TODO erp_order_current_process 对应的类}
     */
    private String currentProcess;
    /**
     * 客户
     */
    private String customer;
    /**
     * 图片
     */
    private String orderImage;
    /**
     * 二维码
     */
    private String qrCode;
    /**
     * 规格
     *
     * 枚举 {@link TODO erp_specification 对应的类}
     */
    private String specification;
    /**
     * 版型
     *
     * 枚举 {@link TODO erp_pattern 对应的类}
     */
    private String pattern;
    /**
     * 布料
     *
     * 枚举 {@link TODO erp_fabric 对应的类}
     */
    private String fabric;
    /**
     * 数量
     */
    private Integer number;
    /**
     * 提货方式
     *
     * 枚举 {@link TODO erp_order_pickup_method 对应的类}
     */
    private String pickupMethod;
    /**
     * 发货地址
     */
    private String shippingAddress;
    /**
     * 预计发货时间
     */
    private LocalDateTime exceptShippingTime;
    /**
     * 发货订单
     */
    private String shippingNo;
    /**
     * 发货时间
     */
    private LocalDateTime shippingTime;
    /**
     * 打印状态
     *
     * 枚举 {@link TODO erp_order_print_status 对应的类}
     */
    private String printStatus;
    /**
     * 补水
     */
    private String hydration;
    /**
     * 备注
     */
    private String remark;


}
