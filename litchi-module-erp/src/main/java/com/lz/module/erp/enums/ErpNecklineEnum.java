package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpNecklineEnum implements ArrayValuable<String> {

    NECKLINE_0("0", "其他"),
    NECKLINE_1("1", "桃子领"),
    NECKLINE_2("2", "圆领"),
    NECKLINE_3("3", "大小领"),
    NECKLINE_4("4", "起两角搭领"),
    NECKLINE_5("5", "圆领大小领"),
    NECKLINE_6("6", "V领搭领"),
    NECKLINE_7("7", "POLO领"),
    NECKLINE_8("8", "翻领");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpNecklineEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
