package com.lz.module.erp.dal.mysql.order;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.lz.module.erp.controller.admin.order.vo.OrderPageReqVO;
import com.lz.module.erp.controller.admin.order.vo.OrderStatisticsRespVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.enums.ErpOrderAuditStatusEnum;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单信息 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderMapper extends BaseMapperX<OrderDO> {

    default PageResult<OrderDO> selectPage(OrderPageReqVO reqVO) {
        LambdaQueryWrapperX<OrderDO> queryWrapper = builderQueryWrapper(reqVO);
        return selectPage(reqVO, queryWrapper);
    }

    default LambdaQueryWrapperX<OrderDO> builderQueryWrapper(OrderPageReqVO reqVO) {
        return builderQueryConditions(reqVO)
                .applyOrderDesc(reqVO, OrderDO::getId);
    }

    default LambdaQueryWrapperX<OrderDO> builderQueryConditions(OrderPageReqVO reqVO) {
        return new LambdaQueryWrapperX<OrderDO>()
                .likeIfPresent(OrderDO::getName, reqVO.getName())
                .betweenIfPresent(OrderDO::getLoan, reqVO.getLoan())
                .eqIfPresent(OrderDO::getLoanStatus, reqVO.getLoanStatus())
                .betweenIfPresent(OrderDO::getPostage, reqVO.getPostage())
                .eqIfPresent(OrderDO::getPostageStatus, reqVO.getPostageStatus())
                .eqIfPresent(OrderDO::getOrderNo, reqVO.getOrderNo())
                .betweenIfPresent(OrderDO::getOrderTime, reqVO.getOrderTime())
                .eqIfPresent(OrderDO::getOrderResource, reqVO.getOrderResource())
                .eqIfPresent(OrderDO::getOrderStatus, reqVO.getOrderStatus())
                .eqIfPresent(OrderDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(OrderDO::getCurrentProcess, reqVO.getCurrentProcess())
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
                .betweenIfPresent(OrderDO::getCreateTime, reqVO.getCreateTime());
    }

    default PageResult<OrderDO> selectShipPage(OrderPageReqVO pageReqVO) {
        LambdaQueryWrapperX<OrderDO> queryWrapperX = builderQueryWrapper(pageReqVO);
        queryWrapperX.isNull(OrderDO::getShippingTime);
        queryWrapperX.eq(OrderDO::getAuditStatus, ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_3.getStatus());
        return selectPage(pageReqVO, queryWrapperX);
    }

    default List<OrderStatisticsRespVO> getOrderStatistics(OrderPageReqVO pageReqVO) {
        LambdaQueryWrapperX<OrderDO> queryWrapperX = builderQueryConditions(pageReqVO);
        return getOrderStatistics(queryWrapperX);
    }

    List<OrderStatisticsRespVO> getOrderStatistics(@Param("ew") Wrapper<OrderDO> wrapper);

    default List<OrderStatisticsRespVO> getOrderShipStatistics(OrderPageReqVO pageReqVO){
        LambdaQueryWrapperX<OrderDO> queryWrapperX = builderQueryConditions(pageReqVO);
        queryWrapperX.isNull(OrderDO::getShippingTime);
        queryWrapperX.eq(OrderDO::getAuditStatus, ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_3.getStatus());
        return getOrderStatistics(queryWrapperX);
    };

    default List<OrderStatisticsRespVO> getOrderLoanStatistics(OrderPageReqVO pageReqVO){
        LambdaQueryWrapperX<OrderDO> queryWrapperX = builderQueryConditions(pageReqVO);
        return getOrderLoanStatisticsByLoanStatus();
    }

    List<OrderStatisticsRespVO> getOrderLoanStatisticsByLoanStatus();

    default List<OrderStatisticsRespVO> getOrderPostageStatistics(OrderPageReqVO pageReqVO){
        LambdaQueryWrapperX<OrderDO> queryWrapperX = builderQueryConditions(pageReqVO);
        return getOrderPostageStatisticsByPostageStatus();
    }

    List<OrderStatisticsRespVO> getOrderPostageStatisticsByPostageStatus();

    ;
}
