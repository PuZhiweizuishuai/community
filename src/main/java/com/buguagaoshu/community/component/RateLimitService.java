package com.buguagaoshu.community.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Redis 的失败计数锁定与固定窗口限流。
 *
 * <p>多实例共享计数；Redis 不可用时放行（限流是加固手段，不应拖垮主流程），
 * 但会按时间节流输出 error 日志并累计失败次数，便于通过日志/监控发现「防护已失效」。
 */
@Component
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final String FAIL_PREFIX = "limit:fail:";
    private static final String WINDOW_PREFIX = "limit:win:";

    /** 默认锁定阈值：同一维度（邮箱/用户）连续失败 5 次锁定 15 分钟 */
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    /** 降级日志最小间隔，防止 Redis 长时间不可用时日志刷爆磁盘 */
    private static final long DEGRADE_LOG_INTERVAL_MILLIS = 60_000L;

    private final StringRedisTemplate redis;

    /** Redis 操作失败累计次数，可用于监控指标或健康检查 */
    private final AtomicLong degradedCount = new AtomicLong();

    private volatile long lastDegradeLogAt = 0L;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 敏感操作（登录/改绑）连续失败是否已达默认阈值 */
    public boolean isLocked(String key) {
        return isLocked(key, MAX_FAILURES);
    }

    /**
     * 指定阈值的锁定判断：不同维度可用不同阈值，
     * 例如登录 IP 维度放宽到 30 次，避免 NAT 出口下的正常用户互相影响。
     */
    public boolean isLocked(String key, int maxFailures) {
        try {
            String value = redis.opsForValue().get(FAIL_PREFIX + key);
            return value != null && Integer.parseInt(value) >= maxFailures;
        } catch (NumberFormatException e) {
            // 计数被外部写坏时按未锁定处理，避免误锁全部用户
            return false;
        } catch (Exception e) {
            onRedisFailure("isLocked", e);
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
        } catch (Exception e) {
            onRedisFailure("recordFailure", e);
        }
    }

    public void reset(String key) {
        try {
            redis.delete(FAIL_PREFIX + key);
        } catch (Exception e) {
            onRedisFailure("reset", e);
        }
    }

    /**
     * 固定窗口限流：窗口内第 max 次以内的请求返回 true。
     * 用于注册、验证码、找回密码等无登录态入口。
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
            onRedisFailure("tryAcquire", e);
            return true;
        }
    }

    /** 累计降级次数：Redis 异常导致限流失效的次数 */
    public long degradedCount() {
        return degradedCount.get();
    }

    private void onRedisFailure(String operation, Exception e) {
        long total = degradedCount.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastDegradeLogAt >= DEGRADE_LOG_INTERVAL_MILLIS) {
            lastDegradeLogAt = now;
            log.error("Redis 不可用，限流/锁定已降级放行！操作 {}，累计失败 {} 次，请立即检查 Redis 健康状态",
                    operation, total, e);
        }
    }
}
