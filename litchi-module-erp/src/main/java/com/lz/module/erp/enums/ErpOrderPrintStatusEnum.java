package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpOrderPrintStatusEnum implements ArrayValuable<String> {

    ORDER_PRINT_STATUS_0("0", "未打印"),
    ORDER_PRINT_STATUS_1("1", "已打印");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpOrderPrintStatusEnum::getStatus).toArray(String[]::new);

    /**
     * 状态值
     */
    private final String status;

    /**
     * 状态名
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
