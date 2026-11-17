package com.buguagaoshu.community.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 邮箱相关的一次性凭据，全部存 Redis（opaque token，可吊销、自带过期）：
 * - 找回密码 token：30 分钟有效，使用即销毁
 * - 注册邮箱验证码：10 分钟有效，校验即销毁
 */
@Service
public class EmailTokenService {

    private static final String RESET_PREFIX = "mail:pwdreset:";
    private static final String REGISTER_PREFIX = "mail:regcode:";
    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final Duration REGISTER_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public EmailTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String createResetToken(long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(RESET_PREFIX + token, Long.toString(userId), RESET_TTL);
        return token;
    }

    /** 消费找回密码 token：返回用户 id，无效/过期返回 null */
    public Long consumeResetToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = RESET_PREFIX + token;
        String userId = redis.opsForValue().get(key);
        if (userId == null) {
            return null;
        }
        redis.delete(key);
        return Long.valueOf(userId);
    }

    public String createRegisterCode(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redis.opsForValue().set(REGISTER_PREFIX + email.toLowerCase(), code, REGISTER_TTL);
        return code;
    }

    /** 校验注册邮箱验证码：无论成败立即销毁，防止复用 */
    public boolean verifyRegisterCode(String email, String code) {
        if (email == null || code == null || code.isBlank()) {
            return false;
        }
        String key = REGISTER_PREFIX + email.toLowerCase();
        String expected = redis.opsForValue().get(key);
        redis.delete(key);
        return code.trim().equals(expected);
    }
}