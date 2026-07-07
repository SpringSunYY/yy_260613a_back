package com.lz.module.erp.dal.mysql.order;

import java.util.*;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import com.lz.module.erp.controller.admin.order.vo.*;

/**
 * 订单信息 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderMapper extends BaseMapperX<OrderDO> {

    default PageResult<OrderDO> selectPage(OrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderDO>()
                .likeIfPresent(OrderDO::getName, reqVO.getName())
                .eqIfPresent(OrderDO::getOrderNo, reqVO.getOrderNo())
                .betweenIfPresent(OrderDO::getOrderTime, reqVO.getOrderTime())
                .eqIfPresent(OrderDO::getOrderResource, reqVO.getOrderResource())
                .eqIfPresent(OrderDO::getOrderStatus, reqVO.getOrderStatus())
                .eqIfPresent(OrderDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(OrderDO::getCurrentProcess, reqVO.getCurrentProcess())
                .betweenIfPresent(OrderDO::getShipmentTime, reqVO.getShipmentTime())
                .likeIfPresent(OrderDO::getCustomer, reqVO.getCustomer())
                .eqIfPresent(OrderDO::getSpecification, reqVO.getSpecification())
                .eqIfPresent(OrderDO::getPattern, reqVO.getPattern())
                .eqIfPresent(OrderDO::getFabric, reqVO.getFabric())
                .betweenIfPresent(OrderDO::getNumber, reqVO.getNumber())
                .eqIfPresent(OrderDO::getPickupMethod, reqVO.getPickupMethod())
                .betweenIfPresent(OrderDO::getExceptShippingTime, reqVO.getExceptShippingTime())
                .eqIfPresent(OrderDO::getShippingNo, reqVO.getShippingNo())
                .betweenIfPresent(OrderDO::getShippingTime, reqVO.getShippingTime())
                .eqIfPresent(OrderDO::getPrintStatus, reqVO.getPrintStatus())
                .betweenIfPresent(OrderDO::getCreateTime, reqVO.getCreateTime())
                .applyOrderDesc(reqVO, OrderDO::getId));
    }

}