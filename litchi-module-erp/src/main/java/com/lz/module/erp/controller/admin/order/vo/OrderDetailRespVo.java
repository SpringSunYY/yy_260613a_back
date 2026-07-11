package com.lz.module.erp.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 订单明细信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailRespVo  {

    /**
     * 编号
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6502")
    private Long id;
    /**
     * 订单号
     */
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;
    /**
     * 名字
     */
    @Schema(description = "名字", example = "赵六")
    private String setName;
    /**
     * 号码
     */
    @Schema(description = "号码")
    private String setNumber;
    /**
     * 尺码
     *
     * 枚举 {@link TODO erp_set_size 对应的类}
     */
    @Schema(description = "尺码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String setSize;
    /**
     * 数量
     */
    @Schema(description = "数量")
    private Integer setQuantity;
    /**
     * 备注
     */
    @Schema(description = "备注", example = "随便")
    private String remark;

}
