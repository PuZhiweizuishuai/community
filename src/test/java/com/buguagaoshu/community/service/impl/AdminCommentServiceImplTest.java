package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.enums.CommentTypeEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.SiteCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCommentServiceImplTest {

    private CommentMapper commentMapper;
    private QuestionMapper questionMapper;
    private NotificationService notificationService;
    private SiteCacheService siteCache;
    private AdminCommentServiceImpl service;

    private final User admin = admin();

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        questionMapper = mock(QuestionMapper.class);
        notificationService = mock(NotificationService.class);
        siteCache = mock(SiteCacheService.class);
        service = new AdminCommentServiceImpl(commentMapper, questionMapper,
                mock(UserBatchLoader.class), notificationService, siteCache);
    }

    @Test
    void deleteApprovedRootCommentDecrementsQuestionCount() {
        Comment comment = comment(1L, CommentTypeEnum.QUESTION.getType(), 10L, -1L,
                ReviewStatusEnum.APPROVED.getStatus());
        when(commentMapper.selectOneById(1L)).thenReturn(comment);
        when(commentMapper.alterStatus(1L, 0, 1)).thenReturn(1);

        service.delete(1L, admin);

        ArgumentCaptor<Question> update = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).updateQuestionCommentCount(update.capture());
        assertEquals(10L, update.getValue().getQuestionId());
        assertEquals(-1L, update.getValue().getCommentCount());
        verify(commentMapper, never()).updateCommentCount(any(Comment.class));
        verify(notificationService).createSystemNotification(eq(5L), eq(99L), eq(10L), eq(1L),
                eq(NotificationTypeEnum.ADMIN_DELETE_COMMENT.getType()), anyString());
        verify(siteCache).invalidate(SiteCacheService.HOME);
    }

    @Test
    void deleteApprovedReplyAlsoDecrementsRootCommentCount() {
        Comment comment = comment(2L, CommentTypeEnum.COMMENT.getType(), 10L, 1L,
                ReviewStatusEnum.APPROVED.getStatus());
        comment.setParentId(3L);
        when(commentMapper.selectOneById(2L)).thenReturn(comment);
        when(commentMapper.alterStatus(2L, 0, 1)).thenReturn(1);

        service.delete(2L, admin);

        verify(questionMapper).updateQuestionCommentCount(any(Question.class));
        ArgumentCaptor<Comment> update = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateCommentCount(update.capture());
        assertEquals(1L, update.getValue().getCommentId());
        assertEquals(-1L, update.getValue().getCommentCount());
    }

    @Test
    void deletePendingCommentDoesNotTouchCounts() {
        Comment comment = comment(3L, CommentTypeEnum.QUESTION.getType(), 10L, -1L,
                ReviewStatusEnum.PENDING.getStatus());
        when(commentMapper.selectOneById(3L)).thenReturn(comment);
        when(commentMapper.alterStatus(3L, 0, 1)).thenReturn(1);

        service.delete(3L, admin);

        verify(questionMapper, never()).updateQuestionCommentCount(any(Question.class));
        verify(commentMapper, never()).updateCommentCount(any(Comment.class));
    }

    @Test
    void deleteAlreadyDeletedCommentIsRejected() {
        Comment comment = comment(4L, CommentTypeEnum.QUESTION.getType(), 10L, -1L,
                ReviewStatusEnum.APPROVED.getStatus());
        comment.setStatus(0);
        when(commentMapper.selectOneById(4L)).thenReturn(comment);

        CustomizeException exception = assertThrows(CustomizeException.class, () -> service.delete(4L, admin));

        assertEquals(409, exception.getCode());
        verify(commentMapper, never()).alterStatus(anyLong(), anyInt(), anyInt());
        verify(questionMapper, never()).updateQuestionCommentCount(any(Question.class));
    }

    @Test
    void recoverApprovedCommentIncrementsCounts() {
        Comment comment = comment(5L, CommentTypeEnum.QUESTION.getType(), 10L, -1L,
                ReviewStatusEnum.APPROVED.getStatus());
        comment.setStatus(0);
        when(commentMapper.selectOneById(5L)).thenReturn(comment);
        when(commentMapper.alterStatus(5L, 1, 0)).thenReturn(1);

        service.recover(5L, admin);

        ArgumentCaptor<Question> update = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).updateQuestionCommentCount(update.capture());
        assertEquals(1L, update.getValue().getCommentCount());
        verify(notificationService).createSystemNotification(eq(5L), eq(99L), eq(10L), eq(5L),
                eq(NotificationTypeEnum.ADMIN_RECOVER_COMMENT.getType()), anyString());
    }

    private static Comment comment(long id, int type, long questionId, long parentCommentId, int reviewStatus) {
        Comment comment = new Comment();
        comment.setCommentId(id);
        comment.setQuestionId(questionId);
        comment.setType(type);
        comment.setParentId(questionId);
        comment.setParentCommentId(parentCommentId);
        comment.setCommentator(5L);
        comment.setContent("测试评论");
        comment.setStatus(1);
        comment.setReviewStatus(reviewStatus);
        comment.setCreateTime(1L);
        comment.setModifiedTime(1L);
        return comment;
    }

    private static User admin() {
        User user = new User();
        user.setId(99L);
        user.setUserId("admin");
        return user;
    }
}
