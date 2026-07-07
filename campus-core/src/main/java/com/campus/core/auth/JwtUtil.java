package com.campus.core.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret:Y29tcHV0ZXItc2NpZW5jZS1zbWFydC1jYW1wdXMtand0LXNlY3JldC1rZXktMjAyNg==}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Long userId, String studentId, Integer role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("studentId", studentId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中提取用户信息
     */
    public UserContext.UserInfo getUserInfo(String token) {
        Claims claims = parseToken(token);
        return new UserContext.UserInfo(
                Long.valueOf(claims.getSubject()),
                claims.get("studentId", String.class),
                claims.get("role", Integer.class)
        );
    }
}
