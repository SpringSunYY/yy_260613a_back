package com.lz.module.erp.service.orderAudit;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.module.erp.controller.admin.orderAudit.vo.OrderAuditDetailVO;
import com.lz.module.erp.controller.admin.orderAudit.vo.OrderAuditPageReqVO;
import com.lz.module.erp.controller.admin.orderAudit.vo.OrderAuditSaveReqVO;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistoryDetailVO;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.orderAudit.OrderAuditDO;
import com.lz.module.erp.dal.dataobject.orderProcessHistory.OrderProcessHistoryDO;
import com.lz.module.erp.dal.mysql.orderAudit.OrderAuditMapper;
import com.lz.module.erp.enums.ErpOrderAuditStatusEnum;
import com.lz.module.erp.enums.ErpOrderCurrentProcessEnum;
import com.lz.module.erp.service.order.OrderService;
import com.lz.module.erp.service.orderProcess.OrderProcessService;
import com.lz.module.system.api.user.AdminUserApi;
import com.lz.module.system.api.user.dto.AdminUserSimpRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

/**
 * 订单审核记录 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
public class OrderAuditServiceImpl implements OrderAuditService {

    @Resource
    private OrderAuditMapper orderAuditMapper;
    @Resource
    private OrderService orderService;
    @Resource
    private OrderProcessService orderProcessService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @DSTransactional
    public Long createOrderAudit(OrderAuditSaveReqVO createReqVO) {
        //如果传过来的权限不是同意或者拒绝
        if (createReqVO.getAuditStatus().equals(ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_3.getStatus())
                && createReqVO.getAuditStatus().equals(ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_4.getStatus())) {
            throw exception(ORDER_AUDIT_STATUS_APPROVE_ERROR);
        }
        //查询订单并判断是否已同意，已同意不可以再次提交审核
        OrderDO orderByOrderNo = orderService.validateOrderExistsByNo(createReqVO.getOrderNo());
        if (StrUtil.isNotEmpty(orderByOrderNo.getOrderStatus()) &&
                orderByOrderNo.getOrderStatus().equals(ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_3.getStatus())) {
            throw exception(ORDER_AUDIT_STATUE_APPROVE);
        }
        // 插入
        OrderAuditDO orderAudit = BeanUtils.toBean(createReqVO, OrderAuditDO.class);
        //原来的状态
        orderAudit.setOldAuditStatus(orderByOrderNo.getOrderStatus());
        orderAuditMapper.insert(orderAudit);
        //更新订单审核状态
        //如果是同意更新工序为待排版
        if (orderAudit.getAuditStatus().equals(ErpOrderAuditStatusEnum.ORDER_AUDIT_STATUS_3.getStatus())) {
            orderByOrderNo.setCurrentProcess(ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_2.getStatus());
            //更新工序
            orderProcessService.updateProcessToTargetProcessByNo(orderByOrderNo.getOrderNo(),orderByOrderNo.getCurrentProcess(), ErpOrderCurrentProcessEnum.ORDER_CURRENT_PROCESS_2.getStatus());
        }
        orderByOrderNo.setAuditStatus(orderAudit.getAuditStatus());
        orderService.updateOrder(orderByOrderNo);

        // 返回
        return orderAudit.getId();
    }

    @Override
    public void updateOrderAudit(OrderAuditSaveReqVO updateReqVO) {
        // 校验存在
        validateOrderAuditExists(updateReqVO.getId());
        // 更新
        OrderAuditDO updateObj = BeanUtils.toBean(updateReqVO, OrderAuditDO.class);
        orderAuditMapper.updateById(updateObj);
    }

    @Override
    public void deleteOrderAudit(Long id) {
        // 校验存在
        validateOrderAuditExists(id);
        // 删除
        orderAuditMapper.deleteById(id);
    }

    @Override
    public void deleteOrderAuditListByIds(List<Long> ids) {
        // 删除
        orderAuditMapper.deleteByIds(ids);
    }


    private void validateOrderAuditExists(Long id) {
        if (orderAuditMapper.selectById(id) == null) {
            throw exception(ORDER_AUDIT_NOT_EXISTS);
        }
    }

    @Override
    public OrderAuditDO getOrderAudit(Long id) {
        return orderAuditMapper.selectById(id);
    }

    @Override
    public PageResult<OrderAuditDO> getOrderAuditPage(OrderAuditPageReqVO pageReqVO) {
        PageResult<OrderAuditDO> orderAuditDOPageResult = orderAuditMapper.selectPage(pageReqVO);
        //构建创建人
        //提取所有的创建人
        List<String> creatorIds = orderAuditDOPageResult.getList()
                .stream().map(OrderAuditDO::getCreator).distinct().toList();
        List<AdminUserSimpRespDTO> userSimpList = adminUserApi.getUserSimpList(creatorIds);
        //根据id转为map
        Map<String, AdminUserSimpRespDTO> userSimpMap = userSimpList.stream()
                .collect(Collectors.toMap(AdminUserSimpRespDTO::getId, v -> v));
        orderAuditDOPageResult.getList().forEach(orderDO -> {
            orderDO.setCreator(userSimpMap.getOrDefault(orderDO.getCreator(),new AdminUserSimpRespDTO()).getNickname());
        });
        return orderAuditDOPageResult;
    }

    @Override
    public List<OrderAuditDetailVO> getOrderAuditListByNo(String no) {
        List<OrderAuditDO> dos = orderAuditMapper.selectList(
                new LambdaQueryWrapper<>(OrderAuditDO.class)
                .eq(OrderAuditDO::getOrderNo, no)
                .orderByDesc(OrderAuditDO::getCreateTime));
        if (dos.isEmpty()) {
            return Collections.emptyList();
        }
        //转为vo
        List<OrderAuditDetailVO> detailVOS = BeanUtils.toBean(dos, OrderAuditDetailVO.class);
        //构建创建人
        //提取所有的创建人
        List<String> creatorIds = detailVOS
                .stream().map(OrderAuditDetailVO::getCreator).distinct().toList();
        List<AdminUserSimpRespDTO> userSimpList = adminUserApi.getUserSimpList(creatorIds);
        //根据id转为map
        Map<String, AdminUserSimpRespDTO> userSimpMap = userSimpList.stream()
                .collect(Collectors.toMap(AdminUserSimpRespDTO::getId, v -> v));
        detailVOS.forEach(detailVO -> {
            AdminUserSimpRespDTO user = userSimpMap.getOrDefault(detailVO.getCreator(), new AdminUserSimpRespDTO());
            detailVO.setCreator(user.getNickname());
            detailVO.setAvatar(user.getAvatar());
        });
        return detailVOS;
    }


}
