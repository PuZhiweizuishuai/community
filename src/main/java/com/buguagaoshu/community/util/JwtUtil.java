package com.buguagaoshu.community.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-20 15:19
 * jwt 工具类（无状态单例：TTL 由调用方按请求传入，禁止写入实例字段）
 */
public class JwtUtil {

    @Value("${jwt.key}")
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @PostConstruct
    void validateKey() {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.key 必须是至少 32 字节的随机密钥，请通过环境变量或 application-local.yml 注入");
        }
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

    /**
     * 签发「记住我」token。
     *
     * <p>payload 只放用户 ID（jti）与签发/过期时间：JWT 的 payload 只是 Base64 编码，
     * 任何拿到 token 的人都能解码，因此不写入 email / userName / 角色等敏感信息，
     * 这些信息在恢复会话时由服务端按 userId 查库获取。
     *
     * @param id        用户主键
     * @param ttlMillis 有效期（毫秒），<= 0 表示不设过期
     */
    public String createJWT(long id, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        JwtBuilder jwtBuilder = Jwts.builder()
                .id(Long.toString(id))
                .issuedAt(new Date(nowMillis))
                .signWith(signingKey());
        if (ttlMillis > 0) {
            jwtBuilder.expiration(new Date(nowMillis + ttlMillis));
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