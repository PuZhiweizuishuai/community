package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotQuestionCache;
import com.buguagaoshu.community.dto.HotQuestionDTO;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-09 18:46
 * 热门问题定时器
 */
@Slf4j
@Component
public class HotQuestionTasks {

    private final QuestionService questionService;

    private final HotQuestionCache hotQuestionCache;

    @Autowired
    public HotQuestionTasks(QuestionService questionService, HotQuestionCache hotQuestionCache) {
        this.questionService = questionService;
        this.hotQuestionCache = hotQuestionCache;
    }

    @Scheduled(fixedRate = 3600000)
    public void hotQuestionCurrentTime() {
        long offset = 0;
        // 暴力循环最新的一百条提问计算热门话题
        long limit = 100;
        log.info("开始循环计算最热问题");
        List<Question> questionList = questionService.getQuestionListForTag(offset, limit);
        List<HotQuestionDTO> hotQuestionDTOList = new ArrayList<>();
        for (Question question : questionList) {
            HotQuestionDTO hotQuestionDTO = new HotQuestionDTO();
            hotQuestionDTO.setQuestionId(question.getQuestionId());
            hotQuestionDTO.setTitle(question.getTitle());
            // 设置排序权值
            hotQuestionDTO.setSortWeight(question.getViewCount() * 2 + question.getLikeCount() * 5 + question.getCommentCount() * 10);
            hotQuestionDTOList.add(hotQuestionDTO);
        }
        hotQuestionCache.updateHotQuestion(hotQuestionDTOList);
        log.info("最热问题计算完成");
    }
}
