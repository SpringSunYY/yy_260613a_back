package com.lz.module.erp.service.orderVector;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.vector.constants.CollectionConstants;
import com.lz.framework.vector.core.vector.ImageIndexService;
import com.lz.framework.vector.pojo.SearchResult;
import com.lz.framework.vector.pojo.VectorRecord;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessSaveReqVO;
import com.lz.module.erp.controller.admin.orderVector.vo.OrderVectorPageReqVO;
import com.lz.module.erp.controller.admin.orderVector.vo.OrderVectorSaveReqVO;
import com.lz.module.erp.dal.dataobject.orderVector.OrderVectorDO;
import com.lz.module.erp.dal.mysql.orderVector.OrderVectorMapper;
import com.lz.module.infra.api.file.FileApi;
import com.lz.module.infra.service.vector.ImageSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lz.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.lz.module.erp.enums.ErrorCodeConstants.ORDER_VECTOR_NOT_EXISTS;

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
    private ImageSearchService imageSearchService;

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
    public void indexOrderVector(OrderProcessSaveReqVO reqVO) {
        String orderImage = reqVO.getOrderImage();
        if (StrUtil.isEmpty(orderImage)) {
            return;
        }
        //首先查询是否已经为订单的某张
        // 图片构建向量
        List<OrderVectorDO> orderVectorDOS = orderVectorMapper.selectList(new LambdaQueryWrapper<OrderVectorDO>()
                .eq(OrderVectorDO::getOrderNo, reqVO.getOrderNo()));
        //转换为地址列表
        List<String> imageUrlDos = orderVectorDOS.stream().map(OrderVectorDO::getImageUrl).toList();
        //使用分隔符||分割文件
        String[] orderImages = orderImage.split("\\|\\|");
        //过滤出尚未构建向量的图片地址
        List<String> newImages = Arrays.stream(orderImages)
                .filter(img -> !imageUrlDos.contains(img))
                .toList();
        if (CollUtil.isEmpty(newImages)) {
            return;
        }
        ArrayList<OrderVectorDO> vectorDOS = new ArrayList<>();
        //为新图片构建向量并保存
        for (String imageUrl : newImages) {
            OrderVectorDO orderVector = new OrderVectorDO();
            orderVector.setOrderNo(reqVO.getOrderNo());
            orderVector.setImageUrl(imageUrl);
            byte[] fileContent = fileApi.getFileContent(imageUrl);
            try {
                VectorRecord vectorRecord = imageIndexService.index(imageUrl, fileContent,
                        reqVO.getOrderNo(), CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
                orderVector.setFeatureVector(Arrays.toString(vectorRecord.getVector()));
                orderVector.setVectorId(vectorRecord.getId());
                vectorDOS.add(orderVector);
            } catch (Exception e) {
                log.error("erp-订单工序构建向量失败，订单号：{},异常：{}", reqVO.getOrderNo(), e.getMessage());
            }
        }
        if (vectorDOS.isEmpty()) return;
        orderVectorMapper.insertBatch(vectorDOS);
    }

    @Override
    public List<SearchResult> searchById(String id, Integer topK) throws Exception {
        if (StrUtil.isEmpty(id)) {
            return List.of();
        }
        return imageSearchService.searchById(id, topK == null ? 10 : topK,
                CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
    }

    @Override
    public List<SearchResult> searchByUpload(InputStream inputStream, Integer topK) throws Exception {
        if (inputStream == null) {
            return List.of();
        }
        return imageSearchService.searchByStream(inputStream, topK == null ? 10 : topK,
                CollectionConstants.ERP_ORDER_IMAGE_VECTOR);
    }
}
