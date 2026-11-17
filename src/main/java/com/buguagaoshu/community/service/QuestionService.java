package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.User;

import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:51
 */
public interface QuestionService {
    /**
     * 插入或编辑问题，并全量替换内容段落（同一事务）。
     * @param question 问题
     * @param contents 内容段落（1 ~ 3 个，按顺序）
     * @return 结果
     * */
    int createQuestion(Question question, List<QuestionContent> contents);

    /**
     * @param page 页码
     * @param size 每页显示数量
     * @param cursor 游标（非空时走游标分页，忽略 page）
     * 获取问题列表,并添加相应用户信息
     * @return 问题列表
     * */
    PaginationDto<QuestionDto> getSomeQuestionDto(String page, String size, String tag, Integer sort, Integer classification, String cursor);


    /**
     * 获取当前用户发布的问题列表
     * @param page 页码
     * @param size 每页显示数量
     * @param id 用户id
     * @return 问题列表
     * */
    PaginationDto<QuestionDto> getQuestionByUserId(String page, String size, long id);

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

    /**
     * 评论数加 1
     * @param question 问题
     * @return 评论数加 1
     * */
    int updateQuestionCommentCount(Question question);


    /**
     * 根据问题标签匹配相关问题
     * @param questionId 问题 id
     * @return 相关问题，问题不存在或已删除时返回空列表
     * */
    List<Question> getRelevantQuestion(String questionId);


    /**
     * 搜索问题
     * @param search 搜索关键字
     * @param page 页码
     * @param size 大小
     * @return 结果
     * */
    PaginationDto<QuestionDto> searchQuestion(String search, String page, String size);


    /**
     * 获取问题列表
     * */
    List<Question> getQuestionListForTag(long page, long size);


    long getAllQuestionCount();

    PaginationDto<QuestionDto> getAllQuestionList(String page, String size);


    PaginationDto<QuestionDto> searchAllQuestionList(String search ,String page, String size);


    /**
     * 带状态的计算该用户发布的问题数量
     * @param id 用户 id
     * @param status 状态
     * @return 返回问题总数
     */
    long getUserQuestionCount(long id, int status);


    /**
     * 更新问题关注数
     * @param question 问题
     * @return 结果
     * */
    int updateFollowCount(Question question);

    /**
     * 删除问题（作者自删或管理员删除）：通知作者与评论者、话题计数 -1、置为删除态，整体事务。
     * @param questionId 问题 id
     * @param operator 操作者（会话用户或管理员）
     * @param isAdmin 是否管理员操作
     */
    void deleteQuestion(long questionId, User operator, boolean isAdmin);

    /**
     * 恢复被删除的问题（仅管理员）：通知作者与评论者、话题计数 +1、恢复正常态，整体事务。
     */
    void recoveryQuestion(long questionId, User admin);
}
