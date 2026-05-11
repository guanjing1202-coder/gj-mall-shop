package com.gj.mall.admin.dto;

import lombok.Data;

@Data
public class AdminRoleQueryDTO {

    private String keyword;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
