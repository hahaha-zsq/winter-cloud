package com.winter.cloud.common.util;


import com.winter.cloud.common.constants.CommonConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（基于 JJWT 0.13.0）
 */
@Slf4j
public class JwtUtil {

    /**
     * 密钥（至少 256 位，实际使用时应从配置文件读取）
     */
    private static final String SECRET_KEY = "winter-cloud-auth-secret-key-must-be-at-least-256-bits-long-for-hs256";

    /**
     * Token 过期时间（默认 7 天，单位：毫秒）
     */
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 生成密钥
     */
    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     *
     * @param subject 主题（通常是用户ID或用户名）
     * @return Token 字符串
     */
    public static String generateToken(String subject) {
        return generateToken(subject, null);
    }

    /**
     * 生成 Token（带自定义声明）
     *
     * @param subject 主题（通常是用户ID或用户名）
     * @param claims  自定义声明
     * @return Token 字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, EXPIRATION_TIME);
    }

    /**
     * 生成 Token（自定义过期时间）
     *
     * @param subject        主题
     * @param claims         自定义声明
     * @param expirationTime 过期时间（毫秒）
     * @return Token 字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims, long expirationTime) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTime);

      JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey());

        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }

        return builder.compact();
    }

    /**
     * 解析 Token
     *
     * @param token Token 字符串
     * @return Claims 对象
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析 Token 失败: {}", e.getMessage());
            throw new RuntimeException("Token 无效或已过期");
        }
    }

    /**
     * 从 Token 中获取主题（用户ID或用户名）
     *
     * @param token Token 字符串
     * @return 主题
     */
    public static String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中获取自定义声明
     *
     * @param token Token 字符串
     * @param key   声明的键
     * @return 声明的值
     */
    public static Object getClaim(String token, String key) {
        return parseToken(token).get(key);
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token Token 字符串
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token Token 字符串
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新 Token（生成新的 Token）
     *
     * @param token 旧 Token
     * @return 新 Token
     */
    public static String refreshToken(String token) {
        Claims claims = parseToken(token);
        String subject = claims.getSubject();
        return generateToken(subject);
    }

    public static void main(String[] args) {
        HashMap<String, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put(CommonConstants.Claim.NAME, "winter");
        String token = JwtUtil.generateToken("12345",objectObjectHashMap);
        System.err.println(token);
        Claims claims = JwtUtil.parseToken(token);
        System.err.println(claims);
    }

}
