package com.gj.mall.user.service;

import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.vo.UserVO;

public interface UmsUserService {

    UmsUser getById(Long id);

    UserVO getProfile(Long userId);

    void updateProfile(Long userId, UmsUser update);
}
