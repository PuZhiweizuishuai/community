package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
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

    /**
     * @param page 页码
     * @param size 每页显示数量
     * 获取问题列表,并添加相应用户信息
     * @return 问题列表
     * */
    PaginationDto getSomeQuestionDto(String page, String size);


    /**
     * 获取当前用户发布的问题列表
     * @param page 页码
     * @param size 每页显示数量
     * @param id 用户id
     * @return 问题列表
     * */
    PaginationDto getQuestionByUserId(String page, String size, long id);

    /**
     * 查找问题
     * @param questionId 问题id
     * @return 问题 DTO
     * */
    QuestionDto selectQuestionById(String questionId);

    /**
     * 查找问题
     * @param questionId 问题id
     * @return 问题
     * */
    Question selectQuestionNotDtoById(String questionId);

    /**
     * 阅读数加 1
     * @param questionId 问题 id
     * @return 阅读数加 1
     * */
    int updateQuestionViewCount(long questionId);
}
