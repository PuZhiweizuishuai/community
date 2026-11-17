package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.enums.CommentTypeEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Notification;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    private CommentMapper commentMapper;
    private QuestionMapper questionMapper;
    private NotificationMapper notificationMapper;
    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        questionMapper = mock(QuestionMapper.class);
        notificationMapper = mock(NotificationMapper.class);
        UserBatchLoader userBatchLoader = mock(UserBatchLoader.class);
        when(userBatchLoader.loadUsers(any(), anyBoolean())).thenReturn(Map.of());
        NotificationService notificationService = mock(NotificationService.class);
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.isReviewEnabled()).thenReturn(false);
        service = new CommentServiceImpl(commentMapper, questionMapper, notificationMapper,
                notificationService, userBatchLoader, systemConfigService);

        Question question = new Question();
        question.setQuestionId(10L);
        question.setUserId(2L);
        question.setStatus(1);
        when(questionMapper.selectQuestionById(10L, 1)).thenReturn(question);
        when(commentMapper.insertComment(any(Comment.class))).thenAnswer(invocation -> {
            invocation.<Comment>getArgument(0).setCommentId(100L);
            return 1;
        });
    }

    @Test
    void replyToSecondLevelCommentIsAttachedToRoot() {
        Comment root = comment(1L, CommentTypeEnum.QUESTION.getType(), 10L, -1L, 5L);
        Comment child = comment(2L, CommentTypeEnum.COMMENT.getType(), 1L, 1L, 6L);
        when(commentMapper.selectCommentByCommentId(2L, 1)).thenReturn(child);
        when(commentMapper.selectCommentByCommentId(1L, 1)).thenReturn(root);

        service.insertComment(newReply(2L, 7L));

        ArgumentCaptor<Comment> inserted = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insertComment(inserted.capture());
        // 回复二级评论时，parentCommentId 必须归一到根评论，否则该回复在前端不可见
        assertEquals(1L, inserted.getValue().getParentCommentId());
        assertEquals(2L, inserted.getValue().getParentId());

        ArgumentCaptor<Comment> countUpdate = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateCommentCount(countUpdate.capture());
        assertEquals(1L, countUpdate.getValue().getCommentId());
        assertEquals(1L, countUpdate.getValue().getCommentCount());

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insertSelective(notification.capture());
        assertEquals(6L, notification.getValue().getReceiver());
        assertEquals(NotificationTypeEnum.REPLY_COMMENT.getType(), notification.getValue().getType());
        assertEquals(2L, notification.getValue().getCommentId());
    }

    @Test
    void replyToRootIncrementsRootCount() {
        Comment root = comment(1L, CommentTypeEnum.QUESTION.getType(), 10L, -1L, 5L);
        when(commentMapper.selectCommentByCommentId(1L, 1)).thenReturn(root);

        service.insertComment(newReply(1L, 7L));

        ArgumentCaptor<Comment> inserted = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insertComment(inserted.capture());
        assertEquals(1L, inserted.getValue().getParentCommentId());
        assertEquals(1L, inserted.getValue().getParentId());

        ArgumentCaptor<Comment> countUpdate = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateCommentCount(countUpdate.capture());
        assertEquals(1L, countUpdate.getValue().getCommentId());
        assertEquals(1L, countUpdate.getValue().getCommentCount());
    }

    @Test
    void replyToCommentFromAnotherQuestionIsRejected() {
        Comment otherQuestionComment = comment(3L, CommentTypeEnum.COMMENT.getType(), 99L, 1L, 5L);
        when(commentMapper.selectCommentByCommentId(3L, 1)).thenReturn(otherQuestionComment);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> service.insertComment(newReply(3L, 7L)));

        assertEquals(CustomizeErrorCode.COMMENT_NOT_FOUND.getCode(), exception.getCode());
        verify(commentMapper, never()).insertComment(any(Comment.class));
    }

    @Test
    void emptyThreadReturnsEmptyList() {
        when(commentMapper.selectListByQuery(any())).thenReturn(List.of());

        assertTrue(service.getTwoLevelCommentByParent("1").isEmpty());
    }

    private static Comment comment(long id, int type, long parentId, long parentCommentId, long commentator) {
        Comment comment = new Comment();
        comment.setCommentId(id);
        comment.setQuestionId(10L);
        comment.setType(type);
        comment.setParentId(parentId);
        comment.setParentCommentId(parentCommentId);
        comment.setCommentator(commentator);
        comment.setCreateTime(1L);
        comment.setModifiedTime(1L);
        return comment;
    }

    private static Comment newReply(long targetId, long commentator) {
        Comment reply = new Comment();
        reply.setQuestionId(10L);
        reply.setParentId(targetId);
        reply.setType(CommentTypeEnum.COMMENT.getType());
        reply.setCommentator(commentator);
        reply.setContent("回复内容");
        reply.setCreateTime(2L);
        reply.setModifiedTime(2L);
        return reply;
    }
}
