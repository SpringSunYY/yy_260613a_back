package com.lz.module.erp.service.orderAudit;

import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.module.erp.controller.admin.orderAudit.vo.OrderAuditPageReqVO;
import com.lz.module.erp.controller.admin.orderAudit.vo.OrderAuditSaveReqVO;
import com.lz.module.erp.dal.dataobject.orderAudit.OrderAuditDO;
import com.lz.module.erp.dal.mysql.orderAudit.OrderAuditMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.module.erp.enums.ErrorCodeConstants.ORDER_AUDIT_NOT_EXISTS;

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

    @Override
    public Long createOrderAudit(OrderAuditSaveReqVO createReqVO) {
        // 插入
        OrderAuditDO orderAudit = BeanUtils.toBean(createReqVO, OrderAuditDO.class);
        orderAuditMapper.insert(orderAudit);

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
        return orderAuditMapper.selectPage(pageReqVO);
    }


}
