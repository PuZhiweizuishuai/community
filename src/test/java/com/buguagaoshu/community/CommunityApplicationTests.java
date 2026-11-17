package com.buguagaoshu.community;

import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.util.HotRankingUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不依赖外部 MySQL/Redis 的基础逻辑冒烟测试。
 * 完整上下文集成测试需要 Testcontainers 基础设施，见后续迭代。
 */
class CommunityApplicationTests {

    @Test
    void hotRankingPrefersRecentEngagement() {
        long now = System.currentTimeMillis();
        Question fresh = question(now - 3600_000L, now - 60_000L, 10, 5, 100);
        Question stale = question(now - 400L * 86400_000L, now - 300L * 86400_000L, 10, 5, 100);
        assertTrue(HotRankingUtil.questionScore(fresh, now) > HotRankingUtil.questionScore(stale, now),
                "相同互动量下，最近活跃的问题热度应更高");
    }

    @Test
    void hotRankingEngagementIsMonotonic() {
        long now = System.currentTimeMillis();
        Question low = question(now - 3600_000L, now - 60_000L, 1, 0, 10);
        Question high = question(now - 3600_000L, now - 60_000L, 20, 10, 100);
        assertTrue(HotRankingUtil.questionScore(high, now) > HotRankingUtil.questionScore(low, now));
    }

    private Question question(long createTime, long alterTime, int comments, int likes, long views) {
        Question question = new Question();
        question.setCreateTime(createTime);
        question.setAlterTime(alterTime);
        question.setCommentCount(comments);
        question.setLikeCount(likes);
        question.setViewCount(views);
        return question;
    }
}