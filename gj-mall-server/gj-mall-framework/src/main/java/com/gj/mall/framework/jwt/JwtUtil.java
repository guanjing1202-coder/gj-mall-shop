package com.gj.mall.framework.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${mall.jwt.secret:gj-mall-shop-default-secret-key-please-change-me-in-production-32bytes}")
    private String secret;

    /** access token 过期时间，秒，默认 2 小时 */
    @Value("${mall.jwt.access-expire:7200}")
    private long accessExpireSeconds;

    /** refresh token 过期时间，秒，默认 7 天 */
    @Value("${mall.jwt.refresh-expire:604800}")
    private long refreshExpireSeconds;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String userType) {
        return createToken(userId, userType, accessExpireSeconds);
    }

    public String createRefreshToken(Long userId, String userType) {
        return createToken(userId, userType, refreshExpireSeconds);
    }

    private String createToken(Long userId, String userType, long expireSeconds) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put("uid", userId);
        claims.put("typ", userType);
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000L);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        Object uid = parse(token).get("uid");
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public String getUserType(String token) {
        Object typ = parse(token).get("typ");
        return typ == null ? null : typ.toString();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }
}
