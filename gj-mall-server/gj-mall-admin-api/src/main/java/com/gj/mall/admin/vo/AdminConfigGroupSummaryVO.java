package com.gj.mall.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "系统配置分组摘要")
public class AdminConfigGroupSummaryVO {
    private String groupCode;
    private String groupName;
    private Integer totalCount;
    private Integer requiredCount;
    private Integer readyCount;
    private Integer enabledCount;
    private Integer filledCount;
    private Integer missingCount;
    private Integer uninitializedCount;
    private Integer sensitiveCount;
    private Integer completenessPercent;
    private List<String> missingKeys = new ArrayList<>();
    private List<String> uninitializedKeys = new ArrayList<>();
}
