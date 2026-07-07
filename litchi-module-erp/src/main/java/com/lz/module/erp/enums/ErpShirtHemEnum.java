package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpShirtHemEnum implements ArrayValuable<String> {

    SHIRT_HEM_0("0", "其他"),
    SHIRT_HEM_1("1", "平脚"),
    SHIRT_HEM_2("2", "弧形");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpShirtHemEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
