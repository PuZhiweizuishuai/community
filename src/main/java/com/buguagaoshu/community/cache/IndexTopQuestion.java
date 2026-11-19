package com.buguagaoshu.community.cache;


import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 首页置顶问题：构建新列表后原子引用替换，避免 clear-add 空窗。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-08 17:47
 */
@Component
public class IndexTopQuestion {
    private final QuestionService questionService;

    private volatile List<QuestionDto> topQuestion = List.of();

    @Autowired
    public IndexTopQuestion(QuestionService questionService) {
        this.questionService = questionService;
    }

    public void setTopQuestion(Long[] questionId) {
        if (questionId == null || questionId.length == 0) {
            topQuestion = List.of();
            return;
        }
        // 批量 IN 查询 + 列表列轻量装配，避免逐 id 查询
        Map<Long, QuestionDto> byId = questionService.selectQuestionDtosByIds(Arrays.asList(questionId))
                .stream().collect(Collectors.toMap(QuestionDto::getQuestionId, Function.identity()));
        List<QuestionDto> refreshed = new ArrayList<>(questionId.length);
        for (Long i : questionId) {
            QuestionDto questionDto = byId.get(i);
            // 待审核 / 未通过的问题不能出现在首页置顶
            if (questionDto == null || questionDto.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
                continue;
            }
            refreshed.add(questionDto.toListItem());
        }
        topQuestion = List.copyOf(refreshed);
    }

    public List<QuestionDto> getTopQuestion() {
        return topQuestion;
    }

}