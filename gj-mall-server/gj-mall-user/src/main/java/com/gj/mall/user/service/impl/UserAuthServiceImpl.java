package com.gj.mall.user.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.constant.CommonConstants;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.framework.jwt.JwtUtil;
import com.gj.mall.user.dto.LoginDTO;
import com.gj.mall.user.dto.RegisterDTO;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import com.gj.mall.user.service.UserAuthService;
import com.gj.mall.user.vo.LoginVO;
import com.gj.mall.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UmsUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    private static final String USER_TYPE = "user";

    @Override
    @Transactional
    public LoginVO register(RegisterDTO dto, String ip) {
        // 用户名重复
        Long count = userMapper.selectCount(Wrappers.<UmsUser>lambdaQuery()
                .eq(UmsUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "用户名已存在");
        }
        if (StrUtil.isNotBlank(dto.getPhone())) {
            Long phoneCount = userMapper.selectCount(Wrappers.<UmsUser>lambdaQuery()
                    .eq(UmsUser::getPhone, dto.getPhone()));
            if (phoneCount != null && phoneCount > 0) {
                throw new BizException(ResultCode.DATA_EXISTS, "手机号已被注册");
            }
        }

        UmsUser u = new UmsUser();
        u.setUsername(dto.getUsername());
        u.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        u.setNickname(StrUtil.isBlank(dto.getNickname()) ? dto.getUsername() : dto.getNickname());
        u.setPhone(dto.getPhone());
        u.setStatus(1);
        u.setLastLoginAt(LocalDateTime.now());
        u.setLastLoginIp(ip);
        userMapper.insert(u);
        log.info("用户注册成功: {} ({})", u.getUsername(), u.getId());
        return issueLogin(u);
    }

    @Override
    public LoginVO login(LoginDTO dto, String ip) {
        UmsUser u = userMapper.selectOne(Wrappers.<UmsUser>lambdaQuery()
                .eq(UmsUser::getUsername, dto.getAccount())
                .or().eq(UmsUser::getPhone, dto.getAccount()));
        if (u == null) {
            throw new BizException(ResultCode.LOGIN_FAIL);
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword(), u.getPassword())) {
            throw new BizException(ResultCode.LOGIN_FAIL);
        }
        // 更新登录时间
        UmsUser update = new UmsUser();
        update.setId(u.getId());
        update.setLastLoginAt(LocalDateTime.now());
        update.setLastLoginIp(ip);
        userMapper.updateById(update);

        return issueLogin(u);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.get("uid").toString());
        UmsUser u = userMapper.selectById(userId);
        if (u == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        return issueLogin(u);
    }

    @Override
    public void logout(Long userId) {
        if (userId == null) return;
        redis.delete(CommonConstants.REDIS_USER_TOKEN + userId);
    }

    /** 生成双 token + 写入 Redis 白名单 */
    private LoginVO issueLogin(UmsUser u) {
        String access = jwtUtil.createAccessToken(u.getId(), USER_TYPE);
        String refresh = jwtUtil.createRefreshToken(u.getId(), USER_TYPE);
        redis.opsForValue().set(
                CommonConstants.REDIS_USER_TOKEN + u.getId(),
                access,
                jwtUtil.getAccessExpireSeconds(),
                TimeUnit.SECONDS);
        return LoginVO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtUtil.getAccessExpireSeconds())
                .user(UserVO.from(u))
                .build();
    }
}
