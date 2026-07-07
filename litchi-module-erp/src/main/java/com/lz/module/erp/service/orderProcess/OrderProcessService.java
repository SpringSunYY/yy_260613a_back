package com.lz.module.erp.service.orderProcess;

import java.util.*;
import jakarta.validation.*;
import com.lz.module.erp.controller.admin.orderProcess.vo.*;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;

/**
 * 订单工序 Service 接口
 *
 * @author 荔枝软件
 */
public interface OrderProcessService {

    /**
     * 创建订单工序
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderProcess(@Valid OrderProcessSaveReqVO createReqVO);

    /**
     * 更新订单工序
     *
     * @param updateReqVO 更新信息
     */
    void updateOrderProcess(@Valid OrderProcessSaveReqVO updateReqVO);

    /**
     * 删除订单工序
     *
     * @param id 编号
     */
    void deleteOrderProcess(Long id);

    /**
    * 批量删除订单工序
    *
    * @param ids 编号
    */
    void deleteOrderProcessListByIds(List<Long> ids);

    /**
     * 获得订单工序
     *
     * @param id 编号
     * @return 订单工序
     */
    OrderProcessDO getOrderProcess(Long id);


    /**
     * 获得订单工序
     *
     * @param orderNo 工单号
     * @return 订单工序
     */
    OrderProcessDO getOrderProcessByOrderNo(String orderNo);
    /**
     * 获得订单工序分页
     *
     * @param pageReqVO 分页查询
     * @return 订单工序分页
     */
    PageResult<OrderProcessDO> getOrderProcessPage(OrderProcessPageReqVO pageReqVO);

}
