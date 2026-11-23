package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.mapper.UserLoginHistoryMapper;
import com.buguagaoshu.community.model.UserLoginHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserLoginHistoryServiceImplTest {

    @Test
    void recordFailureDoesNotBreakLogin() {
        UserLoginHistoryMapper mapper = mock(UserLoginHistoryMapper.class);
        when(mapper.insertSelective(any())).thenThrow(new RuntimeException("db down"));
        UserLoginHistoryServiceImpl service = new UserLoginHistoryServiceImpl(mapper);

        assertDoesNotThrow(() -> service.record(7, "127.0.0.1",
                "Mozilla/5.0", UserLoginHistory.TYPE_WEB));
    }
}
