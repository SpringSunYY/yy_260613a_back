package com.lz.module.erp.dal.mysql.orderProcess;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessPageReqVO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单工序 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderProcessMapper extends BaseMapperX<OrderProcessDO> {

    default PageResult<OrderProcessDO> selectPage(OrderProcessPageReqVO reqVO) {
        LambdaQueryWrapperX<OrderProcessDO> queryWrapper = builderQueryWrapper(reqVO);
        queryWrapper.applyOrderDesc(reqVO, OrderProcessDO::getUpdateTime);
        return selectPage(reqVO, queryWrapper);
    }

    default LambdaQueryWrapperX<OrderProcessDO> builderQueryWrapper(OrderProcessPageReqVO reqVO) {
        return new LambdaQueryWrapperX<OrderProcessDO>()
                .eqIfPresent(OrderProcessDO::getCurrentProcess, reqVO.getCurrentProcess())
                .notInIfPresent(OrderProcessDO::getCurrentProcess, reqVO.getNotInCurrentProcesses())
                .inIfPresent(OrderProcessDO::getCurrentProcess, reqVO.getInCurrentProcesses())
                .eqIfPresent(OrderProcessDO::getOrderStatus, reqVO.getOrderStatus())
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
                .betweenIfPresent(OrderProcessDO::getCreateTime, reqVO.getCreateTime());
    }

    default PageResult<OrderProcessDO> selectSortPage(OrderProcessPageReqVO pageReqVO) {
        LambdaQueryWrapperX<OrderProcessDO> orderProcessDOLambdaQueryWrapperX = builderQueryWrapper(pageReqVO);
        orderProcessDOLambdaQueryWrapperX.orderByAsc(OrderProcessDO::getOrderStatus);
        orderProcessDOLambdaQueryWrapperX.orderByDesc(OrderProcessDO::getCreateTime);
        return selectPage(pageReqVO, orderProcessDOLambdaQueryWrapperX);
    }
}
