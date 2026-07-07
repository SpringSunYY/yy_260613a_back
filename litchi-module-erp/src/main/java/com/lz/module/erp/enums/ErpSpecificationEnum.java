package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpSpecificationEnum implements ArrayValuable<String> {

    SPECIFICATION_0("0", "其他"),
    SPECIFICATION_1("1", "套装"),
    SPECIFICATION_2("2", "单上衣"),
    SPECIFICATION_3("3", "单裤"),
    SPECIFICATION_4("4", "双面套装");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpSpecificationEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
