package com.lz.module.erp.dal.mysql.orderProcess;

import java.util.*;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import org.apache.ibatis.annotations.Mapper;
import com.lz.module.erp.controller.admin.orderProcess.vo.*;

/**
 * 订单工序 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderProcessMapper extends BaseMapperX<OrderProcessDO> {

    default PageResult<OrderProcessDO> selectPage(OrderProcessPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderProcessDO>()
                .eqIfPresent(OrderProcessDO::getCurrentProcess, reqVO.getCurrentProcess())
                .eqIfPresent(OrderProcessDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(OrderProcessDO::getLayoutPerson, reqVO.getLayoutPerson())
                .eqIfPresent(OrderProcessDO::getPattern, reqVO.getPattern())
                .eqIfPresent(OrderProcessDO::getFabric, reqVO.getFabric())
                .eqIfPresent(OrderProcessDO::getCategory, reqVO.getCategory())
                .eqIfPresent(OrderProcessDO::getSpecification, reqVO.getSpecification())
                .eqIfPresent(OrderProcessDO::getHasForked, reqVO.getHasForked())
                .eqIfPresent(OrderProcessDO::getShirtHem, reqVO.getShirtHem())
                .eqIfPresent(OrderProcessDO::getPocket, reqVO.getPocket())
                .eqIfPresent(OrderProcessDO::getNeckline, reqVO.getNeckline())
                .betweenIfPresent(OrderProcessDO::getCreateTime, reqVO.getCreateTime())
                .applyOrderDesc(reqVO, OrderProcessDO::getId));
    }

}