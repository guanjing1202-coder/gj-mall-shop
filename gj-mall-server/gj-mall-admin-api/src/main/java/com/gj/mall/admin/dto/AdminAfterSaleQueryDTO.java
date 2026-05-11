package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台售后单分页查询")
public class AdminAfterSaleQueryDTO {

    @Schema(description = "售后单号/订单号/原因")
    private String keyword;

    private Long userId;
    private Integer type;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
