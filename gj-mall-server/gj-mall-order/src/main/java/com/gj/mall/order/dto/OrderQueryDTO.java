package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单分页查询")
public class OrderQueryDTO {

    @Schema(description = "页码")
    private Long pageNum = 1L;

    @Schema(description = "每页大小")
    private Long pageSize = 10L;

    @Schema(description = "订单状态：留空=全部")
    private Integer status;

    @Schema(description = "订单号模糊搜索（仅后台）")
    private String orderNo;

    @Schema(description = "用户 ID（仅后台）")
    private Long userId;

    @Schema(description = "物流单号模糊搜索（仅后台）")
    private String deliveryNo;
}
