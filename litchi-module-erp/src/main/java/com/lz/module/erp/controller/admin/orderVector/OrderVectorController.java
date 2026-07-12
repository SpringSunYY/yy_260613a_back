package com.lz.module.erp.controller.admin.orderVector;

import com.lz.framework.apilog.core.annotation.ApiAccessLog;
import com.lz.framework.common.pojo.CommonResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.excel.core.util.ExcelUtils;
import com.lz.framework.vector.pojo.SearchResult;
import com.lz.module.erp.controller.admin.orderVector.vo.*;
import com.lz.module.erp.dal.dataobject.orderVector.OrderVectorDO;
import com.lz.module.erp.service.orderVector.OrderVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.lz.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.lz.framework.common.pojo.CommonResult.success;

/**
 * 订单向量 Controller
 *
 * @author 荔枝软件
 */
@Tag(name = "管理后台 - 订单向量")
@RestController
@RequestMapping("/erp/order-vector")
@Validated
public class OrderVectorController {

    @Resource
    private OrderVectorService orderVectorService;

    /**
     * 创建订单向量
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单向量")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:create')")
    public CommonResult<Long> createOrderVector(@Valid @RequestBody OrderVectorSaveReqVO createReqVO) {
        return success(orderVectorService.createOrderVector(createReqVO));
    }

    /**
     * 重新构建向量
     */
    @PostMapping("/reset")
    @Operation(summary = "重构向量")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:create')")
    public CommonResult<Boolean> resetOrderVectorByOrderNo(@Valid @RequestBody OrderVectorRestReqVO reqVO) {
        orderVectorService.resetOrderVectorByOrderNo(reqVO.getOrderNo());
        return success(true);
    }

    /**
     * 更新订单向量
     */
    @PutMapping("/update")
    @Operation(summary = "更新订单向量")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:update')")
    public CommonResult<Boolean> updateOrderVector(@Valid @RequestBody OrderVectorSaveReqVO updateReqVO) {
        orderVectorService.updateOrderVector(updateReqVO);
        return success(true);
    }

    /**
     * 删除订单向量
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除订单向量")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:order-vector:delete')")
    public CommonResult<Boolean> deleteOrderVector(@RequestParam("id") Long id) {
        orderVectorService.deleteOrderVector(id);
        return success(true);
    }

    /**
     * 批量删除订单向量
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除订单向量")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:delete')")
    public CommonResult<Boolean> deleteOrderVectorList(@RequestParam("ids") List<Long> ids) {
        orderVectorService.deleteOrderVectorListByIds(ids);
        return success(true);
    }

    /**
     * 获取订单向量详情
     */
    @GetMapping("/get")
    @Operation(summary = "获得订单向量")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:query')")
    public CommonResult<OrderVectorRespVO> getOrderVector(@RequestParam("id") Long id) {
        OrderVectorDO orderVector = orderVectorService.getOrderVector(id);
        return success(BeanUtils.toBean(orderVector, OrderVectorRespVO.class));
    }

    /**
     * 获取订单向量分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得订单向量分页")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:query')")
    public CommonResult<PageResult<OrderVectorRespVO>> getOrderVectorPage(@Valid OrderVectorPageReqVO pageReqVO) {
        PageResult<OrderVectorDO> pageResult = orderVectorService.getOrderVectorPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderVectorRespVO.class));
    }

    /**
     * 导出订单向量 Excel
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出订单向量 Excel")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderVectorExcel(@Valid OrderVectorPageReqVO pageReqVO,
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderVectorDO> list = orderVectorService.getOrderVectorPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单向量.xls", "数据", OrderVectorExcelVO.class,
                BeanUtils.toBean(list, OrderVectorExcelVO.class));
    }

    /**
     * 以图搜图（按库内向量 id）
     */
    @GetMapping("/search")
    @Operation(summary = "订单向量以图搜图（按库内向量编号）")
    @Parameter(name = "id", description = "库内向量编号（erp_order_vector.vectorId）", required = true)
    @PreAuthorize("@ss.hasPermission('erp:order-vector:query')")
    public CommonResult<List<SearchResult>> searchOrderVectorById(@RequestParam("id") String id,
                                                                  @Valid OrderVectorSearchReqVO reqVO) throws Exception {
        return success(orderVectorService.searchById(id, reqVO.getTopK()));
    }

    /**
     * 以图搜图（按上传的图片）
     */
    @PostMapping("/search/upload")
    @Operation(summary = "订单向量以图搜图（按上传图片）")
    @PreAuthorize("@ss.hasPermission('erp:order-vector:query')")
    public CommonResult<List<SearchResult>> searchOrderVectorByUpload(@Valid OrderVectorSearchReqVO reqVO)
            throws Exception {
        return success(orderVectorService.searchByUpload(
                reqVO.getFile().getInputStream(), reqVO.getTopK()));
    }

}
