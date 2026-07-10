package com.lz.module.erp.service.orderProcessHistory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.system.api.user.AdminUserApi;
import com.lz.module.system.api.user.dto.AdminUserSimpRespDTO;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import com.lz.module.erp.controller.admin.orderProcessHistory.vo.*;
import com.lz.module.erp.dal.dataobject.orderProcessHistory.OrderProcessHistoryDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.util.object.BeanUtils;

import com.lz.module.erp.dal.mysql.orderProcessHistory.OrderProcessHistoryMapper;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.framework.common.util.collection.CollectionUtils.convertList;
import static com.lz.framework.common.util.collection.CollectionUtils.diffList;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

/**
 * 订单工序记录 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
public class OrderProcessHistoryServiceImpl implements OrderProcessHistoryService {

    @Resource
    private OrderProcessHistoryMapper orderProcessHistoryMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Override
    public Long createOrderProcessHistory(OrderProcessHistorySaveReqVO createReqVO) {
        // 插入
        OrderProcessHistoryDO orderProcessHistory = BeanUtils.toBean(createReqVO, OrderProcessHistoryDO.class);
        orderProcessHistoryMapper.insert(orderProcessHistory);

        // 返回
        return orderProcessHistory.getId();
    }

    @Override
    public void updateOrderProcessHistory(OrderProcessHistorySaveReqVO updateReqVO) {
        // 校验存在
        validateOrderProcessHistoryExists(updateReqVO.getId());
        // 更新
        OrderProcessHistoryDO updateObj = BeanUtils.toBean(updateReqVO, OrderProcessHistoryDO.class);
        orderProcessHistoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteOrderProcessHistory(Long id) {
        // 校验存在
        validateOrderProcessHistoryExists(id);
        // 删除
        orderProcessHistoryMapper.deleteById(id);
    }

    @Override
        public void deleteOrderProcessHistoryListByIds(List<Long> ids) {
        // 删除
        orderProcessHistoryMapper.deleteByIds(ids);
        }


    private void validateOrderProcessHistoryExists(Long id) {
        if (orderProcessHistoryMapper.selectById(id) == null) {
            throw exception(ORDER_PROCESS_HISTORY_NOT_EXISTS);
        }
    }

    @Override
    public OrderProcessHistoryDO getOrderProcessHistory(Long id) {
        return orderProcessHistoryMapper.selectById(id);
    }

    @Override
    public PageResult<OrderProcessHistoryDO> getOrderProcessHistoryPage(OrderProcessHistoryPageReqVO pageReqVO) {
        PageResult<OrderProcessHistoryDO> orderProcessHistoryDOPageResult = orderProcessHistoryMapper.selectPage(pageReqVO);
        //构建创建人
        //提取所有的创建人
        List<String> creatorIds = orderProcessHistoryDOPageResult.getList()
                .stream().map(OrderProcessHistoryDO::getCreator).distinct().toList();
        List<AdminUserSimpRespDTO> userSimpList = adminUserApi.getUserSimpList(creatorIds);
        //根据id转为map
        Map<String, AdminUserSimpRespDTO> userSimpMap = userSimpList.stream()
                .collect(Collectors.toMap(AdminUserSimpRespDTO::getId, v -> v));
        orderProcessHistoryDOPageResult.getList().forEach(orderDO -> {
            orderDO.setCreator(userSimpMap.getOrDefault(orderDO.getCreator(),new AdminUserSimpRespDTO()).getNickname());
        });
        return orderProcessHistoryDOPageResult;
    }

    @Override
    public List<OrderProcessHistoryDetailVO> getOrderProcessHistoryByOrderNo(String no) {
        List<OrderProcessHistoryDO> dos = orderProcessHistoryMapper.selectList(new LambdaQueryWrapper<>(OrderProcessHistoryDO.class)
                .eq(OrderProcessHistoryDO::getOrderNo, no)
                .orderByDesc(OrderProcessHistoryDO::getCreateTime));
        if (dos.isEmpty()) {
            return Collections.emptyList();
        }
        //转为vo
        List<OrderProcessHistoryDetailVO> detailVOS = BeanUtils.toBean(dos, OrderProcessHistoryDetailVO.class);
        //构建创建人
        //提取所有的创建人
        List<String> creatorIds = detailVOS
                .stream().map(OrderProcessHistoryDetailVO::getCreator).distinct().toList();
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
