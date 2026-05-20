package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "运费试算请求")
public class FreightQuoteDTO {

    @Schema(description = "收货地址 ID")
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @Schema(description = "商品金额")
    @NotNull(message = "商品金额不能为空")
    @DecimalMin(value = "0", message = "商品金额不能小于 0")
    private BigDecimal orderAmount;
}
