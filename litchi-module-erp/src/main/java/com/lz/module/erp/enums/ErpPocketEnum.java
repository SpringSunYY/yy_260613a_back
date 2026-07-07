package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpPocketEnum implements ArrayValuable<String> {

    POCKET_0("0", "其他"),
    POCKET_1("1", "不要口袋"),
    POCKET_2("2", "双侧口袋");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpPocketEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
