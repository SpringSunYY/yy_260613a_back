package com.lz.module.erp.dal.dataobject.orderProcess;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.lz.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单工序 DO
 *
 * @author 荔枝软件
 */
@TableName("erp_order_process")
@KeySequence("erp_order_process_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderProcessDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 当前工序
     *
     * 枚举 {@link TODO erp_order_current_process 对应的类}
     */
    private String currentProcess;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 订单状态
     *
     * 枚举 {@link TODO erp_order_status 对应的类}
     */
    private String orderStatus;
    /**
     * 排版人
     */
    private String layoutPerson;
    /**
     * 图片
     */
    private String orderImage;
    /**
     * 二维码
     */
    private String qrCode;
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
     * 品类
     *
     * 枚举 {@link TODO erp_category 对应的类}
     */
    private String category;
    /**
     * 规格
     *
     * 枚举 {@link TODO erp_specification 对应的类}
     */
    private String specification;
    /**
     * 开叉与否
     *
     * 枚举 {@link TODO erp_has_forked 对应的类}
     */
    private String hasForked;
    /**
     * 衫脚
     *
     * 枚举 {@link TODO erp_shirt_hem 对应的类}
     */
    private String shirtHem;
    /**
     * 口袋
     *
     * 枚举 {@link TODO erp_pocket 对应的类}
     */
    private String pocket;
    /**
     * 领口
     *
     * 枚举 {@link TODO erp_neckline 对应的类}
     */
    private String neckline;
    /**
     * 包装要求
     */
    private String packagingRequirements;
    /**
     * 车间要求
     */
    private String workshopRequirements;
    /**
     * 特别备注
     */
    private String remark;


}
