package com.lz.module.erp.service.orderProcessHistory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.*;
import com.lz.module.erp.dal.dataobject.orderProcessHistory.OrderProcessHistoryDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.util.object.BeanUtils;

import com.lz.module.erp.dal.mysql.orderProcessHistory.OrderProcessHistoryMapper;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.framework.common.util.collection.CollectionUtils.convertList;
import static com.lz.framework.common.util.collection.CollectionUtils.diffList;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

/**
 * 订单工序记录 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
public class OrderProcessHistoryServiceImpl implements OrderProcessHistoryService {

    @Resource
    private OrderProcessHistoryMapper orderProcessHistoryMapper;

    @Override
    public Long createOrderProcessHistory(OrderProcessHistorySaveReqVO createReqVO) {
        // 插入
        OrderProcessHistoryDO orderProcessHistory = BeanUtils.toBean(createReqVO, OrderProcessHistoryDO.class);
        orderProcessHistoryMapper.insert(orderProcessHistory);

        // 返回
        return orderProcessHistory.getId();
    }

    @Override
    public void updateOrderProcessHistory(OrderProcessHistorySaveReqVO updateReqVO) {
        // 校验存在
        validateOrderProcessHistoryExists(updateReqVO.getId());
        // 更新
        OrderProcessHistoryDO updateObj = BeanUtils.toBean(updateReqVO, OrderProcessHistoryDO.class);
        orderProcessHistoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteOrderProcessHistory(Long id) {
        // 校验存在
        validateOrderProcessHistoryExists(id);
        // 删除
        orderProcessHistoryMapper.deleteById(id);
    }

    @Override
        public void deleteOrderProcessHistoryListByIds(List<Long> ids) {
        // 删除
        orderProcessHistoryMapper.deleteByIds(ids);
        }


    private void validateOrderProcessHistoryExists(Long id) {
        if (orderProcessHistoryMapper.selectById(id) == null) {
            throw exception(ORDER_PROCESS_HISTORY_NOT_EXISTS);
        }
    }

    @Override
    public OrderProcessHistoryDO getOrderProcessHistory(Long id) {
        return orderProcessHistoryMapper.selectById(id);
    }

    @Override
    public PageResult<OrderProcessHistoryDO> getOrderProcessHistoryPage(OrderProcessHistoryPageReqVO pageReqVO) {
        return orderProcessHistoryMapper.selectPage(pageReqVO);
    }


}