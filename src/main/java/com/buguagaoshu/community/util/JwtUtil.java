package com.buguagaoshu.community.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-20 15:19
 * jwt 工具类
 */
public class JwtUtil {
    /**
     * 要加的盐
     * */
    @Value("${jwt.key}")
    private String key;

    /**
     * 过期时间
     * 默认一小时
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

    public String createJWT(long id, String userId, String userName,String email, int power) {
        long nowMillis = System.currentTimeMillis();
        JwtBuilder jwtBuilder = Jwts.builder().setId(Long.toString(id))
                .setSubject(userId)
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, key)
                .claim("roles", power)
                .claim("email", email)
                .claim("userName", userName);
        if(ttl > 0) {
            jwtBuilder.setExpiration(new Date(nowMillis + ttl));
        }
        return jwtBuilder.compact();
    }

    /**
     * 解析 jwt
     * */
    public Claims parseJWT(String jwtStr) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(jwtStr)
                .getBody();
    }
}
