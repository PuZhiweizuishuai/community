package com.buguagaoshu.community.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-20 15:19
 * jwt 工具类
 *
 * <p>升级到 jjwt 0.13.0 后，HS256 要求密钥至少 32 字节；
 * 这里对配置项 jwt.key 做 SHA-256 派生，保持原有短字符串密钥配置可用。
 */
public class JwtUtil {
    /**
     * 要加的盐
     * */
    @Value("${jwt.key}")
    private String key;

    /**
     * 过期时间
     * 默认二小时
     * */
    @Value("${jwt.ttl}")
    private long ttl;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    /**
     * 由配置字符串派生出符合 HS256 强度要求的密钥
     */
    private SecretKey signingKey() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    public String createJWT(long id, String userId, String userName, String email, int power) {
        long nowMillis = System.currentTimeMillis();
        JwtBuilder jwtBuilder = Jwts.builder()
                .id(Long.toString(id))
                .subject(userId)
                .issuedAt(new Date(nowMillis))
                .claim("roles", power)
                .claim("email", email)
                .claim("userName", userName)
                .signWith(signingKey());
        if (ttl > 0) {
            jwtBuilder.expiration(new Date(nowMillis + ttl));
        }
        return jwtBuilder.compact();
    }

    /**
     * 解析 jwt
     * */
    public Claims parseJWT(String jwtStr) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(jwtStr)
                .getPayload();
    }
}
