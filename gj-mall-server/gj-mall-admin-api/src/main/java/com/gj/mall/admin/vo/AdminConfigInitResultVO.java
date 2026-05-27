package com.gj.mall.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "系统配置初始化结果")
public class AdminConfigInitResultVO {
    private String groupCode;
    private Integer createdCount;
    private Integer restoredCount;
    private Integer existingCount;
    private List<String> createdKeys = new ArrayList<>();
    private List<String> restoredKeys = new ArrayList<>();
    private List<String> existingKeys = new ArrayList<>();
}
