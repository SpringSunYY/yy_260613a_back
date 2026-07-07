package com.lz.module.erp.service.orderVector;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.lz.module.erp.controller.admin.orderVector.vo.*;
import com.lz.module.erp.dal.dataobject.orderVector.OrderVectorDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.util.object.BeanUtils;

import com.lz.module.erp.dal.mysql.orderVector.OrderVectorMapper;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.framework.common.util.collection.CollectionUtils.convertList;
import static com.lz.framework.common.util.collection.CollectionUtils.diffList;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

/**
 * 订单向量 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
public class OrderVectorServiceImpl implements OrderVectorService {

    @Resource
    private OrderVectorMapper orderVectorMapper;

    @Override
    public Long createOrderVector(OrderVectorSaveReqVO createReqVO) {
        // 插入
        OrderVectorDO orderVector = BeanUtils.toBean(createReqVO, OrderVectorDO.class);
        orderVectorMapper.insert(orderVector);

        // 返回
        return orderVector.getId();
    }

    @Override
    public void updateOrderVector(OrderVectorSaveReqVO updateReqVO) {
        // 校验存在
        validateOrderVectorExists(updateReqVO.getId());
        // 更新
        OrderVectorDO updateObj = BeanUtils.toBean(updateReqVO, OrderVectorDO.class);
        orderVectorMapper.updateById(updateObj);
    }

    @Override
    public void deleteOrderVector(Long id) {
        // 校验存在
        validateOrderVectorExists(id);
        // 删除
        orderVectorMapper.deleteById(id);
    }

    @Override
        public void deleteOrderVectorListByIds(List<Long> ids) {
        // 删除
        orderVectorMapper.deleteByIds(ids);
        }


    private void validateOrderVectorExists(Long id) {
        if (orderVectorMapper.selectById(id) == null) {
            throw exception(ORDER_VECTOR_NOT_EXISTS);
        }
    }

    @Override
    public OrderVectorDO getOrderVector(Long id) {
        return orderVectorMapper.selectById(id);
    }

    @Override
    public PageResult<OrderVectorDO> getOrderVectorPage(OrderVectorPageReqVO pageReqVO) {
        return orderVectorMapper.selectPage(pageReqVO);
    }


}