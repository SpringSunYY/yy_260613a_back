package com.lz.module.erp.enums;

import com.lz.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpFabricEnum implements ArrayValuable<String> {

    FABRIC_0("0", "其他"),
    FABRIC_1("1", "165gNBA双眼布"),
    FABRIC_2("2", "200gNBA双眼布"),
    FABRIC_3("3", "230gNBA双眼布"),
    FABRIC_4("4", "篮球提花布"),
    FABRIC_5("5", "低弹网"),
    FABRIC_6("6", "方格布"),
    FABRIC_7("7", "针孔布"),
    FABRIC_8("8", "NK有光布"),
    FABRIC_9("9", "灯笼布"),
    FABRIC_10("10", "蝴蝶网"),
    FABRIC_11("11", "耐克提花（勾袖布）"),
    FABRIC_12("12", "阿迪提花（三页前幅）"),
    FABRIC_13("13", "联点布（1.6前幅）"),
    FABRIC_14("14", "牛奶丝（1.6排版）"),
    FABRIC_15("15", "球员布"),
    FABRIC_16("16", "满天星"),
    FABRIC_17("17", "小米通");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ErpFabricEnum::getStatus).toArray(String[]::new);

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
