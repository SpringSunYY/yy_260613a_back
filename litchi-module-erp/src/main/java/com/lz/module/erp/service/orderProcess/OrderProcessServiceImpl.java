package com.lz.module.erp.service.orderProcess;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessPageReqVO;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.dal.mysql.orderProcess.OrderProcessMapper;
import com.lz.module.erp.service.order.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.module.erp.enums.ErrorCodeConstants.ORDER_NOT_EXISTS;
import static com.lz.module.erp.enums.ErrorCodeConstants.ORDER_PROCESS_NOT_EXISTS;

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

    @Resource
    private OrderService orderService;

    @Override
    public Long createOrderProcess(OrderProcessSaveReqVO createReqVO) {
        // 插入
        OrderProcessDO orderProcess = BeanUtils.toBean(createReqVO, OrderProcessDO.class);
        orderProcessMapper.insert(orderProcess);

        // 返回
        return orderProcess.getId();
    }

    @Override
    @DSTransactional
    public void updateOrderProcess(OrderProcessSaveReqVO updateReqVO) {
        // 校验存在
        OrderProcessDO orderProcessDO = validateOrderProcessExists(updateReqVO.getId());
        //校验订单是否存在
        OrderDO orderDO = validateOrderProcessExists(orderProcessDO.getOrderNo());
        //判断冗余数据是否一致，如果不一致要更新订单的数据
        if ((StrUtil.isNotEmpty(updateReqVO.getOrderImage()) && StrUtil.isNotEmpty(orderDO.getOrderImage()) && !updateReqVO.getOrderImage().equals(orderDO.getOrderImage()))
                || (StrUtil.isNotEmpty(updateReqVO.getQrCode()) && StrUtil.isNotEmpty(orderDO.getQrCode()) && !updateReqVO.getQrCode().equals(orderDO.getQrCode()))
                || !updateReqVO.getPattern().equals(orderDO.getPattern())
                || !updateReqVO.getFabric().equals(orderDO.getFabric())
                || !updateReqVO.getSpecification().equals(orderDO.getSpecification())) {
            orderService.initOrderByProcess(orderDO, updateReqVO);
            orderService.updateOrder(orderDO);
        }
        // 更新
        OrderProcessDO updateObj = BeanUtils.toBean(updateReqVO, OrderProcessDO.class);
        orderProcessMapper.updateById(updateObj);
    }

    private OrderDO validateOrderProcessExists(String orderNo) {
        OrderDO order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
        return order;
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


    private OrderProcessDO validateOrderProcessExists(Long id) {
        OrderProcessDO orderProcessDO = orderProcessMapper.selectById(id);
        if (orderProcessDO == null) {
            throw exception(ORDER_PROCESS_NOT_EXISTS);
        }
        return orderProcessDO;
    }

    @Override
    public OrderProcessDO getOrderProcess(Long id) {
        return orderProcessMapper.selectById(id);
    }

    @Override
    public OrderProcessDO getOrderProcessByOrderNo(String orderNo) {
        return orderProcessMapper.selectOne(OrderProcessDO::getOrderNo, orderNo);
    }

    @Override
    public PageResult<OrderProcessDO> getOrderProcessPage(OrderProcessPageReqVO pageReqVO) {
        return orderProcessMapper.selectPage(pageReqVO);
    }
}
