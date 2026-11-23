package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.enums.TopicJoinTypeEnum;
import com.buguagaoshu.community.enums.TopicMemberStatusEnum;
import com.buguagaoshu.community.enums.TopicVisibilityEnum;
import com.buguagaoshu.community.mapper.TopicMemberMapper;
import com.buguagaoshu.community.mapper.TopicModeratorMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.TopicMember;
import com.buguagaoshu.community.model.TopicModerator;
import com.buguagaoshu.community.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicAccessServiceImplTest {

    private TagClassCache tagClassCache;
    private TopicMemberMapper topicMemberMapper;
    private TopicModeratorMapper topicModeratorMapper;
    private TopicAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        tagClassCache = mock(TagClassCache.class);
        topicMemberMapper = mock(TopicMemberMapper.class);
        topicModeratorMapper = mock(TopicModeratorMapper.class);
        service = new TopicAccessServiceImpl(tagClassCache, topicMemberMapper, topicModeratorMapper);
    }

    @Test
    void publicTopicVisibleToAnonymous() {
        when(tagClassCache.getById(1L)).thenReturn(topic(1, TopicVisibilityEnum.PUBLIC.getCode(), 1, 1, null));

        TopicAccessDto access = service.resolve(1L, null);

        assertTrue(access.visible());
        assertTrue(access.canViewContent());
        assertFalse(access.isMember());
        assertFalse(service.canPostInTopic(1L, null));
    }

    @Test
    void privateApplyAnonymousSeesInfoButNotContent() {
        when(tagClassCache.getById(2L)).thenReturn(topic(2, TopicVisibilityEnum.PRIVATE_APPLY.getCode(), 1, 1, null));

        TopicAccessDto access = service.resolve(2L, null);

        assertTrue(access.visible());
        assertFalse(access.canViewContent());
        assertTrue("none".equals(access.joinStatus()));
    }

    @Test
    void privateApplyMemberCanViewContent() {
        when(tagClassCache.getById(3L)).thenReturn(topic(3, TopicVisibilityEnum.PRIVATE_APPLY.getCode(), 1, 1, null));
        when(topicMemberMapper.selectOne(3L, 7L)).thenReturn(member(3, 7,
                TopicMemberStatusEnum.APPROVED.getCode(), TopicJoinTypeEnum.APPLY.getCode()));

        TopicAccessDto access = service.resolve(3L, user(7));

        assertTrue(access.visible());
        assertTrue(access.canViewContent());
        assertTrue(access.isMember());
        assertTrue(service.canPostInTopic(3L, user(7)));
    }

    @Test
    void privateHiddenNonMemberInvisible() {
        when(tagClassCache.getById(4L)).thenReturn(topic(4, TopicVisibilityEnum.PRIVATE_HIDDEN.getCode(), 1, 1, null));

        TopicAccessDto access = service.resolve(4L, user(7));

        assertFalse(access.visible());
        assertFalse(access.canViewContent());
    }

    @Test
    void privateHiddenInvitedUserSeesInfoToAccept() {
        when(tagClassCache.getById(5L)).thenReturn(topic(5, TopicVisibilityEnum.PRIVATE_HIDDEN.getCode(), 1, 1, null));
        when(topicMemberMapper.selectOne(5L, 7L)).thenReturn(member(5, 7,
                TopicMemberStatusEnum.PENDING.getCode(), TopicJoinTypeEnum.INVITE.getCode()));

        TopicAccessDto access = service.resolve(5L, user(7));

        assertTrue(access.visible());
        assertFalse(access.canViewContent());
        assertTrue("invited".equals(access.joinStatus()));
    }

    @Test
    void pendingTopicOnlyVisibleToCreatorAndAdminPath() {
        when(tagClassCache.getById(6L)).thenReturn(topic(6, TopicVisibilityEnum.PUBLIC.getCode(), 0, 0, 7L));

        assertFalse(service.resolve(6L, null).visible());
        assertTrue(service.resolve(6L, user(7)).visible());
        assertFalse(service.canPostInTopic(6L, user(7)));
    }

    @Test
    void ownerCanViewAndPostInPrivateTopic() {
        when(tagClassCache.getById(7L)).thenReturn(topic(7, TopicVisibilityEnum.PRIVATE_HIDDEN.getCode(), 1, 1, 9L));
        TopicModerator owner = new TopicModerator();
        owner.setTopicId(7);
        owner.setUserId(9);
        owner.setRole(TopicModerator.ROLE_OWNER);
        when(topicModeratorMapper.selectOne(7L, 9L)).thenReturn(owner);

        TopicAccessDto access = service.resolve(7L, user(9));

        assertTrue(access.visible());
        assertTrue(access.canViewContent());
        assertTrue(access.isOwner());
        assertTrue(service.canPostInTopic(7L, user(9)));
    }

    private TagClass topic(long id, int visibility, int reviewStatus, int status, Long createdBy) {
        TagClass topic = new TagClass();
        topic.setId(id);
        topic.setVisibility(visibility);
        topic.setReviewStatus(reviewStatus);
        topic.setStatus(status);
        topic.setCreatedBy(createdBy);
        return topic;
    }

    private TopicMember member(long topicId, long userId, int status, String joinType) {
        TopicMember member = new TopicMember();
        member.setTopicId(topicId);
        member.setUserId(userId);
        member.setStatus(status);
        member.setJoinType(joinType);
        return member;
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
