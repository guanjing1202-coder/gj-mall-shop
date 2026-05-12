package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(description = "用户申请售后")
public class AfterSaleApplyDTO {

    @NotNull
    @Schema(description = "售后类型：1仅退款 2退货退款")
    private Integer type;

    @NotBlank
    @Schema(description = "售后原因")
    private String reason;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "凭证图片 URL")
    private List<String> images;
}
