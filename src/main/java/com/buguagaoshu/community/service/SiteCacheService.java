package com.buguagaoshu.community.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 全站统一的本地 TTL 缓存。
 *
 * <p>社区里大量数据是「读多写少」：热门标签 / 热门问题 / 问题分类 / 话题分类 /
 * 话题列表 / 首页列表 / 在线人数。以前部分接口每次请求都会打到 MySQL，
 * 现在统一通过 {@link #get(String, long, Supplier)} 读取：命中直接返回，
 * 未命中才查库并回填；后台写操作调用 {@link #invalidate(String)} 主动失效。
 *
 * <p>使用本机内存而非 Redis 是为了避免额外的网络往返，适合单实例部署；
 * 若将来做多实例，只需把这里的实现替换成 Redis 并保留同样的接口。
 */
@Service
@Slf4j
public class SiteCacheService {

    /** 缓存键前缀，便于按业务维度批量失效 */
    public static final String META = "meta:";
    public static final String HOME = "home:";
    public static final String TOPICS = "topics:";
    public static final String CLASSIFICATION = "classification:";

    private record Entry(Object value, long expireAt) {
        boolean expired(long now) {
            return now >= expireAt;
        }
    }

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    /**
     * 读取缓存；过期或不存在时调用 loader 加载并回填。
     *
     * @param ttlMillis 存活时间（毫秒）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, long ttlMillis, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        Entry entry = cache.get(key);
        if (entry != null && !entry.expired(now)) {
            hits.incrementAndGet();
            return (T) entry.value();
        }
        misses.incrementAndGet();
        T value = loader.get();
        cache.put(key, new Entry(value, now + Math.max(1000L, ttlMillis)));
        return value;
    }

    /** 按前缀失效，例如 invalidate("home:") 只清首页列表 */
    public void invalidate(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            invalidateAll();
            return;
        }
        int removed = 0;
        for (String key : cache.keySet()) {
            if (key.startsWith(prefix)) {
                cache.remove(key);
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("缓存失效 prefix={} 清理 {} 项", prefix, removed);
        }
    }

    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        log.info("全站缓存已清空，共 {} 项", size);
    }

    /** 供后台「控制面板」展示命中率与缓存条目 */
    public Map<String, Object> stats() {
        long hit = hits.get();
        long miss = misses.get();
        List<Map<String, Object>> keys = new ArrayList<>();
        long now = System.currentTimeMillis();
        cache.forEach((key, entry) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", key);
            item.put("ttlSeconds", Math.max(0, (entry.expireAt() - now) / 1000));
            item.put("expired", entry.expired(now));
            keys.add(item);
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", cache.size());
        data.put("hits", hit);
        data.put("misses", miss);
        data.put("hitRate", hit + miss == 0 ? "0%" : String.format("%.1f%%", hit * 100.0 / (hit + miss)));
        data.put("entries", keys);
        return data;
    }

    /** 每分钟清掉过期条目，避免长期运行后 Map 只增不减 */
    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }
}
