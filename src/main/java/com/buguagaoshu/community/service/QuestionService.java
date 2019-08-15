package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.Question;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:51
 */
public interface QuestionService {
    /**
     * 插入问题
     * @param question 问题
     * @return 结果
     * */
    int createQuestion(Question question);
}
