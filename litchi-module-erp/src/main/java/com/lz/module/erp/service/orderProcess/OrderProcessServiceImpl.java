package com.lz.module.erp.service.orderProcess;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessPageReqVO;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistorySaveReqVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.orderAudit.OrderAuditDO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.dal.mysql.orderProcess.OrderProcessMapper;
import com.lz.module.erp.service.order.OrderService;
import com.lz.module.erp.service.orderProcessHistory.OrderProcessHistoryService;
import com.lz.module.system.api.user.AdminUserApi;
import com.lz.module.system.api.user.dto.AdminUserSimpRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Resource
    private OrderProcessHistoryService orderProcessHistoryService;

    @Resource
    private AdminUserApi adminUserApi;
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
            orderDO.setCreator(userSimpMap.getOrDefault(orderDO.getCreator(),new AdminUserSimpRespDTO()).getNickname());
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
        orderProcessMapper.update(updateWrapper);
    }
}
