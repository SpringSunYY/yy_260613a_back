package com.lz.module.erp.controller.admin.orderProcess;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lz.framework.common.pojo.PageParam;
import com.lz.framework.common.pojo.PageResult;
import com.lz.framework.common.pojo.CommonResult;
import com.lz.framework.common.util.object.BeanUtils;
import static com.lz.framework.common.pojo.CommonResult.success;

import com.lz.framework.excel.core.util.ExcelUtils;
import cn.hutool.core.util.StrUtil;

import com.lz.framework.apilog.core.annotation.ApiAccessLog;
import static com.lz.framework.apilog.core.enums.OperateTypeEnum.*;

import com.lz.module.erp.controller.admin.orderProcess.vo.*;
import com.lz.module.erp.controller.admin.orderProcess.vo.OrderProcessExcelVO;
import com.lz.module.erp.dal.dataobject.orderProcess.OrderProcessDO;
import com.lz.module.erp.service.orderProcess.OrderProcessService;
import static com.lz.framework.excel.core.annotations.ExcelDirection.ONLY_IMPORT;

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
