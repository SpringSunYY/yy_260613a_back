package com.lz.module.erp.dal.dataobject.orderAudit;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.lz.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单审核记录 DO
 *
 * @author 荔枝软件
 */
@TableName("erp_order_audit")
@KeySequence("erp_order_audit_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAuditDO extends BaseDO {

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
     * 原状态
     *
     * 枚举 {@link TODO erp_order_audit_status 对应的类}
     */
    private String oldAuditStatus;
    /**
     * 审核状态
     *
     * 枚举 {@link TODO erp_order_audit_status 对应的类}
     */
    private String auditStatus;
    /**
     * 审核人
     */
    private String auditPerson;
    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
    /**
     * 审核意见
     */
    private String auditRemark;


}