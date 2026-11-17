package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.QuestionContentDto;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionContentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionContentServiceImplTest {

    private QuestionContentMapper questionContentMapper;
    private QuestionMapper questionMapper;
    private CommentMapper commentMapper;
    private RateLimitService rateLimitService;
    private QuestionContentServiceImpl service;

    @BeforeEach
    void setUp() {
        questionContentMapper = mock(QuestionContentMapper.class);
        questionMapper = mock(QuestionMapper.class);
        commentMapper = mock(CommentMapper.class);
        rateLimitService = mock(RateLimitService.class);
        service = new QuestionContentServiceImpl(questionContentMapper, questionMapper,
                commentMapper, rateLimitService);

        Question question = new Question();
        question.setQuestionId(1);
        question.setUserId(1);
        when(questionMapper.selectQuestionById(1L, 1)).thenReturn(question);
        when(questionContentMapper.selectByQuestionId(1L)).thenReturn(List.of(
                content(100, 0, 0, "公开内容", null),
                content(101, 1, 1, "密码内容", "secret"),
                content(102, 2, 2, "回复内容", null)));
    }

    private QuestionContent content(long id, int sort, int type, String text, String password) {
        QuestionContent content = new QuestionContent();
        content.setId(id);
        content.setQuestionId(1);
        content.setSort(sort);
        content.setType(type);
        content.setContent(text);
        content.setPassword(password);
        return content;
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void anonymousSeesOnlyNormalSegment() {
        List<QuestionContentDto> contents = service.visibleContents(1, 1, null, false);
        assertEquals("公开内容", contents.get(0).getContent());
        assertFalse(contents.get(0).isLocked());
        assertTrue(contents.get(1).isLocked());
        assertNull(contents.get(1).getContent());
        assertTrue(contents.get(2).isLocked());
        assertNull(contents.get(2).getContent());
    }

    @Test
    void ownerSeesEverythingWithoutPasswordOrReply() {
        List<QuestionContentDto> contents = service.visibleContents(1, 1, user(1), false);
        assertTrue(contents.stream().noneMatch(QuestionContentDto::isLocked));
        assertEquals("密码内容", contents.get(1).getContent());
        assertEquals("回复内容", contents.get(2).getContent());
        verify(commentMapper, never()).selectCountByQuery(any());
    }

    @Test
    void adminSeesEverything() {
        List<QuestionContentDto> contents = service.visibleContents(1, 1, null, true);
        assertTrue(contents.stream().noneMatch(QuestionContentDto::isLocked));
    }

    @Test
    void repliedUserSeesReplySegmentButNotPasswordSegment() {
        when(commentMapper.selectCountByQuery(any())).thenReturn(1L);
        List<QuestionContentDto> contents = service.visibleContents(1, 1, user(2), false);
        assertTrue(contents.get(1).isLocked());
        assertFalse(contents.get(2).isLocked());
        assertEquals("回复内容", contents.get(2).getContent());
    }

    @Test
    void strangerWhoNeverRepliedCannotSeeReplySegment() {
        when(commentMapper.selectCountByQuery(any())).thenReturn(0L);
        List<QuestionContentDto> contents = service.visibleContents(1, 1, user(2), false);
        assertTrue(contents.get(2).isLocked());
        assertNull(contents.get(2).getContent());
    }

    @Test
    void unlockWithCorrectPasswordReturnsContent() {
        when(questionContentMapper.selectOneById(101L)).thenReturn(content(101, 1, 1, "密码内容", "secret"));
        String text = service.unlock(1, 101, "secret", user(2), "127.0.0.1");
        assertEquals("密码内容", text);
        verify(rateLimitService).reset(anyString());
    }

    @Test
    void unlockWithWrongPasswordRecordsFailure() {
        when(questionContentMapper.selectOneById(101L)).thenReturn(content(101, 1, 1, "密码内容", "secret"));
        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.unlock(1, 101, "wrong", user(2), "127.0.0.1"));
        assertEquals(CustomizeErrorCode.CONTENT_PASSWORD_ERROR.getCode(), e.getCode());
        verify(rateLimitService).recordFailure(anyString());
    }

    @Test
    void unlockLockedOutRejected() {
        when(questionContentMapper.selectOneById(101L)).thenReturn(content(101, 1, 1, "密码内容", "secret"));
        when(rateLimitService.isLocked(anyString())).thenReturn(true);
        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.unlock(1, 101, "secret", user(2), "127.0.0.1"));
        assertEquals(CustomizeErrorCode.TOO_MANY_ATTEMPTS.getCode(), e.getCode());
    }

    @Test
    void ownerUnlocksWithoutPassword() {
        when(questionContentMapper.selectOneById(101L)).thenReturn(content(101, 1, 1, "密码内容", "secret"));
        assertEquals("密码内容", service.unlock(1, 101, null, user(1), "127.0.0.1"));
        verify(rateLimitService, never()).recordFailure(anyString());
    }

    @Test
    void unlockRejectsSegmentOfAnotherQuestion() {
        QuestionContent other = content(101, 1, 1, "密码内容", "secret");
        other.setQuestionId(2);
        when(questionContentMapper.selectOneById(101L)).thenReturn(other);
        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.unlock(1, 101, "secret", user(2), "127.0.0.1"));
        assertEquals(CustomizeErrorCode.CONTENT_NOT_FOUND.getCode(), e.getCode());
    }

    @Test
    void unlockRejectsNonPasswordSegment() {
        when(questionContentMapper.selectOneById(100L)).thenReturn(content(100, 0, 0, "公开内容", null));
        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.unlock(1, 100, "secret", user(2), "127.0.0.1"));
        assertEquals(CustomizeErrorCode.CONTENT_NOT_FOUND.getCode(), e.getCode());
    }

    @Test
    void replaceContentsReordersAndClearsUnusedPasswords() {
        QuestionContent normal = content(0, 9, 0, "a", "leftover");
        QuestionContent password = content(0, 9, 1, "b", "secret");
        service.replaceContents(1, List.of(normal, password));
        assertEquals(1, normal.getQuestionId());
        assertEquals(0, normal.getSort());
        assertNull(normal.getPassword());
        assertEquals(1, password.getSort());
        assertEquals("secret", password.getPassword());
        verify(questionContentMapper).deleteByQuestionId(1);
        verify(questionContentMapper).insertBatch(eq(List.of(normal, password)));
    }
}