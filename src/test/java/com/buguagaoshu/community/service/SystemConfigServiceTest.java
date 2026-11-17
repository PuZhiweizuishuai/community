package com.buguagaoshu.community.service;

import com.buguagaoshu.community.mapper.SystemConfigMapper;
import com.buguagaoshu.community.model.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemConfigServiceTest {

    private SystemConfigMapper mapper;
    private SystemConfigService service;

    @BeforeEach
    void setUp() {
        mapper = mock(SystemConfigMapper.class);
        service = new SystemConfigService(mapper);
    }

    @Test
    void chatRoomIsEnabledByDefault() {
        when(mapper.selectAll()).thenReturn(List.of());

        assertTrue(service.isChatEnabled());
    }

    @Test
    void chatRoomCanBeDisabledExplicitly() {
        when(mapper.selectAll()).thenReturn(List.of(
                new SystemConfig(SystemConfigService.CHAT_ENABLED, "false", 1L)));

        assertFalse(service.isChatEnabled());
    }

    @Test
    void saveUsesUpsertForNewConfigKeys() {
        service.save(Map.of(SystemConfigService.CHAT_ENABLED, "false"));

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(mapper).upsert(captor.capture());
        assertEquals(SystemConfigService.CHAT_ENABLED, captor.getValue().getConfigKey());
        assertEquals("false", captor.getValue().getConfigValue());
    }
}
