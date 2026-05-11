package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台物流订单查询")
public class AdminLogisticsOrderQueryDTO {

    private Long pageNum = 1L;
    private Long pageSize = 10L;
    private Integer status;
    private String orderNo;
    private Long userId;
    private String deliveryNo;
    private String deliveryCompany;
}
