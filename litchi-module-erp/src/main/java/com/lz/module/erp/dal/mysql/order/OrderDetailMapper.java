package com.lz.module.erp.dal.mysql.order;

import com.lz.framework.mybatis.core.mapper.BaseMapperX;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 订单明细 Mapper
 *
 * @author 荔枝软件
 */
@Mapper
public interface OrderDetailMapper extends BaseMapperX<OrderDetailDO> {

    default List<OrderDetailDO> selectListByOrderNo(String orderNo) {
        return selectList(OrderDetailDO::getOrderNo, orderNo);
    }

    default int deleteByOrderNo(String orderNo) {
        return delete(OrderDetailDO::getOrderNo, orderNo);
    }

	default int deleteByOrderNos(List<String> orderNos) {
	    return deleteBatch(OrderDetailDO::getOrderNo, orderNos);
	}

}
