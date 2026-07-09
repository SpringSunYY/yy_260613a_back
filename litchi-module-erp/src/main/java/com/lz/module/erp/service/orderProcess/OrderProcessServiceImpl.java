package com.lz.module.erp.service.orderProcess;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.security.core.service.SecurityFrameworkService;
import com.lz.framework.security.core.util.SecurityFrameworkUtils;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessPageReqVO;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistorySaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.dal.mysql.orderProcess.OrderProcessMapper;
import com.lz.module.erp.enums.ErpOrderCurrentProcessEnum;
import com.lz.module.erp.enums.PerConstants;
import com.lz.module.erp.service.order.OrderService;
import com.lz.module.erp.service.orderProcessHistory.OrderProcessHistoryService;
import com.lz.module.erp.service.orderVector.OrderVectorService;
import com.lz.module.system.api.user.AdminUserApi;
import com.lz.module.system.api.user.dto.AdminUserSimpRespDTO;
import jakarta.annotation.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lz.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Resource
    private OrderService orderService;

    @Resource
    private OrderProcessHistoryService orderProcessHistoryService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Resource
    private ThreadPoolTaskExecutor executor;

    @Resource
    private OrderVectorService orderVectorService;
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
        OrderDO orderDO = orderService.validateOrderExistsByNo(orderProcessDO.getOrderNo());
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
        PageResult<OrderProcessDO> orderProcessDOPageResult = orderProcessMapper.selectPage(pageReqVO);
        //构建创建人
        //提取所有的创建人
        List<String> creatorIds = orderProcessDOPageResult.getList()
                .stream().map(OrderProcessDO::getCreator).distinct().toList();
        List<AdminUserSimpRespDTO> userSimpList = adminUserApi.getUserSimpList(creatorIds);
        //根据id转为map
        Map<String, AdminUserSimpRespDTO> userSimpMap = userSimpList.stream()
                .collect(Collectors.toMap(AdminUserSimpRespDTO::getId, v -> v));
        orderProcessDOPageResult.getList().forEach(orderDO -> {
            orderDO.setCreator(userSimpMap.getOrDefault(orderDO.getCreator(), new AdminUserSimpRespDTO()).getNickname());
        });
        return orderProcessDOPageResult;
    }

    @Override
    @DSTransactional
    public void updateProcessToTargetProcessByNo(String orderNo, String targetProcess) {
        //先查询工序是否存在
        OrderProcessDO orderProcessDO = orderProcessMapper.selectOne(OrderProcessDO::getOrderNo, orderNo);
        if (orderProcessDO == null) {
            throw exception(ORDER_PROCESS_NOT_EXISTS);
        }
        OrderProcessHistorySaveReqVO createReqVO = new OrderProcessHistorySaveReqVO();
        createReqVO.setOrderNo(orderNo);
        createReqVO.setOldProcess(orderProcessDO.getCurrentProcess());
        createReqVO.setCurrentProcess(targetProcess);
        orderProcessHistoryService.createOrderProcessHistory(createReqVO);
        LambdaUpdateWrapper<OrderProcessDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderProcessDO::getOrderNo, orderNo);
        updateWrapper.set(OrderProcessDO::getCurrentProcess, targetProcess);
        updateWrapper.set(OrderProcessDO::getUpdateTime, LocalDateTime.now());
        updateWrapper.set(OrderProcessDO::getUpdater, SecurityFrameworkUtils.getLoginUserId());
        orderProcessMapper.update(updateWrapper);
    }

    @Override
    public void updateProcessToTargetProcess(OrderProcessSaveReqVO reqVO) {
        boolean hasPermission = false;
        //根据不同状态判断权限
        if (ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_3.getStatus().equals(reqVO.getCurrentProcess())) {
            hasPermission = securityFrameworkService.hasPermission(
                    PerConstants.ERP_ORDER_PROCESS_LAYOUT);
        }
        if (ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_4.getStatus().equals(reqVO.getCurrentProcess())) {
            hasPermission = securityFrameworkService.hasPermission(
                    PerConstants.ERP_ORDER_PROCESS_PAPER);
        }
        if (ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_5.getStatus().equals(reqVO.getCurrentProcess())) {
            hasPermission = securityFrameworkService.hasPermission(
                    PerConstants.ERP_ORDER_PROCESS_ROLLER);
        }
        if (ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_6.getStatus().equals(reqVO.getCurrentProcess())) {
            hasPermission = securityFrameworkService.hasPermission(
                    PerConstants.ERP_ORDER_PROCESS_LASER);
        }
        if (ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_7.getStatus().equals(reqVO.getCurrentProcess())) {
            hasPermission = securityFrameworkService.hasPermission(
                    PerConstants.ERP_ORDER_PROCESS_SHIP);
            //需要判断是否已经发货
            validateOrderShip(reqVO);
        }
        if (!hasPermission) {
            throw exception(FORBIDDEN);
        }
        this.updateOrderProcess(reqVO);
    }

    private void validateOrderShip(OrderProcessSaveReqVO reqVO) {
        //查询订单
        OrderDO orderDO = orderService.getOrderByOrderNo(reqVO.getOrderNo());
        if (ObjUtil.isNull(orderDO)) {
            throw exception(ORDER_NOT_EXISTS);
        }
        //如果还没有发货
        if (ObjUtil.isNull(orderDO.getShippingTime())) {
            throw exception(ORDER_NOT_SHIPPED);
        }
        //异步去构建向量
        executor.execute(()->{
            orderVectorService.indexOrderVector(reqVO);
        });
    }
}
