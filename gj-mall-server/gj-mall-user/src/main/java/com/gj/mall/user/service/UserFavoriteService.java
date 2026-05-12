package com.gj.mall.user.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.user.vo.UserFavoriteVO;

public interface UserFavoriteService {

    PageResult<UserFavoriteVO> page(Long userId, Long current, Long size);

    Boolean exists(Long userId, Long spuId);

    Long add(Long userId, Long spuId);

    void remove(Long userId, Long spuId);
}
