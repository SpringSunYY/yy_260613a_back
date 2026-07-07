package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpOrderStatusEnum implements ArrayValuable<String> {

    ORDER_STATUS_1("1", "24小时加急"),
    ORDER_STATUS_2("2", "48小时加急"),
    ORDER_STATUS_3("3", "正常"),
    ORDER_STATUS_4("4", "售后");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpOrderStatusEnum::getStatus).toArray(String[]::new);

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
