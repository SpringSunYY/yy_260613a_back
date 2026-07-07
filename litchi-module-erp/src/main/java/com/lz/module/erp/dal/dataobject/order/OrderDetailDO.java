package com.lz.module.erp.dal.dataobject.order;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.lz.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单明细 DO
 *
 * @author 荔枝软件
 */
@TableName("erp_order_detail")
@KeySequence("erp_order_detail_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 名字
     */
    private String setName;
    /**
     * 号码
     */
    private String setNumber;
    /**
     * 尺码
     *
     * 枚举 {@link TODO erp_set_size 对应的类}
     */
    private String setSize;
    /**
     * 数量
     */
    private Integer setQuantity;
    /**
     * 备注
     */
    private String remark;

}
