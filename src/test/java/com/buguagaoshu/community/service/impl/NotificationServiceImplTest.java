package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.TagClassMapper;
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
    private TagClassMapper tagClassMapper;
    private UserBatchLoader userBatchLoader;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        questionMapper = mock(QuestionMapper.class);
        commentMapper = mock(CommentMapper.class);
        tagClassMapper = mock(TagClassMapper.class);
        userBatchLoader = mock(UserBatchLoader.class);
        service = new NotificationServiceImpl(notificationMapper, questionMapper, commentMapper,
                tagClassMapper, userBatchLoader);
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

    @Test
    void systemNotificationUsesSentenceAndSystemActor() {
        // 系统消息只展示一句完整的话，不再出现「系统 提交了问题审核」这种拼接
        Notification notification = new Notification();
        notification.setId(2);
        notification.setType(NotificationTypeEnum.REVIEW_QUESTION_PENDING.getType());
        notification.setNotifier(1);
        notification.setReceiver(5);
        notification.setOuterId(10);
        notification.setCreateTime(System.currentTimeMillis());
        notification.setCommentContent("你的问题已提交审核，通过后将公开展示：「如何入门 Java」");
        when(notificationMapper.getAllSystemCount(5)).thenReturn(1L);
        when(notificationMapper.getAllSystemNotification(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(notification));
        Question question = new Question();
        question.setQuestionId(10);
        question.setTitle("如何入门 Java");
        when(questionMapper.selectListByIds(any())).thenReturn(List.of(question));

        NotificationDTO dto = service.getAllNotification("1", "10", 5, "5").getData().get(0);

        assertEquals("system", dto.getActorType());
        assertEquals("你的问题已提交审核", dto.getTypeName());
        assertEquals("如何入门 Java", dto.getOuterTitle());
        assertEquals("通过后将公开展示", dto.getCommentContent());
    }

    @Test
    void replyNotificationKeepsAuthorName() {
        Notification notification = new Notification();
        notification.setId(3);
        notification.setType(NotificationTypeEnum.REPLY_QUESTION.getType());
        notification.setNotifier(8);
        notification.setReceiver(5);
        notification.setOuterId(10);
        notification.setCommentId(274);
        notification.setCreateTime(System.currentTimeMillis());
        notification.setCommentContent("我也想知道");
        when(notificationMapper.getAllCommentCount(5)).thenReturn(1L);
        when(notificationMapper.getAllCommentNotification(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(notification));
        User notifier = new User();
        notifier.setId(8);
        notifier.setUserId("replier");
        notifier.setUserName("Replier");
        notifier.setHeadUrl("/image/head/nohead.png");
        when(userBatchLoader.loadUsers(any())).thenReturn(Map.of(8L, notifier));
        Question question = new Question();
        question.setQuestionId(10);
        question.setTitle("如何入门 Java");
        when(questionMapper.selectListByIds(any())).thenReturn(List.of(question));

        NotificationDTO dto = service.getAllNotification("1", "10", 5, "0").getData().get(0);

        assertEquals("user", dto.getActorType());
        assertEquals("回复了你的问题", dto.getTypeName());
        assertEquals("Replier", dto.getNotifierName());
        // 列表要带上真实头像与评论 id，前端才能显示照片、点进主页、就地回复
        assertEquals("/image/head/nohead.png", dto.getNotifierAvatar());
        assertEquals("replier", dto.getNotifierId());
        assertEquals(274L, dto.getCommentId());
        assertEquals("我也想知道", dto.getCommentContent());
    }

    private Comment comment(long commentator) {
        Comment comment = new Comment();
        comment.setCommentator(commentator);
        return comment;
    }
}