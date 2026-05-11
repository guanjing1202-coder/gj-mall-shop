package com.gj.mall.pay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "支付请求")
public class PayDTO {

    @Schema(description = "订单 ID")
    @NotNull
    private Long orderId;

    @Schema(description = "渠道：wechat / alipay / mock", example = "mock")
    @NotNull
    private String channel;
}
