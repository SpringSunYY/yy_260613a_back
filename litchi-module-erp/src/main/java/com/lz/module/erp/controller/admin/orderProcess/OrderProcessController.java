package com.lz.module.erp.controller.admin.orderProcess;

import com.lz.framework.apilog.core.annotation.ApiAccessLog;
import com.lz.framework.common.pojo.CommonResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.excel.core.util.ExcelUtils;
import com.lz.module.erp.controller.admin.orderProcess.vo.*;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.service.orderProcess.OrderProcessService;
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
 * 订单工序 Controller
 *
 * @author 荔枝软件
 */
@Tag(name = "管理后台 - 订单工序")
@RestController
@RequestMapping("/erp/order-process")
@Validated
public class OrderProcessController {

    @Resource
    private OrderProcessService orderProcessService;

    /**
     * 创建订单工序
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单工序")
    @PreAuthorize("@ss.hasPermission('erp:order-process:create')")
    public CommonResult<Long> createOrderProcess(@Valid @RequestBody OrderProcessSaveReqVO createReqVO) {
        return success(orderProcessService.createOrderProcess(createReqVO));
    }

    /**
     * 更新订单工序
     */
    @PutMapping("/update")
    @Operation(summary = "更新订单工序")
    @PreAuthorize("@ss.hasPermission('erp:order-process:update')")
    public CommonResult<Boolean> updateOrderProcess(@Valid @RequestBody OrderProcessSaveReqVO updateReqVO) {
        orderProcessService.updateOrderProcess(updateReqVO);
        return success(true);
    }

    @PutMapping("/update/process")
    @Operation(summary = "更新订单工序")
    @PreAuthorize("@ss.hasPermission('erp:order-process:update')")
    public CommonResult<Boolean> updateProcessToTargetProcessByNo(@Valid @RequestBody OrderProcessSortUpdateReqVO reqVO) {
        orderProcessService.updateProcessToTargetProcess(reqVO);
        return success(true);
    }

    @PutMapping("/update/sort")
    @Operation(summary = "更新订单工序")
    @PreAuthorize("@ss.hasPermission('erp:order-process:update')")
    public CommonResult<Boolean> updateOrderProcessBySort(@Valid @RequestBody OrderProcessSortUpdateReqVO reqVO) {
        orderProcessService.updateOrderProcessBySort(reqVO);
        return success(true);
    }

    /**
     * 删除订单工序
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除订单工序")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:order-process:delete')")
    public CommonResult<Boolean> deleteOrderProcess(@RequestParam("id") Long id) {
        orderProcessService.deleteOrderProcess(id);
        return success(true);
    }

    /**
     * 批量删除订单工序
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除订单工序")
                @PreAuthorize("@ss.hasPermission('erp:order-process:delete')")
    public CommonResult<Boolean> deleteOrderProcessList(@RequestParam("ids") List<Long> ids) {
        orderProcessService.deleteOrderProcessListByIds(ids);
        return success(true);
    }

    /**
     * 获取订单工序详情
     */
    @GetMapping("/get")
    @Operation(summary = "获得订单工序")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:order-process:query')")
    public CommonResult<OrderProcessRespVO> getOrderProcess(@RequestParam("id") Long id) {
        OrderProcessDO orderProcess = orderProcessService.getOrderProcess(id);
        return success(BeanUtils.toBean(orderProcess, OrderProcessRespVO.class));
    }

    /**
     * 根据工单号查询订单工序
     */
    @GetMapping("/get-by-order-no")
    @Operation(summary = "根据工单号查询订单工序")
    @Parameter(name = "orderNo", description = "工单号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:order-process:query')")
    public CommonResult<OrderProcessRespVO> getOrderProcessByOrderNo(@RequestParam("orderNo") String orderNo) {
        OrderProcessDO orderProcess = orderProcessService.getOrderProcessByOrderNo(orderNo);
        return success(BeanUtils.toBean(orderProcess, OrderProcessRespVO.class));
    }

    /**
     * 获取订单工序分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得订单工序分页")
    @PreAuthorize("@ss.hasPermission('erp:order-process:query')")
    public CommonResult<PageResult<OrderProcessRespVO>> getOrderProcessPage(@Valid OrderProcessPageReqVO pageReqVO) {
        PageResult<OrderProcessDO> pageResult = orderProcessService.getOrderProcessPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderProcessRespVO.class));
    }

    /**
     * 获取待排序工序
     */
    @GetMapping("/page/sort")
    @Operation(summary = "获取待排序工序")
    @PreAuthorize("@ss.hasPermission('erp:order-process:query')")
    public CommonResult<PageResult<OrderProcessSortRespVO>> getSortProcessPage(@Valid OrderProcessPageReqVO pageReqVO) {
        PageResult<OrderProcessSortRespVO> pageResult = orderProcessService.getSortProcessPage(pageReqVO);
        return success(pageResult);
    }

    /**
     * 导出订单工序 Excel
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出订单工序 Excel")
    @PreAuthorize("@ss.hasPermission('erp:order-process:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderProcessExcel(@Valid OrderProcessPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderProcessDO> list = orderProcessService.getOrderProcessPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单工序.xls", "数据", OrderProcessExcelVO.class,
                        BeanUtils.toBean(list, OrderProcessExcelVO.class));
    }


}
