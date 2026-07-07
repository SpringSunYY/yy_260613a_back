package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpOrderPickupMethodEnum implements ArrayValuable<String> {

    PICKUP_METHOD_0("0", "其他"),
    PICKUP_METHOD_1("1", "中通"),
    PICKUP_METHOD_2("2", "广州档口"),
    PICKUP_METHOD_3("3", "目乐工厂自提"),
    PICKUP_METHOD_4("4", "顺丰标快寄付"),
    PICKUP_METHOD_5("5", "顺丰标快到付"),
    PICKUP_METHOD_6("6", "顺丰特快寄付"),
    PICKUP_METHOD_7("7", "顺丰特快到付"),
    PICKUP_METHOD_8("8", "顺丰-客户填单");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpOrderPickupMethodEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
