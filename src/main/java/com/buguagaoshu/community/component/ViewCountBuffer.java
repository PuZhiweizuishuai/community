package com.buguagaoshu.community.component;

import com.buguagaoshu.community.mapper.QuestionMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    /** 单条 UPDATE 的最大问题数，避免 SQL 过长 */
    private static final int BATCH_SIZE = 500;

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
     * 单批失败时把该批增量重新累加回主缓冲，避免浏览数永久丢失。
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
        Map<Object, Object> entries;
        try {
            entries = redis.opsForHash().entries(snapshotKey);
        } catch (Exception e) {
            // 读取失败时保留快照键，避免整批浏览数被删除
            log.warn("浏览数快照读取失败，保留待恢复 key={}", snapshotKey, e);
            return;
        }
        Map<Long, Long> deltas = new LinkedHashMap<>();
        entries.forEach((field, value) -> {
            try {
                long questionId = Long.parseLong(field.toString());
                long delta = Long.parseLong(value.toString());
                if (delta > 0) {
                    deltas.put(questionId, delta);
                }
            } catch (Exception e) {
                log.warn("浏览数回写失败 field={} value={}", field, value);
            }
        });
        try {
            List<Long> ids = new ArrayList<>(deltas.keySet());
            for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
                Map<Long, Long> batch = new LinkedHashMap<>();
                for (Long id : ids.subList(i, Math.min(i + BATCH_SIZE, ids.size()))) {
                    batch.put(id, deltas.get(id));
                }
                try {
                    questionMapper.updateViewCountBatch(batch);
                } catch (Exception e) {
                    log.warn("浏览数批量回写失败，{} 条增量重新入缓冲", batch.size(), e);
                    requeue(batch);
                }
            }
        } finally {
            try {
                redis.delete(snapshotKey);
            } catch (Exception ignored) {
            }
        }
    }

    /** 回写失败的增量重新累加回主缓冲，等下一轮 flush */
    private void requeue(Map<Long, Long> failed) {
        failed.forEach((questionId, delta) -> {
            try {
                redis.opsForHash().increment(BUFFER_KEY, Long.toString(questionId), delta);
            } catch (Exception e) {
                log.error("浏览数增量回填失败 questionId={} delta={}", questionId, delta, e);
            }
        });
    }

    @PreDestroy
    public void flushOnShutdown() {
        flush();
    }
}