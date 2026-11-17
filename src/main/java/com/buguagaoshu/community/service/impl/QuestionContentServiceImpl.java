package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.QuestionContentDto;
import com.buguagaoshu.community.enums.QuestionContentTypeEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionContentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionContentService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.buguagaoshu.community.model.table.CommentTableDef.COMMENT;

@Service
public class QuestionContentServiceImpl implements QuestionContentService {

    private final QuestionContentMapper questionContentMapper;
    private final QuestionMapper questionMapper;
    private final CommentMapper commentMapper;
    private final RateLimitService rateLimitService;

    public QuestionContentServiceImpl(QuestionContentMapper questionContentMapper,
                                      QuestionMapper questionMapper,
                                      CommentMapper commentMapper,
                                      RateLimitService rateLimitService) {
        this.questionContentMapper = questionContentMapper;
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.rateLimitService = rateLimitService;
    }

    @Override
    @Transactional
    public void replaceContents(long questionId, List<QuestionContent> contents) {
        for (int i = 0; i < contents.size(); i++) {
            QuestionContent content = contents.get(i);
            content.setQuestionId(questionId);
            content.setSort(i);
            if (content.getType() != QuestionContentTypeEnum.PASSWORD.getType()) {
                content.setPassword(null);
            }
        }
        questionContentMapper.deleteByQuestionId(questionId);
        questionContentMapper.insertBatch(contents);
    }

    @Override
    public List<QuestionContent> listByQuestionId(long questionId) {
        return questionContentMapper.selectByQuestionId(questionId);
    }

    @Override
    public List<QuestionContentDto> visibleContents(long questionId, long ownerId, User viewer, boolean isAdmin) {
        List<QuestionContent> contents = listByQuestionId(questionId);
        boolean fullAccess = isAdmin || (viewer != null && viewer.getId() == ownerId);
        boolean replied = false;
        if (!fullAccess && viewer != null
                && contents.stream().anyMatch(c -> c.getType() == QuestionContentTypeEnum.REPLY.getType())) {
            replied = hasCommented(questionId, viewer.getId());
        }

        List<QuestionContentDto> result = new ArrayList<>(contents.size());
        for (QuestionContent content : contents) {
            QuestionContentDto dto = new QuestionContentDto();
            dto.setContentId(content.getId());
            dto.setSort(content.getSort());
            dto.setType(content.getType());
            boolean locked = !fullAccess
                    && (content.getType() == QuestionContentTypeEnum.PASSWORD.getType()
                    || (content.getType() == QuestionContentTypeEnum.REPLY.getType() && !replied));
            dto.setLocked(locked);
            dto.setContent(locked ? null : content.getContent());
            result.add(dto);
        }
        return result;
    }

    @Override
    public String unlock(long questionId, long contentId, String password, User viewer, String ip) {
        Question question = questionMapper.selectQuestionById(questionId, 1);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        // 未公开的问题只有作者与管理员可解锁正文
        if (question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()
                && (viewer == null || viewer.getId() != question.getUserId())) {
            throw new CustomizeException(CustomizeErrorCode.CONTENT_UNDER_REVIEW);
        }
        QuestionContent content = questionContentMapper.selectOneById(contentId);
        if (content == null || content.getQuestionId() != questionId
                || content.getType() != QuestionContentTypeEnum.PASSWORD.getType()) {
            throw new CustomizeException(CustomizeErrorCode.CONTENT_NOT_FOUND);
        }
        if (viewer != null && viewer.getId() == question.getUserId()) {
            return content.getContent();
        }
        String limitKey = "contentUnlock:" + contentId + ":" + ip;
        if (rateLimitService.isLocked(limitKey)) {
            throw new CustomizeException(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        if (password == null || !password.equals(content.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            throw new CustomizeException(CustomizeErrorCode.CONTENT_PASSWORD_ERROR);
        }
        rateLimitService.reset(limitKey);
        return content.getContent();
    }

    /** 回复可见判定：对该问题发表过任意层级、且已通过审核的评论即算已回复 */
    private boolean hasCommented(long questionId, long userId) {
        return commentMapper.selectCountByQuery(QueryWrapper.create()
                .where(COMMENT.QUESTION_ID.eq(questionId))
                .and(COMMENT.COMMENTATOR.eq(userId))
                .and(COMMENT.STATUS.eq(1))
                .and(COMMENT.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()))) > 0;
    }
}