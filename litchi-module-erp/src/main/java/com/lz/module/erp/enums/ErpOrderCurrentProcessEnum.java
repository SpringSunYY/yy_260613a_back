package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpOrderCurrentProcessEnum implements ArrayValuable<String> {

    ORDER_CURRENT_PROCESS_1("1", "草稿"),
    ORDER_CURRENT_PROCESS_2("2", "待排版"),
    ORDER_CURRENT_PROCESS_3("3", "待打纸"),
    ORDER_CURRENT_PROCESS_4("4", "待滚筒"),
    ORDER_CURRENT_PROCESS_5("5", "待激光"),
    ORDER_CURRENT_PROCESS_6("6", "待裁缝发货"),
    ORDER_CURRENT_PROCESS_7("7", "完结");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpOrderCurrentProcessEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
