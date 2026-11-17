package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.mapper.ClickLikeMapper;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.ClickLike;
import com.buguagaoshu.community.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClickLikeServiceImplTest {

    private ClickLikeMapper clickLikeMapper;
    private QuestionMapper questionMapper;
    private UserMapper userMapper;
    private NotificationMapper notificationMapper;
    private ClickLikeServiceImpl service;
    private Question question;

    @BeforeEach
    void setUp() {
        clickLikeMapper = mock(ClickLikeMapper.class);
        questionMapper = mock(QuestionMapper.class);
        CommentMapper commentMapper = mock(CommentMapper.class);
        notificationMapper = mock(NotificationMapper.class);
        userMapper = mock(UserMapper.class);
        service = new ClickLikeServiceImpl(clickLikeMapper, questionMapper, commentMapper,
                notificationMapper, userMapper);
        question = new Question();
        question.setQuestionId(10);
        question.setUserId(2);
        when(questionMapper.selectQuestionById(10, 1)).thenReturn(question);
    }

    private ClickLikeDTO dto() {
        ClickLikeDTO dto = new ClickLikeDTO();
        dto.setNotifier(7L);
        dto.setNotifierName("liker");
        dto.setQuestionId(10L);
        dto.setCommentId(-1L);
        return dto;
    }

    @Test
    void likeQuestionIncrementsCounts() {
        when(clickLikeMapper.selectOneByQuery(any())).thenReturn(null);
        ClickLikeTypeEnum result = service.createClickLike(dto());
        assertEquals(ClickLikeTypeEnum.SUCCESS, result);
        verify(questionMapper).updateQuestionLikeCount(any());
        verify(userMapper).updateUserLikeCount(1, 2);
    }

    @Test
    void duplicateLikeDoesNotIncrementCounts() {
        when(clickLikeMapper.selectOneByQuery(any())).thenReturn(null);
        when(clickLikeMapper.insertSelective(any())).thenThrow(new DuplicateKeyException("uk_like"));
        ClickLikeTypeEnum result = service.createClickLike(dto());
        assertEquals(ClickLikeTypeEnum.SUCCESS, result);
        verify(questionMapper, never()).updateQuestionLikeCount(any());
        verify(userMapper, never()).updateUserLikeCount(anyLong(), anyLong());
    }

    @Test
    void cancelLikeDecrementsCounts() {
        ClickLike old = new ClickLike();
        old.setLikeId(55);
        old.setReceiver(2);
        old.setNotificationId(88);
        when(clickLikeMapper.selectOneByQuery(any())).thenReturn(old);
        when(clickLikeMapper.deleteById(55L)).thenReturn(1);

        ClickLikeTypeEnum result = service.createClickLike(dto());

        assertEquals(ClickLikeTypeEnum.SUCCESS_CANCEL, result);
        verify(userMapper).updateUserLikeCount(-1, 2);
        verify(notificationMapper).deleteById(88L);
    }

    @Test
    void concurrentCancelDoesNotDoubleDecrement() {
        ClickLike old = new ClickLike();
        old.setLikeId(55);
        old.setReceiver(2);
        when(clickLikeMapper.selectOneByQuery(any())).thenReturn(old);
        when(clickLikeMapper.deleteById(55L)).thenReturn(0);

        ClickLikeTypeEnum result = service.createClickLike(dto());

        assertEquals(ClickLikeTypeEnum.SUCCESS_CANCEL, result);
        verify(userMapper, never()).updateUserLikeCount(anyLong(), anyLong());
        verify(questionMapper, never()).updateQuestionLikeCount(any());
    }

    @Test
    void selfLikeRejected() {
        question.setUserId(7);
        assertEquals(ClickLikeTypeEnum.CLICK_LIKE_IS_NOT_USER, service.createClickLike(dto()));
    }

    @Test
    void missingQuestionRejected() {
        when(questionMapper.selectQuestionById(10, 1)).thenReturn(null);
        assertEquals(ClickLikeTypeEnum.LIKE_QUESTION_NOT_FOUND, service.createClickLike(dto()));
    }
}