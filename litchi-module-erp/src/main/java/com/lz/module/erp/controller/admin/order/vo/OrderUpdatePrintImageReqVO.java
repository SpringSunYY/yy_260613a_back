package com.lz.module.erp.controller.admin.order.vo;

import com.lz.framework.common.validation.i18n.I18nMax;
import com.lz.framework.common.validation.i18n.I18nMin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台 - 上传图片vo
 *
 * @Project: LZ-Order
 * @Author: YY
 * @CreateTime: 2026-07-05
 * @Version: 1.0
 */
@Schema(description = "管理后台 - 以图搜图 Request VO")
@Data
public class OrderUpdatePrintImageReqVO {

    /**
     * 编号
     */
    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "25663")
    private String orderNo;

    @Schema(description = "上传文件", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;

}
