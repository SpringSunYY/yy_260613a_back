package com.lz.module.erp.service.order;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.module.erp.controller.admin.order.vo.OrderPageReqVO;
import com.lz.module.erp.controller.admin.order.vo.OrderSaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import com.lz.module.erp.dal.mysql.order.OrderDetailMapper;
import com.lz.module.erp.dal.mysql.order.OrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.framework.common.util.collection.CollectionUtils.convertList;
import static com.lz.framework.common.util.collection.CollectionUtils.diffList;
import static com.lz.module.erp.enums.ErrorCodeConstants.ORDER_NOT_EXISTS;

/**
 * 订单信息 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderSaveReqVO createReqVO) {
        // 插入
        OrderDO order = BeanUtils.toBean(createReqVO, OrderDO.class);
        orderMapper.insert(order);


        // 插入子表
        createOrderDetailList(order.getOrderNo(), createReqVO.getOrderDetails());
        // 返回
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(OrderSaveReqVO updateReqVO) {
        // 校验存在
        validateOrderExists(updateReqVO.getId());
        // 更新
        OrderDO updateObj = BeanUtils.toBean(updateReqVO, OrderDO.class);
        orderMapper.updateById(updateObj);

        // 更新子表
        updateOrderDetailList(updateReqVO.getOrderNo(), updateReqVO.getOrderDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        // 校验存在
        OrderDO orderDO = validateOrderExists(id);
        // 删除
        orderMapper.deleteById(id);

        // 删除子表
        deleteOrderDetailByOrderNo(orderDO.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderListByIds(List<Long> ids) {
        ids.forEach(this::deleteOrder);
    }


    private OrderDO validateOrderExists(Long id) {
        OrderDO orderDO = orderMapper.selectById(id);
        if (orderDO == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
        return orderDO;
    }

    @Override
    public OrderDO getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public PageResult<OrderDO> getOrderPage(OrderPageReqVO pageReqVO) {
        return orderMapper.selectPage(pageReqVO);
    }


    // ==================== 子表（订单明细） ====================

    @Override
    public List<OrderDetailDO> getOrderDetailListByOrderNo(String orderNo) {
        return orderDetailMapper.selectListByOrderNo(orderNo);
    }

    private void createOrderDetailList(String orderNo, List<OrderDetailDO> list) {
        list.forEach(o -> o.setOrderNo(orderNo).clean());
        orderDetailMapper.insertBatch(list);
    }

    private void updateOrderDetailList(String orderNo, List<OrderDetailDO> list) {
        list.forEach(o -> o.setOrderNo(orderNo).clean());
        List<OrderDetailDO> oldList = orderDetailMapper.selectListByOrderNo(orderNo);
        List<List<OrderDetailDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).clean(); // 解决更新情况下：updateTime 不更新
            }
            return same;
        });

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            orderDetailMapper.insertBatch(diffList.getFirst());
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            orderDetailMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            orderDetailMapper.deleteByIds(convertList(diffList.get(2), OrderDetailDO::getId));
        }
    }

    private void deleteOrderDetailByOrderNo(String orderNo) {
        orderDetailMapper.deleteByOrderNo(orderNo);
    }

    private void deleteOrderDetailByOrderNos(List<String> orderNos) {
        orderDetailMapper.deleteByOrderNos(orderNos);
    }


}
