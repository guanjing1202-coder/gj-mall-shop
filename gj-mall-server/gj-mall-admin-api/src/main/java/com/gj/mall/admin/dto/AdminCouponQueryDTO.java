package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台优惠券分页查询")
public class AdminCouponQueryDTO {

    private String keyword;
    private Integer type;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
