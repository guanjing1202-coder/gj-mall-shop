package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台售后操作")
public class AdminAfterSaleActionDTO {

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "退货物流公司")
    private String returnCompany;

    @Schema(description = "退货物流单号")
    private String returnNo;
}
