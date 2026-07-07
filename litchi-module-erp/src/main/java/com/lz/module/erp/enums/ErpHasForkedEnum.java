package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpHasForkedEnum implements ArrayValuable<String> {

    HAS_FORKED_0("0", "其他"),
    HAS_FORKED_1("1", "不开叉"),
    HAS_FORKED_2("2", "衫开叉"),
    HAS_FORKED_3("3", "裤开叉"),
    HAS_FORKED_4("4", "衫裤开叉");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpHasForkedEnum::getStatus).toArray(String[]::new);

    private final String status;

    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
