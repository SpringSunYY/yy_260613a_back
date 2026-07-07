package com.lz.module.erp.dal.dataobject.orderVector;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.lz.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单向量 DO
 *
 * @author 荔枝软件
 */
@TableName("erp_order_vector")
@KeySequence("erp_order_vector_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVectorDO extends BaseDO {

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
     * 向量编号
     */
    private String vectorId;
    /**
     * 图片地址
     */
    private String imageUrl;


}