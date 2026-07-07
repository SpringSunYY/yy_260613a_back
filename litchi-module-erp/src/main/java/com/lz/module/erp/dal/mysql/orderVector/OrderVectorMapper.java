package com.lz.module.erp.dal.mysql.orderVector;

import java.util.*;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.module.erp.dal.dataobject.orderVector.OrderVectorDO;
import org.apache.ibatis.annotations.Mapper;
import com.lz.module.erp.controller.admin.orderVector.vo.*;

/**
 * 订单向量 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderVectorMapper extends BaseMapperX<OrderVectorDO> {

    default PageResult<OrderVectorDO> selectPage(OrderVectorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderVectorDO>()
                .eqIfPresent(OrderVectorDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(OrderVectorDO::getVectorId, reqVO.getVectorId())
                .betweenIfPresent(OrderVectorDO::getCreateTime, reqVO.getCreateTime())
                .applyOrderDesc(reqVO, OrderVectorDO::getId));
    }

}