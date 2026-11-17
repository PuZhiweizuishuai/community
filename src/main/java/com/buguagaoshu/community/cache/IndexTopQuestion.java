package com.buguagaoshu.community.cache;


import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        List<QuestionDto> refreshed = new ArrayList<>(questionId.length);
        for (Long i : questionId) {
            QuestionDto questionDto = questionService.selectQuestionById(i.toString());
            if (questionDto == null) {
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