package com.gj.mall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gj.mall.admin.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
    @Select({
            "<script>",
            "SELECT id, config_key, config_name, config_value, value_type, group_code, description, editable, status, create_time, update_time, deleted",
            "FROM sys_config",
            "WHERE config_key IN",
            "<foreach collection='keys' item='key' open='(' separator=',' close=')'>#{key}</foreach>",
            "ORDER BY config_key ASC",
            "</script>"
    })
    List<SysConfig> selectByConfigKeysIncludingDeleted(@Param("keys") List<String> keys);

    @Update({
            "UPDATE sys_config",
            "SET config_name = #{config.configName},",
            "    config_value = #{config.configValue},",
            "    value_type = #{config.valueType},",
            "    group_code = #{config.groupCode},",
            "    description = #{config.description},",
            "    editable = #{config.editable},",
            "    status = #{config.status},",
            "    deleted = 0,",
            "    update_time = NOW()",
            "WHERE id = #{config.id}"
    })
    int restoreDeletedConfig(@Param("config") SysConfig config);
}
