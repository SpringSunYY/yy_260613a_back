package com.lz.module.erp.controller.admin.order;

import com.lz.framework.apilog.core.annotation.ApiAccessLog;
import com.lz.framework.common.pojo.CommonResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.excel.core.util.ExcelUtils;
import com.lz.module.erp.controller.admin.order.vo.*;
import com.lz.module.erp.dal.dataobject.order.OrderDO;
import com.lz.module.erp.dal.dataobject.order.OrderDetailDO;
import com.lz.module.erp.service.order.OrderService;
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
 * 订单信息 Controller
 *
 * @author 荔枝软件
 */
@Tag(name = "管理后台 - 订单信息")
@RestController
@RequestMapping("/erp/order")
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单信息")
    @PreAuthorize("@ss.hasPermission('erp:order:create')")
    public CommonResult<Long> createOrder(@Valid @RequestBody OrderSaveReqVO createReqVO) {
        return success(orderService.createOrder(createReqVO));
    }

    /**
     * 更新订单信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新订单信息")
    @PreAuthorize("@ss.hasPermission('erp:order:update')")
    public CommonResult<Boolean> updateOrder(@Valid @RequestBody OrderSaveReqVO updateReqVO) {
        orderService.updateOrder(updateReqVO);
        return success(true);
    }

    /**
     * 发货
     */
    @PutMapping("/ship")
    @Operation(summary = "发货订单")
    @PreAuthorize("@ss.hasAnyPermissions('erp:order:update','erp:order-process:ship')")
    public CommonResult<Boolean> shipOrder(@Valid @RequestBody OrderShipReqVO shipReqVO) {
        orderService.shipOrder(shipReqVO);
        return success(true);
    }

    /**
     * 提交审核订单信息
     */
    @PostMapping("/submit-audit-order")
    @Operation(summary = "提交审核订单信息")
    @PreAuthorize("@ss.hasPermission('erp:order:create')")
    public CommonResult<Boolean> submitAuditOrder(@Valid @RequestBody OrderAuditReqVO auditReqVO) {
        orderService.submitAuditOrder(auditReqVO);
        return success(true);
    }

    /**
     * 删除订单信息
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除订单信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:order:delete')")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        orderService.deleteOrder(id);
        return success(true);
    }

    /**
     * 批量删除订单信息
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除订单信息")
    @PreAuthorize("@ss.hasPermission('erp:order:delete')")
    public CommonResult<Boolean> deleteOrderList(@RequestParam("ids") List<Long> ids) {
        orderService.deleteOrderListByIds(ids);
        return success(true);
    }

    /**
     * 获取订单信息详情
     */
    @GetMapping("/get")
    @Operation(summary = "获得订单信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:order:query')")
    public CommonResult<OrderRespVO> getOrder(@RequestParam("id") Long id) {
        OrderDO order = orderService.getOrder(id);
        return success(BeanUtils.toBean(order, OrderRespVO.class));
    }

    /**
     * 获取订单信息
     */
    @GetMapping("/get/no")
    @Operation(summary = "获得订单信息-no")
    @Parameter(name = "orderNo", description = "订单编号", required = true, example = "orderNo")
    @PreAuthorize("@ss.hasPermission('erp:order:query')")
    public CommonResult<OrderRespVO> getOrderByNo(@RequestParam("orderNo") String orderNo) {
        OrderDO order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return success(null);
        }
        return success(BeanUtils.toBean(order, OrderRespVO.class));
    }

    /**
     * 获取订单信息分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得订单信息分页")
    @PreAuthorize("@ss.hasPermission('erp:order:query')")
    public CommonResult<PageResult<OrderRespVO>> getOrderPage(@Valid OrderPageReqVO pageReqVO) {
        PageResult<OrderDO> pageResult = orderService.getOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderRespVO.class));
    }

    /**
     * 获取待发货订单信息分页
     */
    @GetMapping("/ship/page")
    @Operation(summary = "获得订单信息分页")
    @PreAuthorize("@ss.hasPermission('erp:order:query')")
    public CommonResult<PageResult<OrderRespVO>> getShipOrderPage(@Valid OrderPageReqVO pageReqVO) {
        PageResult<OrderDO> pageResult = orderService.getShipOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderRespVO.class));
    }

    /**
     * 导出订单信息 Excel
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出订单信息 Excel")
    @PreAuthorize("@ss.hasPermission('erp:order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderExcel(@Valid OrderPageReqVO pageReqVO,
                                 HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderDO> list = orderService.getOrderPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单信息.xls", "数据", OrderExcelVO.class,
                BeanUtils.toBean(list, OrderExcelVO.class));
    }


    // ==================== 子表（订单明细） ====================

    /**
     * 获取订单明细列表
     */
    @GetMapping("/order-detail/list-by-order-no")
    @Operation(summary = "获得订单明细列表")
    @Parameter(name = "orderNo", description = "订单号")
    @PreAuthorize("@ss.hasPermission('erp:order:query')")
    public CommonResult<List<OrderDetailDO>> getOrderDetailListByOrderNo(@RequestParam("orderNo") String orderNo) {
        return success(orderService.getOrderDetailListByOrderNo(orderNo));
    }

}
