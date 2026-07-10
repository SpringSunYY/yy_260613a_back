package com.lz.module.erp.service.orderAudit;

import java.util.*;
import jakarta.validation.*;
import com.lz.module.erp.controller.admin.orderAudit.vo.*;
import com.lz.module.erp.dal.dataobject.orderAudit.OrderAuditDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;

/**
 * 订单审核记录 Service 接口
 *
 * @author 荔枝软件
 */
public interface OrderAuditService {

    /**
     * 创建订单审核记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderAudit(@Valid OrderAuditSaveReqVO createReqVO);

    /**
     * 更新订单审核记录
     *
     * @param updateReqVO 更新信息
     */
    void updateOrderAudit(@Valid OrderAuditSaveReqVO updateReqVO);

    /**
     * 删除订单审核记录
     *
     * @param id 编号
     */
    void deleteOrderAudit(Long id);

    /**
    * 批量删除订单审核记录
    *
    * @param ids 编号
    */
    void deleteOrderAuditListByIds(List<Long> ids);

    /**
     * 获得订单审核记录
     *
     * @param id 编号
     * @return 订单审核记录
     */
    OrderAuditDO getOrderAudit(Long id);

    /**
     * 获得订单审核记录分页
     *
     * @param pageReqVO 分页查询
     * @return 订单审核记录分页
     */
    PageResult<OrderAuditDO> getOrderAuditPage(OrderAuditPageReqVO pageReqVO);


    /**
     * 获得订单审核记录列表
     *
     * @return 订单审核记录列表
     */
    List<OrderAuditDetailVO> getOrderAuditListByNo(String no);
}
