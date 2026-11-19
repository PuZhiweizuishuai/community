package com.buguagaoshu.community.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 全站统一的业务缓存：Redis 为主缓存（多实例共享），本地仅保留 2 秒 L1 抵御热点往返。
 *
 * <p>失效策略：写操作调用 {@link #invalidate(String)} 删除 Redis 键并通过 pub/sub
 * 广播，所有实例清空对应 L1；Redis 不可用时自动降级为本地缓存（全 TTL），不拖垮主流程。
 * TTL 附加随机抖动，避免集中过期。
 */
@Service
@Slf4j
public class SiteCacheService {

    /** 缓存键前缀，便于按业务维度批量失效 */
    public static final String META = "meta:";
    public static final String HOME = "home:";
    public static final String TOPICS = "topics:";
    public static final String CLASSIFICATION = "classification:";

    public static final String INVALIDATE_CHANNEL = "sitecache:invalidate";
    private static final String KEY_PREFIX = "sitecache:";
    private static final long L1_TTL_MILLIS = 2_000L;
    /** Redis 键数量扫描结果缓存 30 秒，避免仪表盘每次打开都全量 SCAN */
    private static final long KEY_COUNT_TTL_MILLIS = 30_000L;

    private record Entry(Object value, long expireAt) {
        boolean expired(long now) {
            return now >= expireAt;
        }
    }

    private final ConcurrentHashMap<String, Entry> l1 = new ConcurrentHashMap<>();
    /** per-key 回源锁：缓存击穿时同一 key 只放一个请求查库 */
    private final ConcurrentHashMap<String, Object> loadLocks = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private volatile long redisKeyCount;
    private volatile long redisKeyCountAt;

    private final RedisTemplate<String, Object> redis;
    private final StringRedisTemplate stringRedis;

    public SiteCacheService(RedisTemplate<String, Object> cacheRedisTemplate, StringRedisTemplate stringRedis) {
        this.redis = cacheRedisTemplate;
        this.stringRedis = stringRedis;
    }

    /**
     * 读取缓存：L1 → Redis → 加锁回源。
     *
     * @param ttlMillis Redis 存活时间（毫秒）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, long ttlMillis, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        Entry entry = l1.get(key);
        if (entry != null && !entry.expired(now)) {
            hits.incrementAndGet();
            return (T) entry.value();
        }
        Object fromRedis = redisGet(key);
        if (fromRedis != null) {
            hits.incrementAndGet();
            l1.put(key, new Entry(fromRedis, now + L1_TTL_MILLIS));
            return (T) fromRedis;
        }
        Object lock = loadLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            // 双重检查：等锁期间可能已有其它请求回填
            now = System.currentTimeMillis();
            entry = l1.get(key);
            if (entry != null && !entry.expired(now)) {
                hits.incrementAndGet();
                return (T) entry.value();
            }
            fromRedis = redisGet(key);
            if (fromRedis != null) {
                hits.incrementAndGet();
                l1.put(key, new Entry(fromRedis, now + L1_TTL_MILLIS));
                return (T) fromRedis;
            }
            misses.incrementAndGet();
            T value = loader.get();
            long base = Math.max(1000L, ttlMillis);
            long jitter = ThreadLocalRandom.current().nextLong(base / 10 + 1);
            boolean stored = value != null && redisSet(key, value, base + jitter);
            // Redis 写入成功时 L1 只留 2 秒；降级或 null 值时本地持有全 TTL，避免反复回源
            long localTtl = stored ? L1_TTL_MILLIS : base + jitter;
            l1.put(key, new Entry(value, now + localTtl));
            return value;
        }
    }

    /** 按前缀失效，例如 invalidate("home:") 只清首页列表 */
    public void invalidate(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            invalidateAll();
            return;
        }
        deleteByPrefix(KEY_PREFIX + prefix);
        clearLocal(prefix);
        broadcast(prefix);
    }

    public void invalidateAll() {
        deleteByPrefix(KEY_PREFIX);
        clearLocal("*");
        broadcast("*");
        log.info("全站缓存已清空");
    }

    /** 清空本地 L1（本实例失效或收到其它实例的广播时调用） */
    public void clearLocal(String prefix) {
        if ("*".equals(prefix)) {
            l1.clear();
            return;
        }
        l1.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /** 供后台「控制面板」展示命中率与缓存条目 */
    public Map<String, Object> stats() {
        long hit = hits.get();
        long miss = misses.get();
        List<Map<String, Object>> keys = new ArrayList<>();
        long now = System.currentTimeMillis();
        l1.forEach((key, entry) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", key);
            item.put("ttlSeconds", Math.max(0, (entry.expireAt() - now) / 1000));
            item.put("expired", entry.expired(now));
            keys.add(item);
        });
        Map<String, Object> data = new LinkedHashMap<>();
        // 键数量 30 秒内复用上次扫描结果，避免仪表盘每次打开都全量 SCAN
        if (now - redisKeyCountAt > KEY_COUNT_TTL_MILLIS) {
            redisKeyCount = countRedisKeys();
            redisKeyCountAt = now;
        }
        data.put("size", l1.size() + redisKeyCount);
        data.put("l1Size", l1.size());
        data.put("hits", hit);
        data.put("misses", miss);
        data.put("hitRate", hit + miss == 0 ? "0%" : String.format("%.1f%%", hit * 100.0 / (hit + miss)));
        data.put("entries", keys);
        return data;
    }

    /** 每分钟清掉过期 L1 条目 */
    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        l1.entrySet().removeIf(entry -> entry.getValue().expired(now));
        loadLocks.keySet().removeIf(key -> !l1.containsKey(key));
    }

    // ------------------------------------------------------------------
    // Redis 操作：全部容错，不可用时降级为纯本地缓存
    // ------------------------------------------------------------------

    private Object redisGet(String key) {
        try {
            return redis.opsForValue().get(KEY_PREFIX + key);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean redisSet(String key, Object value, long ttlMillis) {
        try {
            redis.opsForValue().set(KEY_PREFIX + key, value, Duration.ofMillis(ttlMillis));
            return true;
        } catch (Exception e) {
            log.warn("业务缓存写入 Redis 失败，降级本地缓存 key={}: {}", key, e.getMessage());
            return false;
        }
    }

    private void deleteByPrefix(String prefix) {
        try {
            Set<String> keys = redis.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> result = new HashSet<>();
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(
                        ScanOptions.scanOptions().match(prefix + "*").count(500).build())) {
                    while (cursor.hasNext()) {
                        result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return result;
            });
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            log.warn("业务缓存失效扫描失败 prefix={}: {}", prefix, e.getMessage());
        }
    }

    private long countRedisKeys() {
        try {
            Long count = redis.execute((RedisCallback<Long>) connection -> {
                long n = 0;
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(
                        ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(500).build())) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        n++;
                    }
                }
                return n;
            });
            return count == null ? 0 : count;
        } catch (Exception e) {
            return 0;
        }
    }

    private void broadcast(String prefix) {
        try {
            stringRedis.convertAndSend(INVALIDATE_CHANNEL, prefix);
        } catch (Exception e) {
            log.warn("缓存失效广播失败 prefix={}: {}", prefix, e.getMessage());
        }
    }
}