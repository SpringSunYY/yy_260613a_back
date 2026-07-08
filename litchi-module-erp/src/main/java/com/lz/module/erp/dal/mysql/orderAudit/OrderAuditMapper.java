package com.lz.module.erp.dal.mysql.orderAudit;

import java.util.*;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.module.erp.dal.dataobject.orderAudit.OrderAuditDO;
import org.apache.ibatis.annotations.Mapper;
import com.lz.module.erp.controller.admin.orderAudit.vo.*;

/**
 * 订单审核记录 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderAuditMapper extends BaseMapperX<OrderAuditDO> {

    default PageResult<OrderAuditDO> selectPage(OrderAuditPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderAuditDO>()
                .eqIfPresent(OrderAuditDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(OrderAuditDO::getOldAuditStatus, reqVO.getOldAuditStatus())
                .eqIfPresent(OrderAuditDO::getAuditStatus, reqVO.getAuditStatus())
                .betweenIfPresent(OrderAuditDO::getCreateTime, reqVO.getCreateTime())
                .applyOrderDesc(reqVO, OrderAuditDO::getId));
    }

}
