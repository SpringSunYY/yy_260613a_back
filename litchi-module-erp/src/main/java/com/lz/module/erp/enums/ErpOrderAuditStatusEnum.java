package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpOrderAuditStatusEnum implements ArrayValuable<String> {

    ORDER_AUDIT_STATUS_1("1", "草稿"),
    ORDER_AUDIT_STATUS_2("2", "待审核"),
    ORDER_AUDIT_STATUS_3("3", "同意"),
    ORDER_AUDIT_STATUS_4("4", "拒绝");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpOrderAuditStatusEnum::getStatus).toArray(String[]::new);

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
