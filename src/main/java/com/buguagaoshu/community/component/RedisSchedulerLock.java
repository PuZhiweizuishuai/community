package com.buguagaoshu.community.component;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Redis 的定时任务分布式锁：多实例部署时同一任务同一时刻只有一个实例执行。
 * Redis 不可用时退化为单机执行（任务本身应幂等）。
 */
@Component
public class RedisSchedulerLock {

    private static final String KEY_PREFIX = "lock:scheduler:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final String instanceId = UUID.randomUUID().toString();

    public RedisSchedulerLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 尝试占锁；ttl 应大于任务最长执行时间，防止实例崩溃后锁永不释放。
     */
    public boolean tryLock(String taskName, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + taskName, instanceId, ttl));
        } catch (Exception e) {
            return true;
        }
    }

    /** 只释放本实例持有的锁 */
    public void unlock(String taskName) {
        try {
            redis.execute(UNLOCK_SCRIPT, List.of(KEY_PREFIX + taskName), instanceId);
        } catch (Exception ignored) {
        }
    }
}