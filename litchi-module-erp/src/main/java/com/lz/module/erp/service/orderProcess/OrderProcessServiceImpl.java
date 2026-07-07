package com.lz.module.erp.service.orderProcess;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.lz.module.erp.controller.admin.orderProcess.vo.*;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.util.object.BeanUtils;

import com.lz.module.erp.dal.mysql.orderProcess.OrderProcessMapper;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.framework.common.util.collection.CollectionUtils.convertList;
import static com.lz.framework.common.util.collection.CollectionUtils.diffList;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

/**
 * 订单工序 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
public class OrderProcessServiceImpl implements OrderProcessService {

    @Resource
    private OrderProcessMapper orderProcessMapper;

    @Override
    public Long createOrderProcess(OrderProcessSaveReqVO createReqVO) {
        // 插入
        OrderProcessDO orderProcess = BeanUtils.toBean(createReqVO, OrderProcessDO.class);
        orderProcessMapper.insert(orderProcess);

        // 返回
        return orderProcess.getId();
    }

    @Override
    public void updateOrderProcess(OrderProcessSaveReqVO updateReqVO) {
        // 校验存在
        validateOrderProcessExists(updateReqVO.getId());
        // 更新
        OrderProcessDO updateObj = BeanUtils.toBean(updateReqVO, OrderProcessDO.class);
        orderProcessMapper.updateById(updateObj);
    }

    @Override
    public void deleteOrderProcess(Long id) {
        // 校验存在
        validateOrderProcessExists(id);
        // 删除
        orderProcessMapper.deleteById(id);
    }

    @Override
        public void deleteOrderProcessListByIds(List<Long> ids) {
        // 删除
        orderProcessMapper.deleteByIds(ids);
        }


    private void validateOrderProcessExists(Long id) {
        if (orderProcessMapper.selectById(id) == null) {
            throw exception(ORDER_PROCESS_NOT_EXISTS);
        }
    }

    @Override
    public OrderProcessDO getOrderProcess(Long id) {
        return orderProcessMapper.selectById(id);
    }

    @Override
    public PageResult<OrderProcessDO> getOrderProcessPage(OrderProcessPageReqVO pageReqVO) {
        return orderProcessMapper.selectPage(pageReqVO);
    }


}