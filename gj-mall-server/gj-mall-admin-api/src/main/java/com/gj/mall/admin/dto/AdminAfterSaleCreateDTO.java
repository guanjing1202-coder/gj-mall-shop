package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "后台创建售后单")
public class AdminAfterSaleCreateDTO {

    @NotNull
    @Schema(description = "订单ID")
    private Long orderId;

    @NotNull
    @Schema(description = "售后类型：1仅退款 2退货退款")
    private Integer type;

    @Schema(description = "售后金额，当前仅支持全额")
    private BigDecimal amount;

    @Schema(description = "售后原因")
    private String reason;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "凭证图片URL")
    private List<String> images;
}
