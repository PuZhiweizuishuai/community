package com.buguagaoshu.community.component;

import com.buguagaoshu.community.mapper.QuestionMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 浏览数缓冲：详情接口只往 Redis 累加（INCR），定时批量回写数据库，
 * 避免热点文章每次 GET 都 UPDATE 同一行造成行锁竞争。
 * Redis 不可用时退化为直接写库。
 */
@Component
@Slf4j
public class ViewCountBuffer {

    private static final String BUFFER_KEY = "viewcount:buffer";

    private final StringRedisTemplate redis;
    private final QuestionMapper questionMapper;

    public ViewCountBuffer(StringRedisTemplate redis, QuestionMapper questionMapper) {
        this.redis = redis;
        this.questionMapper = questionMapper;
    }

    public void increment(long questionId) {
        try {
            redis.opsForHash().increment(BUFFER_KEY, Long.toString(questionId), 1);
        } catch (Exception e) {
            questionMapper.increaseViewCount(questionId, 1);
        }
    }

    /** 尚未回写的浏览量，展示时叠加到数据库值上 */
    public long pending(long questionId) {
        try {
            Object value = redis.opsForHash().get(BUFFER_KEY, Long.toString(questionId));
            return value == null ? 0 : Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 每分钟批量回写：先 RENAME 走缓冲键再读取，回写期间的新增量进入新缓冲，不会丢失。
     */
    @Scheduled(fixedRate = 60_000)
    public void flush() {
        String snapshotKey = BUFFER_KEY + ":flush:" + UUID.randomUUID();
        try {
            if (!Boolean.TRUE.equals(redis.hasKey(BUFFER_KEY))) {
                return;
            }
            redis.rename(BUFFER_KEY, snapshotKey);
        } catch (Exception e) {
            return;
        }
        try {
            Map<Object, Object> entries = redis.opsForHash().entries(snapshotKey);
            entries.forEach((field, value) -> {
                try {
                    long questionId = Long.parseLong(field.toString());
                    long delta = Long.parseLong(value.toString());
                    if (delta > 0) {
                        questionMapper.increaseViewCount(questionId, delta);
                    }
                } catch (Exception e) {
                    log.warn("浏览数回写失败 field={} value={}", field, value);
                }
            });
        } finally {
            try {
                redis.delete(snapshotKey);
            } catch (Exception ignored) {
            }
        }
    }

    @PreDestroy
    public void flushOnShutdown() {
        flush();
    }
}