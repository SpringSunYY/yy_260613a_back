package com.lz.module.erp.enums;

import com.lz.framework.common.exception.ErrorCode;

/**
 * erp 错误码枚举
 *
 * @Project: LZ-Order
 * @Author: YY
 * @CreateTime: 2026-07-07  18:40
 * @Version: 1.0
 */
public interface ErrorCodeConstants {
    // ========== 订单信息 ==========
    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_003_000_001, "erp.order.back.notExists", "订单信息不存在");
    ErrorCode ORDER_EXISTS = new ErrorCode(1_003_000_002, "erp.order.back.exists", "订单信息已存在");
    ErrorCode ORDER_AUDIT_STATUS_ERROR = new ErrorCode(1_003_000_002, "erp.order.back.auditStatusError", "订单审核状态错误");
    // ========== 导入通用错误码（可自行配置国际化） ==========
    ErrorCode ORDER_IMPORT_DATA_EMPTY = new ErrorCode(1_003_000_100, "erp.back.import.dataEmpty", "订单信息导入数据不能为空！");

    // ========== 订单审核记录 ==========
    ErrorCode ORDER_AUDIT_NOT_EXISTS = new ErrorCode(1_003_000_001, "erp.orderAudit.back.notExists", "订单审核记录不存在");
    // ========== 导入通用错误码（可自行配置国际化） ==========
    ErrorCode ORDER_AUDIT_IMPORT_DATA_EMPTY = new ErrorCode(1_003_000_100, "erp.back.import.dataEmpty", "订单审核记录导入数据不能为空！");

    // ========== 订单明细 ==========
    ErrorCode ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_003_000_001, "erp.orderDetail.back.notExists", "订单明细不存在");
    // ========== 导入通用错误码（可自行配置国际化） ==========
    ErrorCode ORDER_DETAIL_IMPORT_DATA_EMPTY = new ErrorCode(1_003_000_100, "erp.back.import.dataEmpty", "订单明细导入数据不能为空！");

    // ========== 订单工序 ==========
    ErrorCode ORDER_PROCESS_NOT_EXISTS = new ErrorCode(1_003_000_001, "erp.orderProcess.back.notExists", "订单工序不存在");
    // ========== 导入通用错误码（可自行配置国际化） ==========
    ErrorCode ORDER_PROCESS_IMPORT_DATA_EMPTY = new ErrorCode(1_003_000_100, "erp.back.import.dataEmpty", "订单工序导入数据不能为空！");

    // ========== 订单工序记录 ==========
    ErrorCode ORDER_PROCESS_HISTORY_NOT_EXISTS = new ErrorCode(1_003_000_001, "erp.orderProcessHistory.back.notExists", "订单工序记录不存在");
    // ========== 导入通用错误码（可自行配置国际化） ==========
    ErrorCode ORDER_PROCESS_HISTORY_IMPORT_DATA_EMPTY = new ErrorCode(1_003_000_100, "erp.back.import.dataEmpty", "订单工序记录导入数据不能为空！");

    // ========== 订单向量 ==========
    ErrorCode ORDER_VECTOR_NOT_EXISTS = new ErrorCode(1_003_000_001, "erp.orderVector.back.notExists", "订单向量不存在");
    // ========== 导入通用错误码（可自行配置国际化） ==========
    ErrorCode ORDER_VECTOR_IMPORT_DATA_EMPTY = new ErrorCode(1_003_000_100, "erp.back.import.dataEmpty", "订单向量导入数据不能为空！");
}
