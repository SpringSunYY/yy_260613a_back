package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpOrderResourceEnum implements ArrayValuable<String> {

    ORDER_RESOURCE_0("0", "其他"),
    ORDER_RESOURCE_1("1", "淘宝"),
    ORDER_RESOURCE_2("2", "京东"),
    ORDER_RESOURCE_3("3", "拼多多");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpOrderResourceEnum::getStatus).toArray(String[]::new);

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
