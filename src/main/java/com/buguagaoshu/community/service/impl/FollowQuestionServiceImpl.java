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
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.buguagaoshu.community.model.table.FollowQuestionTableDef.FOLLOW_QUESTION;

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
    @Transactional
    public int insertFollowQuestion(FollowQuestionDTO followQuestionDTO, Question question, long userId) {
        FollowQuestion followQuestion = new FollowQuestion();
        followQuestion.setFollowQuestionId(question.getQuestionId());
        followQuestion.setFollowQuestionAuthor(question.getUserId());
        followQuestion.setFollowQuestionTitle(question.getTitle());
        followQuestion.setUserId(userId);
        followQuestion.setCreateTime(System.currentTimeMillis());
        FollowQuestion temp = findFollowQuestion(followQuestion.getFollowQuestionId(), userId);
        if (temp == null) {
            try {
                followQuestionMapper.insertSelective(followQuestion);
            } catch (DuplicateKeyException e) {
                // 并发重复关注：唯一索引兜底，不再累加计数
                return 1;
            }
            question.setFollowCount(1);
            questionService.updateFollowCount(question);
            return 1;
        } else {
            int deleted = followQuestionMapper.deleteById(temp.getId());
            if (deleted == 1) {
                question.setFollowCount(-1);
                questionService.updateFollowCount(question);
            }
            return 2;
        }
    }

    @Override
    public boolean isFollowQuestion(FollowQuestion followQuestion) {
        return findFollowQuestion(followQuestion.getFollowQuestionId(), followQuestion.getUserId()) != null;
    }

    private FollowQuestion findFollowQuestion(long followQuestionId, long userId) {
        return followQuestionMapper.selectOneByQuery(QueryWrapper.create()
                .where(FOLLOW_QUESTION.FOLLOW_QUESTION_ID.eq(followQuestionId))
                .and(FOLLOW_QUESTION.USER_ID.eq(userId)));
    }

    @Override
    public PaginationDto<FollowQuestion> selectUserFollowQuestion(long userId, String page, String size) {
        QueryWrapper query = QueryWrapper.create().where(FOLLOW_QUESTION.USER_ID.eq(userId));
        long allCount = followQuestionMapper.selectCountByQuery(query);
        long[] param = NumberUtils.getPageAndSize(page, size, allCount);
        List<FollowQuestion> followQuestions = followQuestionMapper.selectListByQuery(
                query.orderBy(FOLLOW_QUESTION.ID.desc()).limit(param[0], param[1]));
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
