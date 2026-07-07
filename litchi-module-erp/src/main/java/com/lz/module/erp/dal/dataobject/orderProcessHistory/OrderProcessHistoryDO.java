package com.lz.module.erp.dal.dataobject.orderProcessHistory;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.lz.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单工序记录 DO
 *
 * @author 荔枝软件
 */
@TableName("erp_order_process_history")
@KeySequence("erp_order_process_history_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderProcessHistoryDO extends BaseDO {

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
     * 原工序
     *
     * 枚举 {@link TODO erp_order_current_process 对应的类}
     */
    private String oldProcess;
    /**
     * 当前工序
     *
     * 枚举 {@link TODO erp_order_current_process 对应的类}
     */
    private String currentProcess;


}