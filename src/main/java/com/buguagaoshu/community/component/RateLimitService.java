package com.buguagaoshu.community.component;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的失败计数锁定与固定窗口限流。
 * 多实例共享计数；Redis 不可用时放行（限流是加固手段，不应拖垮主流程）。
 */
@Component
public class RateLimitService {

    private static final String FAIL_PREFIX = "limit:fail:";
    private static final String WINDOW_PREFIX = "limit:win:";
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 敏感操作（登录/改绑）连续失败是否已达锁定阈值 */
    public boolean isLocked(String key) {
        try {
            String value = redis.opsForValue().get(FAIL_PREFIX + key);
            return value != null && Integer.parseInt(value) >= MAX_FAILURES;
        } catch (Exception e) {
            return false;
        }
    }

    public void recordFailure(String key) {
        try {
            String redisKey = FAIL_PREFIX + key;
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, LOCK_WINDOW);
            }
        } catch (Exception ignored) {
        }
    }

    public void reset(String key) {
        try {
            redis.delete(FAIL_PREFIX + key);
        } catch (Exception ignored) {
        }
    }

    /**
     * 固定窗口限流：窗口内第 max 次以内的请求返回 true。
     * 用于注册、验证码等无登录态入口。
     */
    public boolean tryAcquire(String key, int max, Duration window) {
        try {
            String redisKey = WINDOW_PREFIX + key;
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, window);
            }
            return count == null || count <= max;
        } catch (Exception e) {
            return true;
        }
    }
}