package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.enums.QuestionVisibilityEnum;
import com.buguagaoshu.community.enums.TopicVisibilityEnum;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.TopicAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionAccessServiceImplTest {

    private TopicAccessService topicAccessService;
    private QuestionAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        topicAccessService = mock(TopicAccessService.class);
        service = new QuestionAccessServiceImpl(topicAccessService);
    }

    @Test
    void loginRequiredPostAnonymousGetsLoginPrompt() {
        when(topicAccessService.resolve(1L, null)).thenReturn(topicAccess(1, TopicVisibilityEnum.PUBLIC.getCode(), true));

        QuestionAccessDto access = service.resolve(question(1, 1, QuestionVisibilityEnum.LOGIN_REQUIRED.getCode()), null, null);

        assertTrue(access.visible());
        assertFalse(access.canViewContent());
        assertTrue(access.loginRequired());
    }

    @Test
    void loginRequiredPostLoggedInCanView() {
        User user = user(7);
        when(topicAccessService.resolve(1L, user)).thenReturn(topicAccess(1, TopicVisibilityEnum.PUBLIC.getCode(), true));

        QuestionAccessDto access = service.resolve(question(1, 1, QuestionVisibilityEnum.LOGIN_REQUIRED.getCode()), user, null);

        assertTrue(access.canViewContent());
        assertFalse(access.loginRequired());
    }

    @Test
    void privateTopicNonMemberContentLocked() {
        User user = user(7);
        when(topicAccessService.resolve(1L, user)).thenReturn(topicAccess(1, TopicVisibilityEnum.PRIVATE_APPLY.getCode(), false));

        QuestionAccessDto access = service.resolve(question(1, 1, QuestionVisibilityEnum.PUBLIC.getCode()), user, null);

        assertTrue(access.visible());
        assertFalse(access.canViewContent());
        assertFalse(access.loginRequired());
    }

    @Test
    void authorCanViewOwnPostInPrivateTopic() {
        User user = user(7);
        when(topicAccessService.resolve(1L, user)).thenReturn(topicAccess(1, TopicVisibilityEnum.PRIVATE_HIDDEN.getCode(), false));

        QuestionAccessDto access = service.resolve(question(1, 7, QuestionVisibilityEnum.PUBLIC.getCode()), user, null);

        assertTrue(access.visible());
        assertTrue(access.canViewContent());
    }

    @Test
    void hiddenTopicNonMemberNotVisible() {
        User user = user(7);
        when(topicAccessService.resolve(1L, user)).thenReturn(new TopicAccessDto(null, false, false, false,
                false, false, false, false, "none", null));

        QuestionAccessDto access = service.resolve(question(1, 1, QuestionVisibilityEnum.PUBLIC.getCode()), user, null);

        assertFalse(access.visible());
        assertFalse(access.canViewContent());
    }

    private Question question(long id, long userId, int visibility) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setUserId(userId);
        question.setTopicId(1L);
        question.setVisibility(visibility);
        return question;
    }

    private TopicAccessDto topicAccess(long topicId, int visibility, boolean canViewContent) {
        TagClass topic = new TagClass();
        topic.setId(topicId);
        topic.setVisibility(visibility);
        topic.setReviewStatus(1);
        topic.setStatus(1);
        return new TopicAccessDto(topic, true, true, canViewContent,
                canViewContent, false, false, false,
                canViewContent ? "joined" : "none", null);
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
