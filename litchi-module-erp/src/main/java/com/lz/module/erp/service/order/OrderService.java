package com.lz.module.erp.service.order;

import java.util.*;

import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import jakarta.validation.*;
import com.lz.module.erp.controller.admin.order.vo.*;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;

/**
 * 订单信息 Service 接口
 *
 * @author 荔枝软件
 */
public interface OrderService {

    /**
     * 创建订单信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrder(@Valid OrderSaveReqVO createReqVO);

    /**
     * 初始化订单信息 根据工序
     *
     * @param order          订单信息
     * @param orderProcess 工序
     */
    void initOrderByProcess(OrderDO order, OrderProcessSaveReqVO orderProcess);

    /**
     * 更新订单信息
     *
     * @param updateReqVO 更新信息
     */
    void updateOrder(@Valid OrderSaveReqVO updateReqVO);

    /**
     * 更新订单信息
     *
     * @param orderDO 订单信息
     */
    void updateOrder(OrderDO orderDO);

    /**
     * 删除订单信息
     *
     * @param id 编号
     */
    void deleteOrder(Long id);

    /**
    * 批量删除订单信息
    *
    * @param ids 编号
    */
    void deleteOrderListByIds(List<Long> ids);

    /**
     * 获得订单信息
     *
     * @param id 编号
     * @return 订单信息
     */
    OrderDO getOrder(Long id);

    /**
     * 获得订单信息
     *
     * @param orderNo 工单号
     * @return 订单信息
     */
    OrderDO getOrderByOrderNo(String orderNo);

    /**
     * 获得订单信息分页
     *
     * @param pageReqVO 分页查询
     * @return 订单信息分页
     */
    PageResult<OrderDO> getOrderPage(OrderPageReqVO pageReqVO);


    // ==================== 子表（订单明细） ====================

    /**
     * 获得订单明细列表
     *
     * @param orderNo 订单号
     * @return 订单明细列表
     */
    List<OrderDetailDO> getOrderDetailListByOrderNo(String orderNo);

}
