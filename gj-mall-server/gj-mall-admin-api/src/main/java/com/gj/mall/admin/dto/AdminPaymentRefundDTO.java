package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台退款")
public class AdminPaymentRefundDTO {

    @Schema(description = "退款金额，当前仅支持全额退款；为空时默认支付金额")
    private BigDecimal amount;

    @Schema(description = "退款原因")
    private String reason;
}
