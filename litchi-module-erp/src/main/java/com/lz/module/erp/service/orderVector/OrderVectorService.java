package com.lz.module.erp.service.orderVector;

import java.util.*;

import com.lz.framework.vector.pojo.SearchResult;
import com.lz.module.erp.controller.admin.order.vo.OrderShipReqVO;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import jakarta.validation.*;
import com.lz.module.erp.controller.admin.orderVector.vo.*;
import com.lz.module.erp.dal.dataobject.orderVector.OrderVectorDO;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.PageParam;

import java.io.InputStream;

/**
 * 订单向量 Service 接口
 *
 * @author 荔枝软件
 */
public interface OrderVectorService {

    /**
     * 创建订单向量
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderVector(@Valid OrderVectorSaveReqVO createReqVO);

    /**
     * 更新订单向量
     *
     * @param updateReqVO 更新信息
     */
    void updateOrderVector(@Valid OrderVectorSaveReqVO updateReqVO);

    /**
     * 删除订单向量
     *
     * @param id 编号
     */
    void deleteOrderVector(Long id);

    /**
    * 批量删除订单向量
    *
    * @param ids 编号
    */
    void deleteOrderVectorListByIds(List<Long> ids);

    /**
     * 获得订单向量
     *
     * @param id 编号
     * @return 订单向量
     */
    OrderVectorDO getOrderVector(Long id);

    /**
     * 获得订单向量分页
     *
     * @param pageReqVO 分页查询
     * @return 订单向量分页
     */
    PageResult<OrderVectorDO> getOrderVectorPage(OrderVectorPageReqVO pageReqVO);


    /**
     * 索引订单向量
     * @Param orderNo 订单号
     * @Param imageUrls 订单图片地址
     */
    void indexOrderVector(String orderNo,String imageUrls);

    /**
     * 以图搜图（按库内向量 id）
     *
     * @param id   库内向量编号（erp_order_vector.vectorId）
     * @param topK Top K
     * @return 相似结果
     */
    List<SearchResult> searchById(String id, Integer topK) throws Exception;

    /**
     * 以图搜图（按上传的图片字节流）
     *
     * @param inputStream 查询图输入流（方法内部会关闭）
     * @param topK        Top K
     * @return 相似结果
     */
    List<SearchResult> searchByUpload(InputStream inputStream, Integer topK) throws Exception;
}
