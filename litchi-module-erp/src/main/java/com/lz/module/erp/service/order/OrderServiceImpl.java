package com.lz.module.erp.service.order;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lz.framework.common.biz.system.dict.DictDataCommonApi;
import com.lz.framework.common.biz.system.dict.dto.DictDataRespDTO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.common.util.object.ObjectUtils;
import com.lz.module.erp.controller.admin.order.vo.*;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.dal.mysql.order.OrderDetailMapper;
import com.lz.module.erp.dal.mysql.order.OrderMapper;
import com.lz.module.erp.dal.mysql.orderProcess.OrderProcessMapper;
import com.lz.module.erp.enums.ErpDictTypeConstants;
import com.lz.module.erp.enums.ErpOrderAuditStatusEnum;
import com.lz.module.erp.enums.ErpOrderCurrentProcessEnum;
import com.lz.module.erp.enums.ErpOrderPrintStatusEnum;
import com.lz.module.erp.service.orderProcess.OrderProcessService;
import com.lz.module.erp.service.orderVector.OrderVectorService;
import com.lz.module.system.api.user.AdminUserApi;
import com.lz.module.system.api.user.dto.AdminUserSimpRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Resource
    private OrderProcessMapper orderProcessMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    @Lazy
    private OrderProcessService orderProcessService;
    @Resource
    private ThreadPoolTaskExecutor executor;

    @Resource
    private OrderVectorService orderVectorService;

    @Resource
    private DictDataCommonApi dictDataCommonApi;

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
        orderProcessService.createOrderProcess(orderProcess);
        orderMapper.insert(order);

        // 返回
        return order.getId();
    }

    @Override
    public void initOrderByProcess(OrderDO order, OrderProcessSaveReqVO orderProcess) {
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
        OrderDO orderDO = validateOrderExists(updateReqVO.getId(), null);
        //如果两个订单号不一样，不可以修改订单号
        if (!orderDO.getOrderNo().equals(updateReqVO.getOrderNo())) {
            throw exception(ORDER_NO_NOT_EQUALS);
        }

        // 更新子表
        int total = updateOrderDetailList(orderDO.getOrderNo(), updateReqVO.getOrderDetails());
        orderDO.setNumber(total);
        //更新工序
        OrderProcessSaveReqVO orderProcess = updateReqVO.getOrderProcess();
        initOrderByProcess(orderDO, orderProcess);
        updateOrderProcess(orderDO.getOrderNo(), orderProcess);
    }

    @Override
    public void updateOrder(OrderDO orderDO) {
        orderMapper.updateById(orderDO);
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public void shipOrder(OrderShipReqVO shipReqVO) {
        shipReqVO.setCurrentProcess(ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_7.getStatus());
        orderProcessService.updateOrderProcess(BeanUtils.toBean(shipReqVO, OrderProcessSaveReqVO.class));
        orderMapper.updateById(BeanUtils.toBean(shipReqVO, OrderDO.class));
        //需要判断是否已经发货，并且构建向量
        validateOrderShip(shipReqVO);
    }

    /**
     * 更新工序
     *
     * @param orderNo      订单号
     * @param orderProcess 工序
     */
    private void updateOrderProcess(String orderNo, OrderProcessSaveReqVO orderProcess) {
        OrderProcessDO orderProcessDO = orderProcessMapper.selectOne(OrderProcessDO::getOrderNo, orderNo);
        //如果不存在表示新增
        if (ObjectUtils.isNull(orderProcessDO)) {
            orderProcess.setCurrentProcess(ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_1.getStatus());
            orderProcessMapper.insert(BeanUtils.toBean(orderProcess, OrderProcessDO.class));
            return;
        }
        //赋值id
        orderProcess.setId(orderProcessDO.getId());
        orderProcessDO.setOrderNo(orderNo);
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


    /**
     * 校验订单信息是否存在，这里返回的还是根据id查询的订单信息
     * Id校验不存在，工单号校验存在
     *
     * @param id      订单信息id
     * @param orderNo 工单号
     * @return 订单信息
     */
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

    /**
     * 校验订单信息是否存在
     *
     * @param orderNo 工单号
     * @return 订单信息
     */
    @Override
    public OrderDO validateOrderExistsByNo(String orderNo) {
        OrderDO order = orderMapper.selectOne(OrderDO::getOrderNo, orderNo);
        if (order == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public OrderDO getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public OrderDO getOrderByOrderNo(String orderNo) {
        return orderMapper.selectOne(OrderDO::getOrderNo, orderNo);
    }

    @Override
    public PageResult<OrderDO> getOrderPage(OrderPageReqVO pageReqVO) {
        PageResult<OrderDO> orderDOPageResult = orderMapper.selectPage(pageReqVO);
        return buildPageResult(orderDOPageResult);
    }

    @Override
    public PageResult<OrderDO> getShipOrderPage(OrderPageReqVO pageReqVO) {
        PageResult<OrderDO> orderDOPageResult = orderMapper.selectShipPage(pageReqVO);
        return buildPageResult(orderDOPageResult);
    }

    private @NonNull PageResult<OrderDO> buildPageResult(PageResult<OrderDO> orderDOPageResult) {
        //构建创建人
        //提取所有的创建人
        List<String> creatorIds = orderDOPageResult.getList()
                .stream().map(OrderDO::getCreator).distinct().toList();
        List<AdminUserSimpRespDTO> userSimpList = adminUserApi.getUserSimpList(creatorIds);
        //根据id转为map
        Map<String, AdminUserSimpRespDTO> userSimpMap = userSimpList.stream()
                .collect(Collectors.toMap(AdminUserSimpRespDTO::getId, v -> v));
        orderDOPageResult.getList().forEach(orderDO -> {
            orderDO.setCreator(userSimpMap.getOrDefault(orderDO.getCreator(), new AdminUserSimpRespDTO()).getNickname());
        });
        return orderDOPageResult;
    }

    @Override
    public void submitAuditOrder(OrderAuditReqVO auditReqVO) {
        //校验存在
        OrderDO orderDO = validateOrderExists(auditReqVO.getId(), null);
        //如果状态不是待审核
        if (!ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_1.getStatus().equals(orderDO.getAuditStatus())) {
            throw exception(ORDER_AUDIT_STATUS_ERROR);
        }
        //直接更新订单状态为待审核
        LambdaUpdateWrapper<OrderDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderDO::getId, auditReqVO.getId());
        updateWrapper.set(OrderDO::getAuditStatus, ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_2.getStatus());
        orderMapper.update(updateWrapper);
    }


    // ==================== 子表（订单明细） ====================

    @Override
    public List<OrderDetailDO> getOrderDetailListByOrderNo(String orderNo) {
        List<OrderDetailDO> orderDetailDOS = orderDetailMapper.selectListByOrderNo(orderNo);
        List<DictDataRespDTO> dataList = dictDataCommonApi.getDictDataList(ErpDictTypeConstants.ERP_SET_SIZE);
        Map<String, Integer> sortMap = dataList.stream()
                .collect(Collectors.toMap(DictDataRespDTO::getValue, DictDataRespDTO::getSort));
        orderDetailDOS.sort(Comparator.comparingInt(o -> sortMap.getOrDefault(o.getSetSize(), Integer.MAX_VALUE)));
        return orderDetailDOS;
    }

    private int createOrderDetailList(String orderNo, List<OrderDetailSaveReqVO> list) {
        list.forEach(o -> o.setOrderNo(orderNo));
        List<OrderDetailDO> orderDetailDOS = BeanUtils.toBean(list, OrderDetailDO.class);
        orderDetailMapper.insertBatch(orderDetailDOS);
        return orderDetailDOS.stream().mapToInt(OrderDetailDO::getSetQuantity).sum();
    }

    private int updateOrderDetailList(String orderNo, List<OrderDetailSaveReqVO> list) {
        list.forEach(o -> o.setOrderNo(orderNo));
        //使用老的订单号查询
        List<OrderDetailDO> oldList = orderDetailMapper.selectListByOrderNo(orderNo);
        List<OrderDetailDO> newList = BeanUtils.toBean(list, OrderDetailDO.class);
        List<List<OrderDetailDO>> diffList = diffList(oldList, newList, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).setOrderNo(orderNo).clean(); // 解决更新情况下：updateTime 不更新
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

    private void validateOrderShip(OrderShipReqVO reqVO) {
        //查询订单
        OrderDO orderDO = this.getOrderByOrderNo(reqVO.getOrderNo());
        if (ObjUtil.isNull(orderDO)) {
            throw exception(ORDER_NOT_EXISTS);
        }
        //如果还没有发货
        if (ObjUtil.isNull(orderDO.getShippingTime())) {
            throw exception(ORDER_NOT_SHIPPED);
        }
        //判断是否有图片
        if (StrUtil.isEmpty(reqVO.getOrderImage())) {
            throw exception(ORDER_NOT_ORDER_IMAGE);
        }
        //异步去构建向量
        executor.execute(() -> {
            orderVectorService.indexOrderVector(reqVO.getOrderNo(), reqVO.getOrderImage());
        });
    }
}
