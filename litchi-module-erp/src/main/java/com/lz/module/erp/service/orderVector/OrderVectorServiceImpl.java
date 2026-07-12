package com.lz.module.erp.service.orderVector;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.vector.constants.CollectionConstants;
import com.lz.framework.vector.core.vector.ImageIndexService;
import com.lz.framework.vector.pojo.QueryResult;
import com.lz.framework.vector.pojo.SearchResult;
import com.lz.framework.vector.pojo.VectorRecord;
import com.lz.module.erp.controller.admin.orderVector.vo.OrderVectorPageReqVO;
import com.lz.module.erp.controller.admin.orderVector.vo.OrderVectorSaveReqVO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.dal.dataobject.orderVector.OrderVectorDO;
import com.lz.module.erp.dal.mysql.orderVector.OrderVectorMapper;
import com.lz.module.erp.service.orderProcess.OrderProcessService;
import com.lz.module.infra.api.file.FileApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.module.erp.enums.ErrorCodeConstants.*;

/**
 * 订单向量 Service 实现类
 *
 * @author 荔枝软件
 */
@Service
@Validated
@Slf4j
public class OrderVectorServiceImpl implements OrderVectorService {

    @Resource
    private OrderVectorMapper orderVectorMapper;

    @Resource
    private ImageIndexService imageIndexService;

    @Resource
    private FileApi fileApi;

    @Resource
    private OrderProcessService orderProcessService;

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
        OrderVectorDO orderVectorDO = validateOrderVectorExists(id);
        // 删除
        orderVectorMapper.deleteById(id);
        imageIndexService.deleteByIds(List.of(orderVectorDO.getVectorId()),
                CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
    }

    @Override
    public void deleteOrderVectorListByIds(List<Long> ids) {
        ids.forEach(this::deleteOrderVector);
    }


    private OrderVectorDO validateOrderVectorExists(Long id) {
        OrderVectorDO orderVectorDO = orderVectorMapper.selectById(id);
        if (orderVectorDO == null) {
            throw exception(ORDER_VECTOR_NOT_EXISTS);
        }
        return orderVectorDO;
    }

    @Override
    public OrderVectorDO getOrderVector(Long id) {
        return orderVectorMapper.selectById(id);
    }

    @Override
    public PageResult<OrderVectorDO> getOrderVectorPage(OrderVectorPageReqVO pageReqVO) {
        return orderVectorMapper.selectPage(pageReqVO);
    }

    @Override
    public void indexOrderVector(String orderNo, String imageUrls) {
        if (StrUtil.isEmpty(imageUrls)) {
            return;
        }
        //首先查询是否已经为订单的某张
        // 图片构建向量
        List<OrderVectorDO> orderVectorDOS = orderVectorMapper.selectList(new LambdaQueryWrapper<OrderVectorDO>()
                .eq(OrderVectorDO::getOrderNo, orderNo));
        //转换为地址列表
        List<String> imageUrlDos = orderVectorDOS.stream().map(OrderVectorDO::getImageUrl).toList();
        //使用分隔符||分割文件
        String[] orderImages = imageUrls.split("\\|\\|");
        //过滤出尚未构建向量的图片地址
        List<String> newImages = Arrays.stream(orderImages)
                .filter(img -> !imageUrlDos.contains(img))
                .toList();
        if (CollUtil.isEmpty(newImages)) {
            return;
        }
        ArrayList<OrderVectorDO> vectorDOS = getVectorNewDos(orderNo, orderImages);
        if (vectorDOS.isEmpty()) return;
        orderVectorMapper.insertBatch(vectorDOS);
    }

    @Override
    public void resetOrderVectorByOrderNo(String orderNo) {
        if (StrUtil.isEmpty(orderNo)) {
            throw exception(ORDER_PROCESS_NOT_EXISTS);
        }
        //先查询工序
        OrderProcessDO process = orderProcessService.getOrderProcessByOrderNo(orderNo);
        if (process == null) {
            throw exception(ORDER_PROCESS_NOT_EXISTS);
        }
        String orderImage = process.getOrderImage();
        if (StrUtil.isEmpty(orderImage)) {
            throw exception(ORDER_PROCESS_NOT_IMAGE);
        }
        //根据订单no查询到当前已经拥有的向量信息,以便后面删除
        List<OrderVectorDO> vectorOldDOS = orderVectorMapper.selectList(new LambdaQueryWrapper<OrderVectorDO>().eq(OrderVectorDO::getOrderNo, orderNo));
        List<QueryResult> queryOldResults = imageIndexService.queryByOriginKey(orderNo, CollectionConstants.ERP_ORDER_IMAGE_VECTOR);

        //使用分隔符||分割文件
        String[] orderImages = orderImage.split("\\|\\|");
        ArrayList<OrderVectorDO> vectorNewDOS = getVectorNewDos(orderNo, orderImages);
        //先删除数据
        if (!vectorOldDOS.isEmpty()) {
            orderVectorMapper.deleteByIds(vectorOldDOS.stream().map(OrderVectorDO::getId).collect(Collectors.toSet()));
        }
        if (!queryOldResults.isEmpty()) {
            imageIndexService.deleteByIds(queryOldResults.stream().map(QueryResult::getId).collect(Collectors.toList()), CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
        }
        //插入新写的数据
        if (!vectorNewDOS.isEmpty()) {
            orderVectorMapper.insertBatch(vectorNewDOS);
        }
    }

    /**
     * 获取dos
     * @param orderNo 订单编号
     * @param orderImages 文件地址
     * @return 向量do
     */
    private @NonNull ArrayList<OrderVectorDO> getVectorNewDos(String orderNo, String[] orderImages) {
        //构建向量
        ArrayList<OrderVectorDO> vectorNewDOS = new ArrayList<>();
        //为新图片构建向量并保存
        for (String imageUrl : orderImages) {
            OrderVectorDO orderVector = new OrderVectorDO();
            orderVector.setOrderNo(orderNo);
            orderVector.setImageUrl(imageUrl);
            byte[] fileContent = fileApi.getFileContent(imageUrl);
            try {
                VectorRecord vectorRecord = imageIndexService.index(imageUrl, fileContent,
                        orderNo, CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
                orderVector.setFeatureVector(Arrays.toString(vectorRecord.getVector()));
                orderVector.setVectorId(vectorRecord.getId());
                vectorNewDOS.add(orderVector);
            } catch (Exception e) {
                log.error("erp-订单工序构建向量失败，订单号：{},异常：{}", orderNo, e.getMessage());
            }
        }
        return vectorNewDOS;
    }

    @Override
    public List<SearchResult> searchById(String id, Integer topK) throws Exception {
        if (StrUtil.isEmpty(id)) {
            return List.of();
        }
        return imageIndexService.searchById(id, topK == null ? 10 : topK,
                CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
    }

    @Override
    public List<SearchResult> searchByUpload(InputStream inputStream, Integer topK) throws Exception {
        if (inputStream == null) {
            return List.of();
        }
        return imageIndexService.searchByStream(inputStream, topK == null ? 10 : topK,
                CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
    }
}
