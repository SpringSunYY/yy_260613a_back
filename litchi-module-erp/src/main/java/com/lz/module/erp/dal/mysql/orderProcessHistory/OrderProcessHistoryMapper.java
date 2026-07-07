package com.lz.module.erp.dal.mysql.orderProcessHistory;

import java.util.*;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.module.erp.dal.dataobject.orderProcessHistory.OrderProcessHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.*;

/**
 * 订单工序记录 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderProcessHistoryMapper extends BaseMapperX<OrderProcessHistoryDO> {

    default PageResult<OrderProcessHistoryDO> selectPage(OrderProcessHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderProcessHistoryDO>()
                .eqIfPresent(OrderProcessHistoryDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(OrderProcessHistoryDO::getOldProcess, reqVO.getOldProcess())
                .eqIfPresent(OrderProcessHistoryDO::getCurrentProcess, reqVO.getCurrentProcess())
                .betweenIfPresent(OrderProcessHistoryDO::getCreateTime, reqVO.getCreateTime())
                .applyOrderDesc(reqVO, OrderProcessHistoryDO::getId));
    }

}