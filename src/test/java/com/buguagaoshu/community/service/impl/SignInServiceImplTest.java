package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.JwtUtil;
import com.buguagaoshu.community.util.StringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignInServiceImplTest {

    private UserService userService;
    private UserPermissionService userPermissionService;
    private OnlineUserService onlineUserService;
    private RateLimitService rateLimitService;
    private SignInServiceImpl signInService;
    private MockedStatic<CaptchaUtil> captchaMock;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userPermissionService = mock(UserPermissionService.class);
        onlineUserService = mock(OnlineUserService.class);
        rateLimitService = mock(RateLimitService.class);
        JwtUtil jwtUtil = new JwtUtil();
        jwtUtil.setKey("test-secret-key-0123456789-0123456789-abcdef");
        signInService = new SignInServiceImpl(userPermissionService, userService,
                onlineUserService, jwtUtil, rateLimitService);
        ReflectionTestUtils.setField(signInService, "defaultTtlMillis", 7_200_000L);
        captchaMock = mockStatic(CaptchaUtil.class);
        captchaMock.when(() -> CaptchaUtil.ver(anyString(), any())).thenReturn(true);
        request = new MockHttpServletRequest();

        User user = new User();
        user.setId(7);
        user.setUserId("alice");
        user.setUserName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword(StringUtil.BCryptPassword("secret123"));
        when(userService.selectUserByEmail("alice@example.com")).thenReturn(user);
        when(userPermissionService.selectUserPermissionById(7))
                .thenReturn(new UserPermission(7, 1, "0", "2026-01-01 00:00:00", 0));
    }

    @AfterEach
    void tearDown() {
        captchaMock.close();
    }

    @Test
    void rememberMeGetsSevenDayToken() {
        HashMap<String, Object> result = signInService.signIn(
                "alice@example.com", "secret123", "remember-me", "abcd", request);
        assertEquals(604800, result.get("time"));
        ArgumentCaptor<OnlineUser> captor = ArgumentCaptor.forClass(OnlineUser.class);
        verify(onlineUserService).insertOnlineUser(captor.capture());
        assertEquals(7 * 24 * 3600 * 1000L, captor.getValue().getExpireTime() - captor.getValue().getTime());
    }

    @Test
    void normalLoginGetsTwoHourTokenAndSessionCookie() {
        HashMap<String, Object> result = signInService.signIn(
                "alice@example.com", "secret123", null, "abcd", request);
        assertEquals(-1, result.get("time"));
        ArgumentCaptor<OnlineUser> captor = ArgumentCaptor.forClass(OnlineUser.class);
        verify(onlineUserService).insertOnlineUser(captor.capture());
        assertEquals(7_200_000L, captor.getValue().getExpireTime() - captor.getValue().getTime());
    }

    @Test
    void wrongPasswordRecordsFailure() {
        assertThrows(CustomizeException.class, () -> signInService.signIn(
                "alice@example.com", "wrong", null, "abcd", request));
        verify(rateLimitService).recordFailure("login:alice@example.com");
        verify(onlineUserService, never()).insertOnlineUser(any());
    }

    @Test
    void lockedAccountRejected() {
        when(rateLimitService.isLocked("login:alice@example.com")).thenReturn(true);
        assertThrows(CustomizeException.class, () -> signInService.signIn(
                "alice@example.com", "secret123", null, "abcd", request));
        verify(onlineUserService, never()).insertOnlineUser(any());
    }

    @Test
    void wrongCaptchaRejected() {
        captchaMock.when(() -> CaptchaUtil.ver(anyString(), any())).thenReturn(false);
        assertThrows(CustomizeException.class, () -> signInService.signIn(
                "alice@example.com", "secret123", null, "bad", request));
    }
}