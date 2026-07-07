package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpCategoryEnum implements ArrayValuable<String> {

    CATEGORY_0("0", "其他"),
    CATEGORY_1("1", "篮球服"),
    CATEGORY_2("2", "足球服"),
    CATEGORY_3("3", "乒羽排"),
    CATEGORY_4("4", "POLO衫");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpCategoryEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
