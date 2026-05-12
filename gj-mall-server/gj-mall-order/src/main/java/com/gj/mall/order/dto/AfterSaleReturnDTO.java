package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "用户填写退货物流")
public class AfterSaleReturnDTO {

    @NotBlank
    @Schema(description = "退货物流公司")
    private String returnCompany;

    @NotBlank
    @Schema(description = "退货物流单号")
    private String returnNo;
}
