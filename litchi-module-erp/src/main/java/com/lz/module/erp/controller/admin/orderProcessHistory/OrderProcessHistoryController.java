package com.lz.module.erp.controller.admin.orderProcessHistory;

import com.lz.framework.apilog.core.annotation.ApiAccessLog;
import com.lz.framework.common.pojo.CommonResult;
import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.util.object.BeanUtils;
import com.lz.framework.excel.core.util.ExcelUtils;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistoryExcelVO;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistoryPageReqVO;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistoryRespVO;
import com.lz.module.erp.controller.admin.orderProcessHistory.vo.OrderProcessHistorySaveReqVO;
import com.lz.module.erp.dal.dataobject.orderProcessHistory.OrderProcessHistoryDO;
import com.lz.module.erp.service.orderProcessHistory.OrderProcessHistoryService;
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
 * 订单工序记录 Controller
 *
 * @author 荔枝软件
 */
@Tag(name = "管理后台 - 订单工序记录")
@RestController
@RequestMapping("/erp/order-process-history")
@Validated
public class OrderProcessHistoryController {

    @Resource
    private OrderProcessHistoryService orderProcessHistoryService;

    /**
     * 创建订单工序记录
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单工序记录")
    @PreAuthorize("@ss.hasPermission('erp:order-process-history:create')")
    public CommonResult<Long> createOrderProcessHistory(@Valid @RequestBody OrderProcessHistorySaveReqVO createReqVO) {
        return success(orderProcessHistoryService.createOrderProcessHistory(createReqVO));
    }

    /**
     * 更新订单工序记录
     */
    @PutMapping("/update")
    @Operation(summary = "更新订单工序记录")
    @PreAuthorize("@ss.hasPermission('erp:order-process-history:update')")
    public CommonResult<Boolean> updateOrderProcessHistory(@Valid @RequestBody OrderProcessHistorySaveReqVO updateReqVO) {
        orderProcessHistoryService.updateOrderProcessHistory(updateReqVO);
        return success(true);
    }

    /**
     * 删除订单工序记录
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除订单工序记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:order-process-history:delete')")
    public CommonResult<Boolean> deleteOrderProcessHistory(@RequestParam("id") Long id) {
        orderProcessHistoryService.deleteOrderProcessHistory(id);
        return success(true);
    }

    /**
     * 批量删除订单工序记录
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除订单工序记录")
                @PreAuthorize("@ss.hasPermission('erp:order-process-history:delete')")
    public CommonResult<Boolean> deleteOrderProcessHistoryList(@RequestParam("ids") List<Long> ids) {
        orderProcessHistoryService.deleteOrderProcessHistoryListByIds(ids);
        return success(true);
    }

    /**
     * 获取订单工序记录详情
     */
    @GetMapping("/get")
    @Operation(summary = "获得订单工序记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:order-process-history:query')")
    public CommonResult<OrderProcessHistoryRespVO> getOrderProcessHistory(@RequestParam("id") Long id) {
        OrderProcessHistoryDO orderProcessHistory = orderProcessHistoryService.getOrderProcessHistory(id);
        return success(BeanUtils.toBean(orderProcessHistory, OrderProcessHistoryRespVO.class));
    }

    /**
     * 获取订单工序记录分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得订单工序记录分页")
    @PreAuthorize("@ss.hasPermission('erp:order-process-history:query')")
    public CommonResult<PageResult<OrderProcessHistoryRespVO>> getOrderProcessHistoryPage(@Valid OrderProcessHistoryPageReqVO pageReqVO) {
        PageResult<OrderProcessHistoryDO> pageResult = orderProcessHistoryService.getOrderProcessHistoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderProcessHistoryRespVO.class));
    }

    /**
     * 导出订单工序记录 Excel
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出订单工序记录 Excel")
    @PreAuthorize("@ss.hasPermission('erp:order-process-history:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderProcessHistoryExcel(@Valid OrderProcessHistoryPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderProcessHistoryDO> list = orderProcessHistoryService.getOrderProcessHistoryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单工序记录.xls", "数据", OrderProcessHistoryExcelVO.class,
                        BeanUtils.toBean(list, OrderProcessHistoryExcelVO.class));
    }


}
