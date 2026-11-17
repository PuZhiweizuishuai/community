package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.component.RedisSchedulerLock;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.HotRankingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 热门标签定时器。
 *
 * <p>旧实现是「每篇问题给标签加 评论数 + 10」，没有时间衰减、没有去重、
 * 空标签也会上榜。新版做法：
 *
 * <ul>
 *   <li>按「最后活跃时间」做 14 天指数衰减，最近被讨论的标签自然上浮；</li>
 *   <li>每篇问题的贡献经过对数抑制，避免单篇爆款带飞一个标签；</li>
 *   <li>标签 trim + 大小写归一去重，过滤空串，同一篇里重复标签只算一次；</li>
 *   <li>汇总分数再做一次对数压缩，让长尾标签也有机会进入榜单。</li>
 * </ul>
 */
@Slf4j
@Component
public class HotTagTasks {

    private final QuestionService questionService;
    private final HotTagCache hotTagCache;
    private final RedisSchedulerLock schedulerLock;

    public HotTagTasks(QuestionService questionService, HotTagCache hotTagCache,
                       RedisSchedulerLock schedulerLock) {
        this.questionService = questionService;
        this.hotTagCache = hotTagCache;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(fixedRate = 3600000)
    public void hotTagsCurrentTime() {
        if (!schedulerLock.tryLock("hotTag", Duration.ofMinutes(10))) {
            return;
        }
        try {
            log.info("开始计算热门标签");
            long now = System.currentTimeMillis();
            List<Question> questions = questionService.getQuestionListForTag(0, HotRankingUtil.CANDIDATE_LIMIT);

            Map<String, Double> scores = new HashMap<>();
            Map<String, String> displayNames = new LinkedHashMap<>();

            for (Question question : questions) {
                if (question == null || question.getStatus() == 0 || question.getTag() == null) {
                    continue;
                }
                double contribution = HotRankingUtil.tagContribution(question, now);
                Set<String> normalized = new LinkedHashSet<>();
                for (String raw : question.getTag().split("[,，]")) {
                    String tag = raw == null ? "" : raw.trim();
                    if (tag.isEmpty()) {
                        continue;
                    }
                    String key = tag.toLowerCase(Locale.ROOT);
                    if (!normalized.add(key)) {
                        continue;
                    }
                    scores.merge(key, contribution, Double::sum);
                    displayNames.putIfAbsent(key, tag);
                }
            }

            Map<String, Long> priorities = new HashMap<>();
            scores.forEach((key, value) ->
                    priorities.put(displayNames.getOrDefault(key, key),
                            HotRankingUtil.scaled(1 + Math.log1p(value))));

            hotTagCache.updateTags(priorities);
            log.info("热门标签计算完成，候选标签 {} 个，上榜 {} 个", priorities.size(), hotTagCache.getHots().size());
        } finally {
            schedulerLock.unlock("hotTag");
        }
    }
}
