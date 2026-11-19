package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Notification;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private NotificationMapper notificationMapper;
    private QuestionMapper questionMapper;
    private CommentMapper commentMapper;
    private UserBatchLoader userBatchLoader;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        questionMapper = mock(QuestionMapper.class);
        commentMapper = mock(CommentMapper.class);
        userBatchLoader = mock(UserBatchLoader.class);
        service = new NotificationServiceImpl(notificationMapper, questionMapper, commentMapper, userBatchLoader);
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusChangeNotifiesOwnerAndCommentersOnce() {
        Question question = new Question();
        question.setQuestionId(10);
        question.setUserId(1);
        // 评论者：1 既是作者也是评论者，2 普通评论者，3 操作者本人
        when(commentMapper.selectDistinctCommentators(10L)).thenReturn(List.of(1L, 2L, 3L));

        service.notifyQuestionStatusChange(question, 3,
                NotificationTypeEnum.SYSTEM_DELETE_QUESTION, NotificationTypeEnum.SYSTEM_DELETE_COMMENT);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationMapper).insertBatch(captor.capture());
        List<Notification> batch = captor.getValue();
        assertEquals(2, batch.size());
        assertTrue(batch.stream().anyMatch(n -> n.getReceiver() == 1
                && n.getType() == NotificationTypeEnum.SYSTEM_DELETE_QUESTION.getType()));
        assertTrue(batch.stream().anyMatch(n -> n.getReceiver() == 2
                && n.getType() == NotificationTypeEnum.SYSTEM_DELETE_COMMENT.getType()));
        // 操作者不收通知
        assertTrue(batch.stream().noneMatch(n -> n.getReceiver() == 3));
    }

    @Test
    void deletedContentShowsPlaceholderInsteadOfNpe() {
        // B-5 回归：通知指向的问题已被物理删除
        Notification notification = new Notification();
        notification.setId(1);
        notification.setType(NotificationTypeEnum.LIKE_QUESTION.getType());
        notification.setNotifier(8);
        notification.setReceiver(5);
        notification.setOuterId(99);
        notification.setCreateTime(System.currentTimeMillis());
        when(notificationMapper.getAllLikeCount(5)).thenReturn(1L);
        when(notificationMapper.getAllLikeNotification(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(notification));
        User notifier = new User();
        notifier.setId(8);
        notifier.setUserId("liker");
        notifier.setUserName("Liker");
        when(userBatchLoader.loadUsers(any())).thenReturn(Map.of(8L, notifier));
        when(questionMapper.selectListByIds(any())).thenReturn(List.of());

        PaginationDto<NotificationDTO> page = service.getAllNotification("1", "10", 5, "3");

        assertEquals("内容已删除", page.getData().get(0).getOuterTitle());
        assertEquals("Liker", page.getData().get(0).getNotifierName());
    }

    private Comment comment(long commentator) {
        Comment comment = new Comment();
        comment.setCommentator(commentator);
        return comment;
    }
}