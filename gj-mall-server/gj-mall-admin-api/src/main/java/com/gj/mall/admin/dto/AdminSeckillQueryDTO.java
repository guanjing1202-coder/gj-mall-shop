package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台秒杀活动分页查询")
public class AdminSeckillQueryDTO {

    private String keyword;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
