package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotQuestionCache;
import com.buguagaoshu.community.component.RedisSchedulerLock;
import com.buguagaoshu.community.dto.HotQuestionDTO;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.HotRankingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 热门问题定时器。
 *
 * <p>旧实现是「浏览量×2 + 点赞×5 + 评论×10」的线性累加，既不考虑时间，
 * 也会让一篇远古爆款永久霸榜。现在改用 {@link HotRankingUtil} 的多信号加权 +
 * 对数抑制 + 时间衰减打分，候选池也由 100 扩大到 300。
 */
@Slf4j
@Component
public class HotQuestionTasks {

    private final QuestionService questionService;
    private final HotQuestionCache hotQuestionCache;
    private final RedisSchedulerLock schedulerLock;

    public HotQuestionTasks(QuestionService questionService, HotQuestionCache hotQuestionCache,
                            RedisSchedulerLock schedulerLock) {
        this.questionService = questionService;
        this.hotQuestionCache = hotQuestionCache;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(fixedRate = 3600000)
    public void hotQuestionCurrentTime() {
        if (!schedulerLock.tryLock("hotQuestion", Duration.ofMinutes(10))) {
            return;
        }
        try {
            log.info("开始计算热门问题");
            long now = System.currentTimeMillis();
            List<Question> questions = questionService.getQuestionListForTag(0, HotRankingUtil.CANDIDATE_LIMIT);
            List<HotQuestionDTO> ranked = new ArrayList<>();
            for (Question question : questions) {
                if (question == null || question.getStatus() == 0) {
                    continue;
                }
                HotQuestionDTO dto = new HotQuestionDTO();
                dto.setQuestionId(question.getQuestionId());
                dto.setTitle(question.getTitle());
                dto.setSortWeight(HotRankingUtil.scaled(HotRankingUtil.questionScore(question, now)));
                ranked.add(dto);
            }
            ranked.sort(Comparator.comparingLong(HotQuestionDTO::getSortWeight).reversed()
                    .thenComparingLong(HotQuestionDTO::getQuestionId));
            hotQuestionCache.updateHotQuestion(ranked);
            log.info("热门问题计算完成，候选 {} 条，上榜 {} 条", ranked.size(), hotQuestionCache.getHotQuestionDTOList().size());
        } finally {
            schedulerLock.unlock("hotQuestion");
        }
    }
}
