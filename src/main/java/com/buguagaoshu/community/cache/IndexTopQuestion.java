package com.buguagaoshu.community.cache;


import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-08 17:47
 * 首页置顶问题
 */
@Component
public class IndexTopQuestion {
    private final QuestionService questionService;

    private List<QuestionDto> topQuestion = new ArrayList<>(3);

    @Autowired
    public IndexTopQuestion(QuestionService questionService) {
        this.questionService = questionService;
    }

    public void setTopQuestion(Long[] questionId) {
        topQuestion.clear();
        for(Long i : questionId) {
            QuestionDto questionDto = questionService.selectQuestionById(i.toString());
            if(questionDto == null) {
                continue;
            }
            topQuestion.add(questionDto);
        }
    }

    public List<QuestionDto> getTopQuestion() {
        return topQuestion;
    }

}
