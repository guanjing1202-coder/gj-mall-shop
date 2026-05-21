package com.gj.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gj.mall.user.entity.UmsUserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UmsUserMessageMapper extends BaseMapper<UmsUserMessage> {

    @Update("UPDATE ums_user_message SET read_status = 1, read_time = NOW() WHERE user_id = #{userId} AND read_status = 0 AND deleted = 0")
    int markAllRead(@Param("userId") Long userId);

    @Update("UPDATE ums_user_message SET deleted = 1 WHERE user_id = #{userId} AND read_status = 1 AND deleted = 0")
    int clearRead(@Param("userId") Long userId);
}
