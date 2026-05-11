package com.gj.mall.admin.dto;

import lombok.Data;

@Data
public class AdminCommentQueryDTO {

    private String keyword;
    private Long userId;
    private Long spuId;
    private Long skuId;
    private Integer score;
    private Integer status;
    private Boolean hasImage;
    private Boolean hasReply;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
