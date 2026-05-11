package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "物流公司查询")
public class AdminDeliveryCompanyQueryDTO {

    private Long pageNum = 1L;
    private Long pageSize = 10L;
    private String keyword;
    private Integer status;
}
