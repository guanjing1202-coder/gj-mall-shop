package com.gj.mall.user.service.impl;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import com.gj.mall.user.service.UmsUserService;
import com.gj.mall.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UmsUserServiceImpl implements UmsUserService {

    private final UmsUserMapper userMapper;

    @Override
    public UmsUser getById(Long id) {
        UmsUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        return u;
    }

    @Override
    public UserVO getProfile(Long userId) {
        return UserVO.from(getById(userId));
    }

    @Override
    public void updateProfile(Long userId, UmsUser update) {
        update.setId(userId);
        // 安全：禁止前端传密码/状态/openid
        update.setPassword(null);
        update.setStatus(null);
        update.setWxOpenid(null);
        update.setWxUnionid(null);
        update.setUsername(null);
        userMapper.updateById(update);
    }
}
