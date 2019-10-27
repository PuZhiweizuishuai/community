package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.FollowQuestionDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.FollowQuestionMapper;
import com.buguagaoshu.community.model.FollowQuestion;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.FollowQuestionService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.DateFormatUtil;
import com.buguagaoshu.community.util.NumberUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 15:01
 */
@Service
public class FollowQuestionServiceImpl implements FollowQuestionService {
    private final FollowQuestionMapper followQuestionMapper;

    private final QuestionService questionService;

    @Autowired
    public FollowQuestionServiceImpl(FollowQuestionMapper followQuestionMapper, QuestionService questionService) {
        this.followQuestionMapper = followQuestionMapper;
        this.questionService = questionService;
    }

    @Override
    public int insertFollowQuestion(FollowQuestionDTO followQuestionDTO, Question question, Claims claims) {
        FollowQuestion followQuestion = new FollowQuestion();
        followQuestion.setFollowQuestionId(question.getQuestionId());
        followQuestion.setFollowQuestionAuthor(question.getUserId());
        followQuestion.setFollowQuestionTitle(question.getTitle());
        followQuestion.setUserId(Long.parseLong(claims.getId()));
        followQuestion.setCreateTime(System.currentTimeMillis());
        FollowQuestion temp = followQuestionMapper.selectFollowQuestion(followQuestion);
        if (temp == null) {
            question.setFollowCount(1);
            questionService.updateFollowCount(question);
            followQuestionMapper.insertFollowQuestion(followQuestion);
            return 1;
        } else {
            question.setFollowCount(-1);
            questionService.updateFollowCount(question);
            followQuestionMapper.deleteFollowQuestion(temp.getId());
            return 2;
        }
    }

    @Override
    public boolean isFollowQuestion(FollowQuestion followQuestion) {
        FollowQuestion temp = followQuestionMapper.selectFollowQuestion(followQuestion);
        return temp != null;
    }

    @Override
    public PaginationDto<FollowQuestion> selectUserFollowQuestion(long userId, String page, String size) {
        long allCount = followQuestionMapper.selectUserFollowQuestionCount(userId);
        long[] param = NumberUtils.getPageAndSize(page, size, allCount);
        List<FollowQuestion> followQuestions = followQuestionMapper.selectUserFollowQuestion(userId, param[0], param[1]);
        for (FollowQuestion followQuestion : followQuestions) {
            followQuestion.setTime(DateFormatUtil.getTimeDifference(followQuestion.getCreateTime(), System.currentTimeMillis()));
        }
        PaginationDto<FollowQuestion> followQuestionPaginationDto = new PaginationDto<>();
        followQuestionPaginationDto.setData(followQuestions);
        followQuestionPaginationDto.setPagination(param[2], param[3], param[1]);
        followQuestionPaginationDto.setAllCount(allCount);
        return followQuestionPaginationDto;
    }
}
