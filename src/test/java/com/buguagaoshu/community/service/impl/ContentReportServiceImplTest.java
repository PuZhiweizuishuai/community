package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.ContentReportCreateDto;
import com.buguagaoshu.community.dto.ContentReportHandleDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReportStatusEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.ContentReportMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.ContentReport;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminCommentService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentReportServiceImplTest {

    private ContentReportMapper reportMapper;
    private QuestionMapper questionMapper;
    private CommentMapper commentMapper;
    private NotificationService notificationService;
    private QuestionService questionService;
    private AdminCommentService adminCommentService;
    private ContentReportServiceImpl service;

    private final User reporter = user(7L);
    private final User admin = user(99L);

    @BeforeEach
    void setUp() {
        reportMapper = mock(ContentReportMapper.class);
        questionMapper = mock(QuestionMapper.class);
        commentMapper = mock(CommentMapper.class);
        notificationService = mock(NotificationService.class);
        questionService = mock(QuestionService.class);
        adminCommentService = mock(AdminCommentService.class);
        service = new ContentReportServiceImpl(reportMapper, questionMapper, commentMapper,
                mock(UserBatchLoader.class), notificationService, questionService, adminCommentService);
    }

    @Test
    void createCommentReportStoresQuestionAndSnapshot() {
        Comment comment = new Comment();
        comment.setCommentId(20L);
        comment.setQuestionId(10L);
        comment.setCommentator(5L);
        comment.setContent("违规评论");
        comment.setStatus(1);
        comment.setReviewStatus(ReviewStatusEnum.APPROVED.getStatus());
        when(commentMapper.selectOneById(20L)).thenReturn(comment);
        when(questionMapper.selectOneById(10L)).thenReturn(question(10L, 3L));
        when(reportMapper.selectCountByQuery(any())).thenReturn(0L);

        ContentReportCreateDto dto = new ContentReportCreateDto();
        dto.setTargetType("comment");
        dto.setTargetId(20L);
        dto.setReason("abuse");
        dto.setDescription("辱骂他人");

        service.create(dto, reporter);

        ArgumentCaptor<ContentReport> inserted = ArgumentCaptor.forClass(ContentReport.class);
        verify(reportMapper).insert(inserted.capture());
        ContentReport saved = inserted.getValue();
        assertEquals("comment", saved.getTargetType());
        assertEquals(10L, saved.getQuestionId());
        assertEquals(5L, saved.getTargetUserId());
        assertEquals("违规评论", saved.getTargetSnapshot());
        assertEquals(ReportStatusEnum.PENDING.getStatus(), saved.getStatus());
        assertEquals(7L, saved.getReporterId());
    }

    @Test
    void cannotReportOwnContent() {
        when(questionMapper.selectOneById(10L)).thenReturn(question(10L, reporter.getId()));

        ContentReportCreateDto dto = new ContentReportCreateDto();
        dto.setTargetType("question");
        dto.setTargetId(10L);
        dto.setReason("spam");

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> service.create(dto, reporter));
        assertEquals(400, exception.getCode());
        verify(reportMapper, never()).insert(any(ContentReport.class));
    }

    @Test
    void acceptingReportCanRemoveTargetQuestion() {
        ContentReport report = pendingReport("question", 10L, 10L);
        when(reportMapper.selectOneById(1L)).thenReturn(report);
        when(questionMapper.selectOneById(10L)).thenReturn(question(10L, 5L));
        when(reportMapper.handle(eq(1L), eq(1), eq(99L), any(), anyLong(), eq(1))).thenReturn(1);

        service.handle(1L, new ContentReportHandleDto(true, "确认违规", true), admin);

        verify(questionService).deleteQuestion(10L, admin, true);
        verify(notificationService).createSystemNotification(eq(7L), eq(99L), eq(10L), eq(-1L),
                eq(NotificationTypeEnum.REPORT_ACCEPTED.getType()), any(String.class));
    }

    @Test
    void rejectingReportRequiresNote() {
        ContentReport report = pendingReport("question", 10L, 10L);
        when(reportMapper.selectOneById(1L)).thenReturn(report);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> service.handle(1L, new ContentReportHandleDto(false, null, false), admin));

        assertEquals(400, exception.getCode());
        verify(reportMapper, never()).handle(anyLong(), anyInt(), anyLong(), any(), anyLong(), anyInt());
    }

    @Test
    void acceptingReportWithoutDeleteLeavesContent() {
        ContentReport report = pendingReport("question", 10L, 10L);
        when(reportMapper.selectOneById(1L)).thenReturn(report);
        when(reportMapper.handle(eq(1L), eq(1), eq(99L), any(), anyLong(), eq(0))).thenReturn(1);

        service.handle(1L, new ContentReportHandleDto(true, null, false), admin);

        verify(questionService, never()).deleteQuestion(anyLong(), any(User.class), anyBoolean());
        verify(notificationService).createSystemNotification(eq(7L), eq(99L), eq(10L), eq(-1L),
                eq(NotificationTypeEnum.REPORT_ACCEPTED.getType()), any(String.class));
    }

    private ContentReport pendingReport(String type, long targetId, long questionId) {
        ContentReport report = new ContentReport();
        report.setId(1L);
        report.setTargetType(type);
        report.setTargetId(targetId);
        report.setQuestionId(questionId);
        report.setTargetUserId(5L);
        report.setTargetSnapshot("被举报内容");
        report.setReporterId(7L);
        report.setReason("spam");
        report.setStatus(ReportStatusEnum.PENDING.getStatus());
        report.setCreateTime(System.currentTimeMillis());
        return report;
    }

    private Question question(long id, long userId) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setUserId(userId);
        question.setTitle("测试问题");
        question.setStatus(1);
        question.setReviewStatus(ReviewStatusEnum.APPROVED.getStatus());
        question.setCreateTime(1L);
        return question;
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUserId("user" + id);
        user.setUserName("用户" + id);
        return user;
    }
}
