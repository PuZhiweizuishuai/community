package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.TagClassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionServiceImplTest {

    private QuestionMapper questionMapper;
    private TagClassService tagClassService;
    private NotificationService notificationService;
    private UserBatchLoader userBatchLoader;
    private QuestionServiceImpl service;
    private Question question;
    private User owner;
    private User stranger;
    private User admin;

    @BeforeEach
    void setUp() {
        questionMapper = mock(QuestionMapper.class);
        tagClassService = mock(TagClassService.class);
        ClassificationCache classificationCache = mock(ClassificationCache.class);
        notificationService = mock(NotificationService.class);
        userBatchLoader = mock(UserBatchLoader.class);
        service = new QuestionServiceImpl(questionMapper, tagClassService, classificationCache,
                notificationService, userBatchLoader);

        question = new Question();
        question.setQuestionId(10);
        question.setUserId(1);
        question.setStatus(1);
        question.setTag("Java");
        when(questionMapper.selectOneById(10L)).thenReturn(question);

        owner = user(1);
        stranger = user(2);
        admin = user(99);
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void ownerCanDeleteOwnQuestion() {
        // B-1 回归：自删分支不再对 null 管理员解引用
        service.deleteQuestion(10, owner, false);
        verify(notificationService).notifyQuestionStatusChange(eq(question), eq(1L),
                eq(NotificationTypeEnum.SYSTEM_DELETE_QUESTION), eq(NotificationTypeEnum.SYSTEM_DELETE_COMMENT));
        verify(tagClassService).updateTalkCount("Java", -1);
        verify(questionMapper).alterStatus(10, 0);
    }

    @Test
    void adminCanDeleteOthersQuestion() {
        service.deleteQuestion(10, admin, true);
        verify(questionMapper).alterStatus(10, 0);
    }

    @Test
    void strangerCannotDelete() {
        CustomizeException e = assertThrows(CustomizeException.class,
                () -> service.deleteQuestion(10, stranger, false));
        assertEquals(CustomizeErrorCode.FORBIDDEN.getCode(), e.getCode());
        verify(questionMapper, never()).alterStatus(anyLong(), eq(0));
    }

    @Test
    void deletingAlreadyDeletedRejected() {
        question.setStatus(0);
        assertThrows(CustomizeException.class, () -> service.deleteQuestion(10, owner, false));
        verify(tagClassService, never()).updateTalkCount(any(), eq(-1));
    }

    @Test
    void recoveryRestoresStatusAndCount() {
        question.setStatus(0);
        service.recoveryQuestion(10, admin);
        verify(tagClassService).updateTalkCount("Java", 1);
        verify(questionMapper).alterStatus(10, 1);
    }

    @Test
    void recoveringActiveQuestionRejected() {
        assertThrows(CustomizeException.class, () -> service.recoveryQuestion(10, admin));
        verify(questionMapper, never()).alterStatus(anyLong(), eq(1));
    }

    @Test
    void cursorPageTrimsExtraRowAndBuildsNextCursor() {
        // 取 size+1 条判断是否还有下一页
        List<Question> fetched = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Question q = new Question();
            q.setQuestionId(100 - i);
            q.setUserId(1);
            q.setAlterTime(1000 - i);
            q.setCreateTime(1000 - i);
            fetched.add(q);
        }
        when(questionMapper.getQuestionsByAlterTimeCursor(eq(2000L), eq(500L), isNull(), isNull(), eq(16L), eq(1)))
                .thenReturn(fetched);
        when(questionMapper.selectCountByQuery(any())).thenReturn(100L);
        when(userBatchLoader.loadUsers(any())).thenReturn(Map.of());

        PaginationDto<QuestionDto> page = service.getSomeQuestionDto("1", "15", null, 3, 0, "2000:500");

        assertEquals(15, page.getData().size());
        Question last = fetched.get(14);
        assertEquals(last.getAlterTime() + ":" + last.getQuestionId(), page.getNextCursor());
        assertEquals(100, page.getAllCount());
    }

    @Test
    void cursorPageLastPageHasNoNextCursor() {
        Question q = new Question();
        q.setQuestionId(5);
        q.setUserId(1);
        q.setAlterTime(100);
        q.setCreateTime(100);
        when(questionMapper.getQuestionsByAlterTimeCursor(anyLong(), anyLong(), isNull(), isNull(), anyLong(), eq(1)))
                .thenReturn(List.of(q));
        when(questionMapper.selectCountByQuery(any())).thenReturn(1L);
        when(userBatchLoader.loadUsers(any())).thenReturn(Map.of());

        PaginationDto<QuestionDto> page = service.getSomeQuestionDto("1", "15", null, 3, 0, "2000:500");

        assertNull(page.getNextCursor());
    }

    @Test
    void invalidCursorRejected() {
        assertThrows(CustomizeException.class,
                () -> service.getSomeQuestionDto("1", "15", null, 3, 0, "not-a-cursor"));
    }
}