package com.lz.module.erp.controller.admin.orderAudit;

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

import com.lz.module.erp.controller.admin.orderAudit.vo.*;
import com.lz.module.erp.controller.admin.orderAudit.vo.OrderAuditExcelVO;
import com.lz.module.erp.dal.dataobject.orderAudit.OrderAuditDO;
import com.lz.module.erp.service.orderAudit.OrderAuditService;
import static com.lz.framework.excel.core.annotations.ExcelDirection.ONLY_IMPORT;

/**
 * 订单审核记录 Controller
 *
 * @author 荔枝软件
 */
@Tag(name = "管理后台 - 订单审核记录")
@RestController
@RequestMapping("/erp/order-audit")
@Validated
public class OrderAuditController {

    @Resource
    private OrderAuditService orderAuditService;

    /**
     * 创建订单审核记录
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单审核记录")
    @PreAuthorize("@ss.hasPermission('erp:order-audit:create')")
    public CommonResult<Long> createOrderAudit(@Valid @RequestBody OrderAuditSaveReqVO createReqVO) {
        return success(orderAuditService.createOrderAudit(createReqVO));
    }

    /**
     * 更新订单审核记录
     */
    @PutMapping("/update")
    @Operation(summary = "更新订单审核记录")
    @PreAuthorize("@ss.hasPermission('erp:order-audit:update')")
    public CommonResult<Boolean> updateOrderAudit(@Valid @RequestBody OrderAuditSaveReqVO updateReqVO) {
        orderAuditService.updateOrderAudit(updateReqVO);
        return success(true);
    }

    /**
     * 删除订单审核记录
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除订单审核记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:order-audit:delete')")
    public CommonResult<Boolean> deleteOrderAudit(@RequestParam("id") Long id) {
        orderAuditService.deleteOrderAudit(id);
        return success(true);
    }

    /**
     * 批量删除订单审核记录
     */
    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除订单审核记录")
                @PreAuthorize("@ss.hasPermission('erp:order-audit:delete')")
    public CommonResult<Boolean> deleteOrderAuditList(@RequestParam("ids") List<Long> ids) {
        orderAuditService.deleteOrderAuditListByIds(ids);
        return success(true);
    }

    /**
     * 获取订单审核记录详情
     */
    @GetMapping("/get")
    @Operation(summary = "获得订单审核记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:order-audit:query')")
    public CommonResult<OrderAuditRespVO> getOrderAudit(@RequestParam("id") Long id) {
        OrderAuditDO orderAudit = orderAuditService.getOrderAudit(id);
        return success(BeanUtils.toBean(orderAudit, OrderAuditRespVO.class));
    }

    /**
     * 获取订单审核记录分页
     */
    @GetMapping("/page")
    @Operation(summary = "获得订单审核记录分页")
    @PreAuthorize("@ss.hasPermission('erp:order-audit:query')")
    public CommonResult<PageResult<OrderAuditRespVO>> getOrderAuditPage(@Valid OrderAuditPageReqVO pageReqVO) {
        PageResult<OrderAuditDO> pageResult = orderAuditService.getOrderAuditPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderAuditRespVO.class));
    }

    /**
     * 导出订单审核记录 Excel
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出订单审核记录 Excel")
    @PreAuthorize("@ss.hasPermission('erp:order-audit:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderAuditExcel(@Valid OrderAuditPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderAuditDO> list = orderAuditService.getOrderAuditPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单审核记录.xls", "数据", OrderAuditExcelVO.class,
                        BeanUtils.toBean(list, OrderAuditExcelVO.class));
    }


}
