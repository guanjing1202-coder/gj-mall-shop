package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付回调记录分页查询")
public class AdminPaymentCallbackQueryDTO {

    @Schema(description = "回调编号/支付流水/第三方流水/通知ID")
    private String keyword;

    private Integer channel;
    private Integer signatureStatus;
    private Integer processStatus;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
