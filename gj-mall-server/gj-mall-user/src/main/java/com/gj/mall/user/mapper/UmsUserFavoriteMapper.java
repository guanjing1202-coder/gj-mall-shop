package com.gj.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gj.mall.user.entity.UmsUserFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

@Mapper
public interface UmsUserFavoriteMapper extends BaseMapper<UmsUserFavorite> {

    @Delete("DELETE FROM ums_user_favorite WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Delete({
            "<script>",
            "DELETE FROM ums_user_favorite WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int physicalDeleteBatchIds(@Param("ids") Collection<Long> ids);
}
