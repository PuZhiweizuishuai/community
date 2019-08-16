package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.model.Question;

import java.util.List;

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

    /**
     * TODO 优化分页
     * @param page 页码
     * @param size 每页显示数量
     * 获取问题列表,并添加相应用户信息
     * @return 问题列表
     * */
    PaginationDto getSomeQuestionDto(String page, String size);
}
