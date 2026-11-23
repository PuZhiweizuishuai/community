package com.buguagaoshu.community.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        jwtUtil.setKey("test-secret-key-0123456789-0123456789-abcdef");
    }

    @Test
    void ttlIsPerCallNotSharedState() {
        long now = System.currentTimeMillis();
        String shortToken = jwtUtil.createJWT(1, 7_200_000L);
        String longToken = jwtUtil.createJWT(2, 604_800_000L);

        Claims shortClaims = jwtUtil.parseJWT(shortToken);
        Claims longClaims = jwtUtil.parseJWT(longToken);
        // S-3 回归：两次签发的 TTL 互不影响
        assertTrue(shortClaims.getExpiration().getTime() - now <= 7_200_000L + 5000);
        assertTrue(longClaims.getExpiration().getTime() - now > 600_000_000L);
        assertEquals("1", shortClaims.getId());
        assertEquals("2", longClaims.getId());
    }

    @Test
    void expiredTokenRejected() throws InterruptedException {
        String token = jwtUtil.createJWT(1, 1);
        Thread.sleep(1100);
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parseJWT(token));
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwtUtil.createJWT(1, 60_000);
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(Exception.class, () -> jwtUtil.parseJWT(tampered));
    }
}