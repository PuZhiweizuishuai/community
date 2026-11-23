package com.buguagaoshu.community.util;

import com.buguagaoshu.community.dto.TopicActivityStat;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;

/**
 * 热门内容排序打分工具。
 *
 * <p>参考 Hacker News / Reddit 的「热度 = 互动质量 ÷ 时间衰减」思路，并针对问答社区做了三点调整：
 *
 * <ol>
 *   <li><b>多信号加权</b>：评论 &gt; 点赞 &gt; 关注 &gt; 浏览，浏览量大但信息量低，权重最小。</li>
 *   <li><b>对数抑制</b>：用 {@code 1 + log1p(engagement)} 代替线性累加，避免一两篇远古爆款长期霸榜。</li>
 *   <li><b>双时间衰减</b>：用「最后活跃时间」做指数衰减（旧帖被重新讨论可以回暖），
 *       再用「发布时间」做一个温和的 gravity 分母，保证新内容仍有曝光机会。</li>
 * </ol>
 *
 * <p>分数本身是无单位的相对值，只用于排序；对外统一放大 100 万倍后以 long 存储，
 * 保证老帖（分数很小）之间仍能区分出先后顺序。
 */
public final class HotRankingUtil {

    /** 候选池：只扫描最近 N 条状态正常的问题，避免全表扫描 */
    public static final long CANDIDATE_LIMIT = 300L;

    /** 互动信号权重 */
    private static final double W_VIEW = 1.0;
    private static final double W_LIKE = 6.0;
    private static final double W_COMMENT = 10.0;
    private static final double W_FOLLOW = 4.0;

    /** 最后活跃时间的衰减尺度（天） */
    private static final double IDLE_DECAY_DAYS = 21.0;
    /**
     * 衰减下限：老帖不会归零。历史内容仍按互动质量与发布时间排序，
     * 同时也避免纯浮点下溢导致所有老帖 sortWeight 都变成 0。
     */
    private static final double RECENCY_FLOOR = 0.05;
    /** 发布时间 gravity 指数，越小对老帖越宽容 */
    private static final double GRAVITY = 0.35;

    private static final double DAY_MILLIS = 86_400_000.0;
    private static final double SCALE = 1_000_000.0;

    /** 话题活跃统计窗口：最近 30 天有新增或回复的帖子参与热度计算 */
    public static final long TOPIC_ACTIVITY_WINDOW_MILLIS = 30L * 86_400_000L;
    /** 话题新讨论统计窗口：最近 7 天 */
    public static final long TOPIC_NEW_WINDOW_MILLIS = 7L * 86_400_000L;

    /** 长期沉淀权重：关注数、历史讨论数 */
    private static final double W_TOPIC_FOLLOW = 3.0;
    private static final double W_TOPIC_HISTORY = 2.0;
    /** 近期热度权重：新讨论 > 活跃帖子 > 评论 > 点赞 > 浏览 */
    private static final double W_TOPIC_NEW_POST = 8.0;
    private static final double W_TOPIC_ACTIVE_POST = 3.0;
    private static final double W_TOPIC_COMMENT = 0.5;
    private static final double W_TOPIC_LIKE = 0.2;
    private static final double W_TOPIC_VIEW = 0.02;
    /** 近期热度在总分中的占比，越高越偏向「当下正在热门」 */
    private static final double TOPIC_RECENT_WEIGHT = 2.0;
    /** 话题最后活跃时间的衰减尺度（天），比单帖更短，强调当前热度 */
    private static final double TOPIC_IDLE_DECAY_DAYS = 7.0;
    /** 衰减下限：长期无人讨论的话题保留少量历史沉淀分 */
    private static final double TOPIC_RECENCY_FLOOR = 0.15;

    private HotRankingUtil() {
    }

    /** 带权互动量，浏览、点赞、评论、关注分别计权 */
    public static double engagement(Question question) {
        return W_VIEW * Math.max(0, question.getViewCount())
                + W_LIKE * Math.max(0, question.getLikeCount())
                + W_COMMENT * Math.max(0, question.getCommentCount())
                + W_FOLLOW * Math.max(0, question.getFollowCount());
    }

    /**
     * 问题热度分：{@code (1 + log1p(互动)) * exp(-闲置小时/半衰期) / (发布天数 + 2)^gravity}。
     */
    public static double questionScore(Question question, long now) {
        long created = question.getCreateTime() > 0 ? question.getCreateTime() : now;
        long lastActive = Math.max(created, Math.max(question.getAlterTime(), created));
        double ageDays = Math.max(0, (now - created) / DAY_MILLIS);
        double idleDays = Math.max(0, (now - lastActive) / DAY_MILLIS);

        double engagement = 1 + Math.log1p(engagement(question));
        double recency = RECENCY_FLOOR + (1 - RECENCY_FLOOR) * Math.exp(-idleDays / IDLE_DECAY_DAYS);
        double penalty = Math.pow(ageDays + 2, GRAVITY);
        return engagement * recency / penalty;
    }

    /**
     * 单篇问题对某个标签的热度贡献。标签维度不额外做 gravity，
     * 只用「最后活跃时间」的指数衰减 + 互动对数，避免小众标签永远无法上榜。
     */
    public static double tagContribution(Question question, long now) {
        long created = question.getCreateTime() > 0 ? question.getCreateTime() : now;
        long lastActive = Math.max(created, Math.max(question.getAlterTime(), created));
        double idleDays = Math.max(0, (now - lastActive) / DAY_MILLIS);
        // 标签维度衰减更宽容（21 天 + 下限），让长尾技术标签也能沉淀下来
        double recency = 0.15 + 0.85 * Math.exp(-idleDays / IDLE_DECAY_DAYS);
        return (1 + Math.log1p(engagement(question))) * recency;
    }

    /**
     * 话题热度分，参考 Hacker News / Reddit 的「互动质量 × 时间衰减」：
     *
     * <ol>
     *   <li><b>长期沉淀</b>：关注数与历史讨论数取对数，避免超大话题碾压小话题。</li>
     *   <li><b>近期热度</b>：最近 7 天新讨论权重最高，其次为最近 30 天活跃帖子、
     *       评论、点赞与浏览，浏览量权重最低。</li>
     *   <li><b>时间衰减</b>：按话题最后活跃时间做 7 天尺度的指数衰减，
     *       长期无人讨论的话题保留下限分，正在讨论的话题获得完整权重。</li>
     * </ol>
     *
     * @param topic 话题（关注数、历史讨论数来自 topics 表）
     * @param stat  近期活跃统计，无活跃时可为 null
     * @param now   当前时间
     * @return 无单位的相对热度分，仅用于排序
     */
    public static double topicScore(TagClass topic, TopicActivityStat stat, long now) {
        if (topic == null) {
            return 0;
        }
        double legacy = Math.log1p(W_TOPIC_FOLLOW * Math.max(0, topic.getFollowCount())
                + W_TOPIC_HISTORY * Math.max(0, topic.getTalkCount()));

        long activePosts = stat == null ? 0 : Math.max(0, stat.getActiveQuestionCount());
        long newPosts = stat == null ? 0 : Math.max(0, stat.getNewQuestionCount());
        long comments = stat == null ? 0 : Math.max(0, stat.getCommentCount());
        long likes = stat == null ? 0 : Math.max(0, stat.getLikeCount());
        long views = stat == null ? 0 : Math.max(0, stat.getViewCount());
        double recent = Math.log1p(W_TOPIC_NEW_POST * newPosts
                + W_TOPIC_ACTIVE_POST * activePosts
                + W_TOPIC_COMMENT * comments
                + W_TOPIC_LIKE * likes
                + W_TOPIC_VIEW * views);

        long lastActive = stat == null ? 0 : stat.getLastActiveTime();
        // 无活跃记录时按极长闲置处理，只保留衰减下限分
        double idleDays = lastActive > 0 ? Math.max(0, (now - lastActive) / DAY_MILLIS) : 3650.0;
        double recency = TOPIC_RECENCY_FLOOR
                + (1 - TOPIC_RECENCY_FLOOR) * Math.exp(-idleDays / TOPIC_IDLE_DECAY_DAYS);
        return (legacy + TOPIC_RECENT_WEIGHT * recent) * recency;
    }

    /** 转成可存进 Long 的排序权值 */
    public static long scaled(double score) {
        return Math.max(0L, Math.round(score * SCALE));
    }
}
