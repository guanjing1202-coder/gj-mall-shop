package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台操作日志查询")
public class AdminOperationLogQueryDTO {

    private String keyword;
    private Long adminId;
    private String requestMethod;
    private Integer status;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
