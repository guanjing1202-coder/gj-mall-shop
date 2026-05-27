package com.gj.mall.admin.vo;

import com.gj.mall.admin.entity.SysConfig;
import com.gj.mall.admin.support.AdminConfigSecurity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "系统配置")
public class AdminConfigVO {
    private Long id;
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private String valueTypeDesc;
    private String groupCode;
    private String groupName;
    private String description;
    private Integer editable;
    private Integer status;
    private String statusDesc;
    private Boolean sensitive;
    private Boolean masked;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminConfigVO from(SysConfig config) {
        AdminConfigVO vo = new AdminConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigName(config.getConfigName());
        boolean sensitive = AdminConfigSecurity.isSensitive(config.getConfigKey());
        vo.setConfigValue(sensitive ? AdminConfigSecurity.maskValue(config.getConfigValue()) : config.getConfigValue());
        vo.setValueType(config.getValueType());
        vo.setValueTypeDesc(valueTypeName(config.getValueType()));
        vo.setGroupCode(config.getGroupCode());
        vo.setGroupName(groupName(config.getGroupCode()));
        vo.setDescription(config.getDescription());
        vo.setEditable(config.getEditable());
        vo.setStatus(config.getStatus());
        vo.setStatusDesc(config.getStatus() != null && config.getStatus() == 1 ? "启用" : "停用");
        vo.setSensitive(sensitive);
        vo.setMasked(sensitive);
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private static String valueTypeName(String valueType) {
        Map<String, String> names = new HashMap<>();
        names.put("text", "文本");
        names.put("number", "数字");
        names.put("boolean", "布尔");
        names.put("json", "JSON");
        return names.getOrDefault(valueType, valueType);
    }

    private static String groupName(String groupCode) {
        Map<String, String> names = new HashMap<>();
        names.put("basic", "基础");
        names.put("product", "商品");
        names.put("order", "订单");
        names.put("payment", "支付");
        names.put("marketing", "营销");
        names.put("after_sale", "售后");
        return names.getOrDefault(groupCode, groupCode);
    }
}
