package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "系统配置保存")
public class AdminConfigSaveDTO {

    private Long id;

    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键不能超过100个字符")
    private String configKey;

    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称不能超过100个字符")
    private String configName;

    @Size(max = 1000, message = "配置值不能超过1000个字符")
    private String configValue;

    private String valueType;
    private String groupCode;

    @Size(max = 255, message = "说明不能超过255个字符")
    private String description;

    private Integer editable;
    private Integer status;
}
