package com.buguagaoshu.community;

import com.buguagaoshu.community.dto.TopicActivityStat;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;
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

    @Test
    void hotTopicScorePrefersRecentActivity() {
        long now = System.currentTimeMillis();
        TagClass topic = topic(100, 500);
        TopicActivityStat recent = topicStat(20, 5, 40, 100, 2000, now - 60_000L);
        TopicActivityStat stale = topicStat(0, 0, 0, 0, 0, now - 300L * 86400_000L);
        assertTrue(HotRankingUtil.topicScore(topic, recent, now) > HotRankingUtil.topicScore(topic, stale, now),
                "相同历史沉淀下，近期有新讨论与互动的话题热度应更高");
    }

    @Test
    void hotTopicScoreBalancesHistoryAndRecentActivity() {
        long now = System.currentTimeMillis();
        TagClass legacy = topic(5000, 50000);
        TagClass active = topic(5, 20);
        TopicActivityStat activeStat = topicStat(30, 8, 60, 150, 3000, now - 3600_000L);
        assertTrue(HotRankingUtil.topicScore(active, activeStat, now) > HotRankingUtil.topicScore(legacy, null, now),
                "长期无讨论的巨型话题不应压过正在活跃的小话题");
    }

    @Test
    void hotTopicScorePrefersNewDiscussions() {
        long now = System.currentTimeMillis();
        TagClass topic = topic(50, 100);
        TopicActivityStat fewNew = topicStat(20, 1, 20, 40, 500, now - 60_000L);
        TopicActivityStat manyNew = topicStat(20, 10, 20, 40, 500, now - 60_000L);
        assertTrue(HotRankingUtil.topicScore(topic, manyNew, now) > HotRankingUtil.topicScore(topic, fewNew, now),
                "最近 7 天新增讨论更多的话题热度应更高");
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

    private TagClass topic(long followCount, long talkCount) {
        TagClass tagClass = new TagClass();
        tagClass.setFollowCount(followCount);
        tagClass.setTalkCount(talkCount);
        return tagClass;
    }

    private TopicActivityStat topicStat(long activePosts, long newPosts, long comments, long likes,
                                        long views, long lastActiveTime) {
        TopicActivityStat stat = new TopicActivityStat();
        stat.setActiveQuestionCount(activePosts);
        stat.setNewQuestionCount(newPosts);
        stat.setCommentCount(comments);
        stat.setLikeCount(likes);
        stat.setViewCount(views);
        stat.setLastActiveTime(lastActiveTime);
        return stat;
    }
}