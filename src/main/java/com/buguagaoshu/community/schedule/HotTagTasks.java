package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 13:40
 * 热门话题定时器
 */
@Slf4j
@Component
public class HotTagTasks {
    final
    QuestionService questionService;

    private final HotTagCache hotTagCache;

    @Autowired
    public HotTagTasks(QuestionService questionService, HotTagCache hotTagCache) {
        this.questionService = questionService;
        this.hotTagCache = hotTagCache;
    }

    /**
     * 热门话题每一小时更新一次
     * */
    @Scheduled(fixedRate = 3600000)
    public void hotTagsCurrentTime() {
        long offset = 0;
        // 暴力循环最新的一百条提问计算热门话题
        long limit = 100;
        log.info("开始循环计算最热话题", new Date());
        Map<String, Long> priorities = new HashMap<>(128);
        List<Question> questionDtoList = questionService.getQuestionListForTag(offset, limit);
        for(Question question : questionDtoList) {
            String[] tags = question.getTag().split(",|，");
            for(String tag : tags) {
                Long priority = priorities.get(tag);
                if(priority != null) {
                    // 简单的计算，具体的权重需要自己改
                    priorities.put(tag, priority + question.getCommentCount() + 10);
                } else {
                    priorities.put(tag, question.getCommentCount() + 10);
                }
            }
        }
        hotTagCache.updateTags(priorities);
        log.info("最热话题计算完成 {}", new Date());
    }
}
