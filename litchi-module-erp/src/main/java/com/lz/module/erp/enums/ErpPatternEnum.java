package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpPatternEnum implements ArrayValuable<String> {

    PATTERN_0("0", "其他"),
    PATTERN_1("1", "常规版型"),
    PATTERN_2("2", "美式版型"),
    PATTERN_3("3", "足球服（欧码）"),
    PATTERN_4("4", "足球服（亚码）"),
    PATTERN_5("5", "乒羽排"),
    PATTERN_6("6", "GY唛架");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpPatternEnum::getStatus).toArray(String[]::new);

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
