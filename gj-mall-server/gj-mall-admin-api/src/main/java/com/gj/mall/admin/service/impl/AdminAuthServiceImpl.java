package com.gj.mall.admin.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.admin.dto.AdminLoginDTO;
import com.gj.mall.admin.entity.SysUser;
import com.gj.mall.admin.mapper.SysUserMapper;
import com.gj.mall.admin.service.AdminAuthService;
import com.gj.mall.admin.vo.AdminLoginVO;
import com.gj.mall.admin.vo.AdminUserVO;
import com.gj.mall.common.constant.CommonConstants;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.framework.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String USER_TYPE = "admin";

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ResultCode.LOGIN_FAIL);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.LOGIN_FAIL);
        }
        log.info("[admin-auth] login success username={} id={}", user.getUsername(), user.getId());
        return issueLogin(user);
    }

    @Override
    public AdminLoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        String userType = String.valueOf(claims.get("typ"));
        if (!USER_TYPE.equals(userType)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.get("uid").toString());
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        return issueLogin(user);
    }

    @Override
    public void logout(Long adminId) {
        if (adminId == null) {
            return;
        }
        redis.delete(CommonConstants.REDIS_ADMIN_TOKEN + adminId);
    }

    private AdminLoginVO issueLogin(SysUser user) {
        String access = jwtUtil.createAccessToken(user.getId(), USER_TYPE);
        String refresh = jwtUtil.createRefreshToken(user.getId(), USER_TYPE);
        redis.opsForValue().set(
                CommonConstants.REDIS_ADMIN_TOKEN + user.getId(),
                access,
                jwtUtil.getAccessExpireSeconds(),
                TimeUnit.SECONDS);
        return AdminLoginVO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtUtil.getAccessExpireSeconds())
                .user(AdminUserVO.from(user))
                .build();
    }
}
