package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.FollowQuestionDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.FollowQuestion;
import com.buguagaoshu.community.model.Question;
import io.jsonwebtoken.Claims;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 14:53
 */
public interface FollowQuestionService {
    /**
     * 插入关注问题数据
     * @param followQuestionDTO 关注问题
     * @param question 问题
     * @param claims 用户信息
     * @return 结果
     * */
    int insertFollowQuestion(FollowQuestionDTO followQuestionDTO, Question question, Claims claims);

    /**
     * 判断是否关注该问题
     * @param followQuestion 关注问题
     * @return 关注 true， 没关注 false
     * */
    boolean isFollowQuestion(FollowQuestion followQuestion);

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页显示数目
     * @return 结果
     * */
    PaginationDto<FollowQuestion> selectUserFollowQuestion(long userId, String page, String size);

}
