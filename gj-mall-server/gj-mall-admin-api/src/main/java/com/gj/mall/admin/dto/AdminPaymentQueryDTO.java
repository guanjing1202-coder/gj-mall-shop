package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台支付记录分页查询")
public class AdminPaymentQueryDTO {

    @Schema(description = "支付流水号/订单号/第三方流水号")
    private String keyword;

    private Long userId;
    private Integer channel;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
