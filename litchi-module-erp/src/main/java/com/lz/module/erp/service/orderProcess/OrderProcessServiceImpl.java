package com.lz.module.erp.service.orderProcess;

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
import com.lz.module.erp.enums.ErpOrderAuditStatusEnum;
import com.lz.module.erp.enums.ErpOrderCurrentProcessEnum;
import com.lz.module.erp.enums.PerConstants;
import com.lz.module.erp.service.order.OrderService;
import com.lz.module.erp.service.orderProcessHistory.OrderProcessHistoryService;
import com.lz.module.system.api.user.AdminUserApi;
import com.lz.module.system.api.user.dto.AdminUserSimpRespDTO;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lz.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.module.erp.enums.ErrorCodeConstants.ORDER_AUDIT_STATUS_NO_APPROVE;
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
    @Lazy
    private OrderService orderService;

    @Resource
    private OrderProcessHistoryService orderProcessHistoryService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private SecurityFrameworkService securityFrameworkService;


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
    public void updateOrderProcess(OrderProcessSaveReqVO reqVO) {
        //必须要有图片
        // 校验存在
        OrderProcessDO processDO = validateOrderProcessExists(reqVO.getId());
        //校验订单是否存在
        OrderDO orderDO = orderService.validateOrderExistsByNo(processDO.getOrderNo());
        // 如果订单还没有审核通过
        if (!orderDO.getAuditStatus().equals(ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_3.getStatus())) {
            throw exception(ORDER_AUDIT_STATUS_NO_APPROVE);
        }
        //判断冗余数据是否一致，如果不一致要更新订单的数据
        if (isRedundantDataChanged(processDO, orderDO, reqVO)) {
            orderService.initOrderByProcess(orderDO, BeanUtils.toBean(processDO, OrderProcessSaveReqVO.class));
            orderService.updateOrder(orderDO);
        }
        //判断工序是否一致
        if (isFieldChanged(reqVO.getCurrentProcess(), orderDO.getCurrentProcess())
                || isFieldChanged(reqVO.getCurrentProcess(), processDO.getCurrentProcess())) {
            createProcessHistory(reqVO.getOrderNo(), processDO.getCurrentProcess(), reqVO.getCurrentProcess());
        }
        // 更新
        orderProcessMapper.updateById(BeanUtils.toBean(reqVO, OrderProcessDO.class));
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
    public void updateProcessToTargetProcessByNo(String orderNo, String oldProcess, String currentProcess) {
        createProcessHistory(orderNo, oldProcess, currentProcess);
        LambdaUpdateWrapper<OrderProcessDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderProcessDO::getOrderNo, orderNo);
        updateWrapper.set(OrderProcessDO::getCurrentProcess, currentProcess);
        updateWrapper.set(OrderProcessDO::getUpdateTime, LocalDateTime.now());
        updateWrapper.set(OrderProcessDO::getUpdater, SecurityFrameworkUtils.getLoginUserId());
        orderProcessMapper.update(updateWrapper);
    }

    private void createProcessHistory(String orderNo, String oldProcess, String currentProcess) {
        OrderProcessHistorySaveReqVO createReqVO = new OrderProcessHistorySaveReqVO();
        createReqVO.setOrderNo(orderNo);
        createReqVO.setOldProcess(oldProcess);
        createReqVO.setCurrentProcess(currentProcess);
        orderProcessHistoryService.createOrderProcessHistory(createReqVO);
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
        }
        if (!hasPermission) {
            throw exception(FORBIDDEN);
        }
        this.updateOrderProcess(reqVO);
    }

    /**
     * 判断工序/订单中的冗余字段是否与请求数据不一致
     */
    private boolean isRedundantDataChanged(OrderProcessDO processDO, OrderDO orderDO, OrderProcessSaveReqVO reqVO) {
        return isRedundantChanged(processDO.getCurrentProcess(), processDO.getOrderImage(), processDO.getQrCode(),
                processDO.getPattern(), processDO.getFabric(), processDO.getSpecification(), reqVO)
                || isRedundantChanged(orderDO.getCurrentProcess(), orderDO.getOrderImage(), orderDO.getQrCode(),
                orderDO.getPattern(), orderDO.getFabric(), orderDO.getSpecification(), reqVO);
    }

    /**
     * 判断任意冗余字段是否发生变化（数据库有值 且 与请求值不同）
     */
    private boolean isRedundantChanged(String currentProcess, String orderImage, String qrCode,
                                       String pattern, String fabric, String specification,
                                       OrderProcessSaveReqVO reqVO) {
        return isFieldChanged(currentProcess, reqVO.getCurrentProcess())
                || isFieldChanged(orderImage, reqVO.getOrderImage())
                || isFieldChanged(qrCode, reqVO.getQrCode())
                || isFieldChanged(pattern, reqVO.getPattern())
                || isFieldChanged(fabric, reqVO.getFabric())
                || isFieldChanged(specification, reqVO.getSpecification());
    }

    /**
     * 单个字段变化判断：数据库有值且与请求值不同
     */
    private boolean isFieldChanged(String dbValue, String reqValue) {
        return StrUtil.isNotEmpty(dbValue) && StrUtil.isNotEmpty(reqValue) && !reqValue.equals(dbValue);
    }
}
