package com.lz.module.erp.service.order;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.common.util.object.ObjectUtils;
import com.lz.module.erp.controller.admin.order.vo.OrderDetailSaveReqVO;
import com.lz.module.erp.controller.admin.order.vo.OrderPageReqVO;
import com.lz.module.erp.controller.admin.order.vo.OrderSaveReqVO;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.dal.mysql.order.OrderDetailMapper;
import com.lz.module.erp.dal.mysql.order.OrderMapper;
import com.lz.module.erp.dal.mysql.orderProcess.OrderProcessMapper;
import com.lz.module.erp.enums.ErpOrderAuditStatusEnum;
import com.lz.module.erp.enums.ErpOrderCurrentProcessEnum;
import com.lz.module.erp.enums.ErpOrderPrintStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.framework.common.util.collection.CollectionUtils.convertList;
import static com.lz.framework.common.util.collection.CollectionUtils.diffList;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

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
    @Resource
    private OrderProcessMapper orderProcessMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderSaveReqVO createReqVO) {
        //校验订单是否存在
        validateOrderExists(null, createReqVO.getOrderNo());

        // 插入
        OrderDO order = BeanUtils.toBean(createReqVO, OrderDO.class);
        //初始化数据，显示状态自己的
        order.setAuditStatus(ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_1.getStatus());
        order.setPrintStatus(ErpOrderPrintStatusEnum.ORDER_PRINT_STATUS_0.getStatus());
        OrderProcessSaveReqVO orderProcess = createReqVO.getOrderProcess();
        orderProcess.setCurrentProcess(ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_1.getStatus());
        // 插入子表
        int total = createOrderDetailList(order.getOrderNo(), createReqVO.getOrderDetails());
        //根据工序
        initOrderByProcess(order, orderProcess);
        order.setNumber(total);

        //创建工序
        OrderProcessDO orderProcessDO = BeanUtils.toBean(orderProcess, OrderProcessDO.class);
        orderProcessMapper.insert(orderProcessDO);
        orderMapper.insert(order);

        // 返回
        return order.getId();
    }

    private void initOrderByProcess(OrderDO order, OrderProcessSaveReqVO orderProcess) {
        order.setCurrentProcess(orderProcess.getCurrentProcess());
        order.setOrderImage(orderProcess.getOrderImage());
        order.setQrCode(orderProcess.getQrCode());
        order.setPattern(orderProcess.getPattern());
        order.setFabric(orderProcess.getFabric());
        order.setSpecification(orderProcess.getSpecification());

        orderProcess.setOrderNo(order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(OrderSaveReqVO updateReqVO) {
        // 校验存在
        OrderDO orderDO = validateOrderExists(updateReqVO.getId(), updateReqVO.getOrderNo());
        // 更新
        OrderDO updateObj = BeanUtils.toBean(updateReqVO, OrderDO.class);

        // 更新子表
        int total = updateOrderDetailList(orderDO.getOrderNo(), updateReqVO.getOrderNo(), updateReqVO.getOrderDetails());
        updateObj.setNumber(total);
        //更新工序
        OrderProcessSaveReqVO orderProcess = updateReqVO.getOrderProcess();
        initOrderByProcess(updateObj, orderProcess);
        updateOrderProcess(orderDO.getOrderNo(), updateObj.getOrderNo(), orderProcess);
        orderMapper.updateById(updateObj);
    }

    /**
     * 更新工序
     *
     * @param oldNo        数据库内的工单号
     * @param newNo        新的工单号
     * @param orderProcess 工序
     */
    private void updateOrderProcess(String oldNo, String newNo, OrderProcessSaveReqVO orderProcess) {
        //查询的No,如果不一样的话，则查询数据库内的
        boolean hasNewNo = oldNo.equals(newNo);
        String queryNo = hasNewNo ? newNo : oldNo;
        OrderProcessDO orderProcessDO = orderProcessMapper.selectOne(OrderProcessDO::getOrderNo, queryNo);
        //如果不存在表示新增
        if (ObjectUtils.isNull(orderProcessDO)) {
            orderProcess.setCurrentProcess(ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_1.getStatus());
            orderProcessMapper.insert(BeanUtils.toBean(orderProcess, OrderProcessDO.class));
            return ;
        }
        //赋值id
        orderProcess.setId(orderProcessDO.getId());
        orderProcessDO.setOrderNo(newNo);
        orderProcessDO = BeanUtils.toBean(orderProcess, OrderProcessDO.class);
        orderProcessMapper.updateById(orderProcessDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        // 校验存在
        OrderDO orderDO = validateOrderExists(id, null);
        // 删除
        orderMapper.deleteById(id);

        // 删除子表
        deleteOrderDetailByOrderNo(orderDO.getOrderNo());
        // 删除工序
        orderProcessMapper.delete(OrderProcessDO::getOrderNo, orderDO.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderListByIds(List<Long> ids) {
        ids.forEach(this::deleteOrder);
    }


    private OrderDO validateOrderExists(Long id, String orderNo) {
        //如果订单和id都不传过来
        if (ObjectUtil.isEmpty(id) && ObjectUtil.isEmpty(orderNo)) {
            throw exception(ORDER_NOT_EXISTS);
        }
        OrderDO orderDO = new OrderDO();
        if (ObjectUtil.isNotEmpty(id)) {
            orderDO = orderMapper.selectById(id);
            if (orderDO == null) {
                throw exception(ORDER_NOT_EXISTS);
            }
        }
        if (ObjectUtil.isNotEmpty(orderNo)) {
            OrderDO orderDOByNo = orderMapper.selectOne(OrderDO::getOrderNo, orderNo);
            if (orderDOByNo != null && !orderDOByNo.getId().equals(id)) {
                throw exception(ORDER_EXISTS);
            }
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

    private int createOrderDetailList(String orderNo, List<OrderDetailSaveReqVO> list) {
        list.forEach(o -> o.setOrderNo(orderNo));
        List<OrderDetailDO> orderDetailDOS = BeanUtils.toBean(list, OrderDetailDO.class);
        orderDetailMapper.insertBatch(orderDetailDOS);
        return orderDetailDOS.stream().mapToInt(OrderDetailDO::getSetQuantity).sum();
    }

    private int updateOrderDetailList(String oldNo, String newNo, List<OrderDetailSaveReqVO> list) {
        list.forEach(o -> o.setOrderNo(newNo));
        //使用老的订单号查询
        List<OrderDetailDO> oldList = orderDetailMapper.selectListByOrderNo(oldNo);
        List<OrderDetailDO> newList = BeanUtils.toBean(list, OrderDetailDO.class);
        List<List<OrderDetailDO>> diffList = diffList(oldList, newList, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).setOrderNo(newNo).clean(); // 解决更新情况下：updateTime 不更新
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
        //计算总数
        int total = 0;
        for (List<OrderDetailDO> orderDetailDOS : diffList) {
            total += orderDetailDOS.stream().mapToInt(OrderDetailDO::getSetQuantity).sum();
        }
        return total;
    }

    private void deleteOrderDetailByOrderNo(String orderNo) {
        orderDetailMapper.deleteByOrderNo(orderNo);
    }

    private void deleteOrderDetailByOrderNos(List<String> orderNos) {
        orderDetailMapper.deleteByOrderNos(orderNos);
    }


}
