package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员收藏分页查询")
public class AdminFavoriteQueryDTO {

    private String keyword;
    private Long userId;
    private Long spuId;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
