package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpSetSizeEnum implements ArrayValuable<String> {

    SET_SIZE_XXS("XXS", "超小码"),
    SET_SIZE_XS("XS", "加小码"),
    SET_SIZE_S("S", "小码"),
    SET_SIZE_M("M", "中码"),
    SET_SIZE_L("L", "大码"),
    SET_SIZE_XL("XL", "加大码"),
    SET_SIZE_XXL("XXL", "超大码"),
    SET_SIZE_3XL("3XL", "3XL"),
    SET_SIZE_4XL("4XL", "4XL"),
    SET_SIZE_5XL("5XL", "5XL"),
    SET_SIZE_6XL("6XL", "6XL");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpSetSizeEnum::getStatus).toArray(String[]::new);

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
