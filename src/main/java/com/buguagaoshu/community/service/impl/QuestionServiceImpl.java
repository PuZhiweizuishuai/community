package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:51
 */
@Service
public class QuestionServiceImpl implements QuestionService {
    private final QuestionMapper questionMapper;

    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Override
    public int createQuestion(Question question) {
        return questionMapper.createQuestion(question);
    }
}
