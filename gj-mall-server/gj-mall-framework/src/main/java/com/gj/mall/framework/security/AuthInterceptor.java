package com.gj.mall.framework.security;

import com.gj.mall.common.constant.CommonConstants;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.framework.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 鉴权拦截器：
 *  - 标注 @AuthExclude 的方法/类直接放行
 *  - 其它请求需在 Authorization 头携带 Bearer token
 *  - 解析后写入 UserContext，供 Controller/Service 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        if (hm.hasMethodAnnotation(AuthExclude.class)
                || hm.getBeanType().isAnnotationPresent(AuthExclude.class)) {
            return true;
        }
        String auth = req.getHeader(CommonConstants.HEADER_TOKEN);
        if (auth == null || !auth.startsWith(CommonConstants.TOKEN_PREFIX)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String token = auth.substring(CommonConstants.TOKEN_PREFIX.length());
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.get("uid").toString());
        String userType = String.valueOf(claims.get("typ"));

        // 校验白名单（被踢下线后 token 不存在）
        String redisKey = "admin".equals(userType)
                ? CommonConstants.REDIS_ADMIN_TOKEN + userId
                : CommonConstants.REDIS_USER_TOKEN + userId;
        String stored = redis.opsForValue().get(redisKey);
        if (stored == null || !stored.equals(token)) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }

        UserContext.set(UserContext.CurrentUser.builder()
                .userId(userId)
                .userType(userType)
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        UserContext.clear();
    }
}
