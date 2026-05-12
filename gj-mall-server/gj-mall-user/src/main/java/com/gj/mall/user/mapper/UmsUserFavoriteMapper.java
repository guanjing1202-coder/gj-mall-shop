package com.gj.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gj.mall.user.entity.UmsUserFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

@Mapper
public interface UmsUserFavoriteMapper extends BaseMapper<UmsUserFavorite> {

    @Delete("DELETE FROM ums_user_favorite WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Delete("DELETE FROM ums_user_favorite WHERE user_id = #{userId} AND spu_id = #{spuId}")
    int physicalDeleteByUserIdAndSpuId(@Param("userId") Long userId, @Param("spuId") Long spuId);

    @Update("UPDATE ums_user_favorite SET deleted = 0, create_time = NOW() WHERE user_id = #{userId} AND spu_id = #{spuId} AND deleted <> 0")
    int restoreByUserIdAndSpuId(@Param("userId") Long userId, @Param("spuId") Long spuId);

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
