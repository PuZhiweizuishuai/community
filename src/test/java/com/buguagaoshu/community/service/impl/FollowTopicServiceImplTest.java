package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.FollowTopicDTO;
import com.buguagaoshu.community.mapper.FollowTopicMapper;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.TagClassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowTopicServiceImplTest {

    private FollowTopicMapper followTopicMapper;
    private TagClassService tagClassService;
    private FollowTopicServiceImpl service;
    private TagClass tagClass;

    @BeforeEach
    void setUp() {
        followTopicMapper = mock(FollowTopicMapper.class);
        tagClassService = mock(TagClassService.class);
        service = new FollowTopicServiceImpl(followTopicMapper, tagClassService);
        tagClass = new TagClass();
        tagClass.setId(3);
        tagClass.setTitle("Java");
        tagClass.setFollowCount(10);
    }

    private FollowTopicDTO dto() {
        FollowTopicDTO dto = new FollowTopicDTO();
        dto.setTopicId(3L);
        dto.setTopicTitle("Java");
        return dto;
    }

    @Test
    void followIncrementsCount() {
        when(followTopicMapper.selectOneByQuery(any())).thenReturn(null);
        int result = service.insertFollowTop(dto(), 7, tagClass);
        assertEquals(1, result);
        ArgumentCaptor<TagClass> captor = ArgumentCaptor.forClass(TagClass.class);
        verify(tagClassService).updateFollowCount(captor.capture());
        assertEquals(1, captor.getValue().getFollowCount());
    }

    @Test
    void unfollowDecrementsCount() {
        FollowTopic existing = new FollowTopic();
        existing.setFollowTopicId(99);
        when(followTopicMapper.selectOneByQuery(any())).thenReturn(existing);
        when(followTopicMapper.deleteById(99L)).thenReturn(1);

        int result = service.insertFollowTop(dto(), 7, tagClass);

        assertEquals(2, result);
        ArgumentCaptor<TagClass> captor = ArgumentCaptor.forClass(TagClass.class);
        verify(tagClassService).updateFollowCount(captor.capture());
        // B-2 回归：取消关注必须是 -1 而不是 +1
        assertEquals(-1, captor.getValue().getFollowCount());
    }

    @Test
    void concurrentDuplicateFollowDoesNotIncrement() {
        when(followTopicMapper.selectOneByQuery(any())).thenReturn(null);
        when(followTopicMapper.insertSelective(any())).thenThrow(new DuplicateKeyException("uk_follow_topic"));
        int result = service.insertFollowTop(dto(), 7, tagClass);
        assertEquals(1, result);
        verify(tagClassService, never()).updateFollowCount(any());
    }

    @Test
    void concurrentUnfollowDoesNotDoubleDecrement() {
        FollowTopic existing = new FollowTopic();
        existing.setFollowTopicId(99);
        when(followTopicMapper.selectOneByQuery(any())).thenReturn(existing);
        when(followTopicMapper.deleteById(anyLong())).thenReturn(0);
        service.insertFollowTop(dto(), 7, tagClass);
        verify(tagClassService, never()).updateFollowCount(any());
    }
}