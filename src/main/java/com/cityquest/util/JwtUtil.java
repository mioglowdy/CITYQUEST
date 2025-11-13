package com.cityquest.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 生成token
     */
    public String generateToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 解析token
     */
    public Map<String, Object> parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证token
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 从token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Map<String, Object> claims = parseToken(token);
        return claims != null ? claims.get("userId").toString() : null;
    }

    /**
     * 从token中获取用户角色
     */
    public String getUserRoleFromToken(String token) {
        Map<String, Object> claims = parseToken(token);
        return claims != null ? claims.get("role").toString() : null;
    }
}