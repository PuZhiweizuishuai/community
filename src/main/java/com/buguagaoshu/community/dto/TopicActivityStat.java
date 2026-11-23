package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 话题近期活跃统计：由一次分组聚合查询得出，供热门话题打分使用。
 */
@Data
public class TopicActivityStat {
    private Long topicId;
    /** 统计窗口内（最近 30 天）有新增或回复的帖子数 */
    private long activeQuestionCount;
    /** 其中最近 7 天新发布的帖子数 */
    private long newQuestionCount;
    private long commentCount;
    private long likeCount;
    private long viewCount;
    /** 统计窗口内最后活跃时间，无活跃时为 0 */
    private long lastActiveTime;
}
