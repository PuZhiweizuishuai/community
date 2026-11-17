package com.buguagaoshu.community.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailTokenServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private EmailTokenService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new EmailTokenService(redis);
    }

    @Test
    void resetTokenIsStoredWithTtlAndConsumedOnce() {
        String token = service.createResetToken(7);
        verify(ops).set(eq("mail:pwdreset:" + token), eq("7"), any(Duration.class));

        when(ops.get("mail:pwdreset:" + token)).thenReturn("7");
        assertEquals(7L, service.consumeResetToken(token));
        verify(redis).delete("mail:pwdreset:" + token);
    }

    @Test
    void expiredResetTokenRejected() {
        when(ops.get(anyString())).thenReturn(null);
        assertNull(service.consumeResetToken("gone"));
        assertNull(service.consumeResetToken(null));
    }

    @Test
    void registerCodeVerifiedAndDestroyed() {
        when(ops.get("mail:regcode:a@b.com")).thenReturn("123456");
        assertTrue(service.verifyRegisterCode("a@b.com", "123456"));
        verify(redis).delete("mail:regcode:a@b.com");
        // 第二次校验时已被销毁
        when(ops.get("mail:regcode:a@b.com")).thenReturn(null);
        assertFalse(service.verifyRegisterCode("a@b.com", "123456"));
    }

    @Test
    void wrongRegisterCodeRejected() {
        when(ops.get("mail:regcode:a@b.com")).thenReturn("123456");
        assertFalse(service.verifyRegisterCode("a@b.com", "000000"));
        assertFalse(service.verifyRegisterCode("a@b.com", null));
    }
}