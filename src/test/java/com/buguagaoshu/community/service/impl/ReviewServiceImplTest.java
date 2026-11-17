package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.ContentReviewLogMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.ContentReviewLog;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionContentService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TagClassService;
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

class ReviewServiceImplTest {

    private QuestionMapper questionMapper;
    private CommentMapper commentMapper;
    private ContentReviewLogMapper reviewLogMapper;
    private CommentService commentService;
    private TagClassService tagClassService;
    private NotificationService notificationService;
    private SiteCacheService siteCache;
    private ReviewServiceImpl service;

    private final User admin = user(99);
    private final User author = user(7);

    @BeforeEach
    void setUp() {
        questionMapper = mock(QuestionMapper.class);
        commentMapper = mock(CommentMapper.class);
        reviewLogMapper = mock(ContentReviewLogMapper.class);
        commentService = mock(CommentService.class);
        tagClassService = mock(TagClassService.class);
        notificationService = mock(NotificationService.class);
        siteCache = mock(SiteCacheService.class);
        service = new ReviewServiceImpl(questionMapper, commentMapper, reviewLogMapper, commentService,
                tagClassService, notificationService, mock(QuestionContentService.class),
                mock(UserBatchLoader.class), siteCache);
    }

    @Test
    void approvingQuestionPublishesContentAndNotifiesAuthor() {
        Question question = question(10, 7, "Java");
        when(questionMapper.selectOneById(10L)).thenReturn(question);
        when(questionMapper.approveReview(eq(10L), eq(99L), anyLong())).thenReturn(1);

        service.approve("question", 10, admin);

        // 审核通过后才计入话题讨论数，并通知作者
        verify(tagClassService).updateTalkCount("Java", 1);
        verify(notificationService).createSystemNotification(eq(7L), eq(99L), eq(10L), eq(-1L),
                eq(NotificationTypeEnum.REVIEW_QUESTION_PASS.getType()), anyString());
        verify(reviewLogMapper).insertSelective(any(ContentReviewLog.class));
        verify(siteCache).invalidate(SiteCacheService.HOME);
    }

    @Test
    void approvingHandledQuestionIsRejectedWithoutSideEffects() {
        when(questionMapper.selectOneById(10L)).thenReturn(question(10, 7, "Java"));
        // 并发审核时只有第一个请求能更新成功
        when(questionMapper.approveReview(eq(10L), eq(99L), anyLong())).thenReturn(0);

        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.approve("question", 10, admin));

        assertEquals(CustomizeErrorCode.REVIEW_ALREADY_HANDLED.getCode(), e.getCode());
        verify(tagClassService, never()).updateTalkCount(anyString(), anyInt());
        verify(notificationService, never()).createSystemNotification(anyLong(), anyLong(), anyLong(), anyLong(),
                anyInt(), anyString());
    }

    @Test
    void rejectingWithoutReasonIsRejected() {
        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.reject("question", 10, "   ", "请补充说明", admin));

        assertEquals(CustomizeErrorCode.REVIEW_REASON_REQUIRED.getCode(), e.getCode());
        verify(questionMapper, never()).rejectReview(anyLong(), anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void rejectingQuestionStoresReasonAndNotifiesAuthor() {
        Question question = question(10, 7, "Java");
        when(questionMapper.selectOneById(10L)).thenReturn(question);
        when(questionMapper.rejectReview(eq(10L), eq("内容不符合规范"), eq("请补充来源"),
                eq(99L), anyLong())).thenReturn(1);

        service.reject("question", 10, "内容不符合规范", "请补充来源", admin);

        verify(notificationService).createSystemNotification(eq(7L), eq(99L), eq(10L), eq(-1L),
                eq(NotificationTypeEnum.REVIEW_QUESTION_REJECT.getType()),
                org.mockito.ArgumentMatchers.contains("内容不符合规范"));
        ArgumentCaptor<ContentReviewLog> log = ArgumentCaptor.forClass(ContentReviewLog.class);
        verify(reviewLogMapper).insertSelective(log.capture());
        assertEquals("question", log.getValue().getTargetType());
        assertEquals("内容不符合规范", log.getValue().getReason());
    }

    @Test
    void approvingCommentRunsPublishSideEffects() {
        Comment comment = comment(20, 7, 0);
        when(commentMapper.selectOneById(20L)).thenReturn(comment);
        when(commentMapper.approveReview(eq(20L), eq(99L), anyLong())).thenReturn(1);

        service.approve("comment", 20, admin);

        // 评论计数与回复通知统一由 publishApprovedComment 执行
        verify(commentService).publishApprovedComment(comment);
        verify(notificationService).createSystemNotification(eq(7L), eq(99L), eq(10L), eq(20L),
                eq(NotificationTypeEnum.REVIEW_COMMENT_PASS.getType()), anyString());
        verify(siteCache).invalidate(SiteCacheService.HOME);
    }

    @Test
    void resubmittingCommentRequiresOwner() {
        Comment comment = comment(20, 7, 2);
        when(commentMapper.selectOneById(20L)).thenReturn(comment);

        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.resubmitComment(20, "修改后的内容", user(8)));

        assertEquals(CustomizeErrorCode.FORBIDDEN.getCode(), e.getCode());
        verify(commentMapper, never()).resubmitReview(anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    void resubmittingRejectedCommentReentersReview() {
        Comment comment = comment(20, 7, 2);
        when(commentMapper.selectOneById(20L)).thenReturn(comment);
        when(commentMapper.resubmitReview(eq(20L), eq(7L), eq("修改后的内容"), anyLong())).thenReturn(1);

        service.resubmitComment(20, " 修改后的内容 ", author);

        verify(notificationService).createSystemNotification(eq(7L), eq(7L), eq(10L), eq(20L),
                eq(NotificationTypeEnum.REVIEW_COMMENT_PENDING.getType()), anyString());
        verify(reviewLogMapper).insertSelective(any(ContentReviewLog.class));
    }

    @Test
    void approvingUnpublishedCommentIsBlockedWhenAlreadyHandled() {
        when(commentMapper.selectOneById(20L)).thenReturn(comment(20, 7, 0));
        when(commentMapper.approveReview(eq(20L), eq(99L), anyLong())).thenReturn(0);

        assertThrows(CustomizeException.class, () -> service.approve("comment", 20, admin));
        verify(commentService, never()).publishApprovedComment(any(Comment.class));
    }

    private static Question question(long id, long userId, String tag) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setUserId(userId);
        question.setTag(tag);
        question.setTitle("待审核问题");
        question.setStatus(1);
        question.setReviewStatus(0);
        question.setCreateTime(1L);
        return question;
    }

    private static Comment comment(long id, long commentator, int reviewStatus) {
        Comment comment = new Comment();
        comment.setCommentId(id);
        comment.setQuestionId(10L);
        comment.setCommentator(commentator);
        comment.setContent("待审核评论");
        comment.setStatus(1);
        comment.setReviewStatus(reviewStatus);
        comment.setCreateTime(1L);
        comment.setModifiedTime(1L);
        return comment;
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUserName("用户" + id);
        return user;
    }
}
