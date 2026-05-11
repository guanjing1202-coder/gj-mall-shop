package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统配置查询")
public class AdminConfigQueryDTO {

    private String keyword;
    private String groupCode;
    private String valueType;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
