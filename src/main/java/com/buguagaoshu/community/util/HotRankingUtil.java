package com.buguagaoshu.community.util;

import com.buguagaoshu.community.model.Question;

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

    /** 转成可存进 Long 的排序权值 */
    public static long scaled(double score) {
        return Math.max(0L, Math.round(score * SCALE));
    }
}
