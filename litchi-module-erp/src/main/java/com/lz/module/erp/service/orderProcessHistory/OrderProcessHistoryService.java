package com.lz.module.erp.service.orderProcessHistory;

import java.util.*;
import jakarta.validation.*;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.*;
import com.lz.module.erp.dal.dataobject.orderProcessHistory.OrderProcessHistoryDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;

/**
 * 订单工序记录 Service 接口
 *
 * @author 荔枝软件
 */
public interface OrderProcessHistoryService {

    /**
     * 创建订单工序记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderProcessHistory(@Valid OrderProcessHistorySaveReqVO createReqVO);

    /**
     * 更新订单工序记录
     *
     * @param updateReqVO 更新信息
     */
    void updateOrderProcessHistory(@Valid OrderProcessHistorySaveReqVO updateReqVO);

    /**
     * 删除订单工序记录
     *
     * @param id 编号
     */
    void deleteOrderProcessHistory(Long id);

    /**
    * 批量删除订单工序记录
    *
    * @param ids 编号
    */
    void deleteOrderProcessHistoryListByIds(List<Long> ids);

    /**
     * 获得订单工序记录
     *
     * @param id 编号
     * @return 订单工序记录
     */
    OrderProcessHistoryDO getOrderProcessHistory(Long id);

    /**
     * 获得订单工序记录分页
     *
     * @param pageReqVO 分页查询
     * @return 订单工序记录分页
     */
    PageResult<OrderProcessHistoryDO> getOrderProcessHistoryPage(OrderProcessHistoryPageReqVO pageReqVO);


}