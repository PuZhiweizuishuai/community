package com.buguagaoshu.community.captcha;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaUtilTest {

    private MockHttpServletRequest requestWithCaptcha(String code, long createdAt) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(CaptchaUtil.SESSION_KEY, code);
        request.getSession().setAttribute(CaptchaUtil.SESSION_TIME_KEY, createdAt);
        return request;
    }

    @Test
    void verifyIsCaseInsensitiveAndClearsImmediately() {
        MockHttpServletRequest request = requestWithCaptcha("abcd", System.currentTimeMillis());
        assertTrue(CaptchaUtil.ver("ABCD", request));
        // 校验即清除：同一验证码不能二次使用（S-5 回归）
        assertNull(request.getSession().getAttribute(CaptchaUtil.SESSION_KEY));
        assertFalse(CaptchaUtil.ver("abcd", request));
    }

    @Test
    void wrongCodeAlsoClears() {
        MockHttpServletRequest request = requestWithCaptcha("abcd", System.currentTimeMillis());
        assertFalse(CaptchaUtil.ver("wrong", request));
        assertFalse(CaptchaUtil.ver("abcd", request));
    }

    @Test
    void expiredCaptchaRejected() {
        MockHttpServletRequest request = requestWithCaptcha("abcd", System.currentTimeMillis() - 6 * 60 * 1000L);
        assertFalse(CaptchaUtil.ver("abcd", request));
    }

    @Test
    void emptyOrNullCodeRejected() {
        MockHttpServletRequest request = requestWithCaptcha("abcd", System.currentTimeMillis());
        assertFalse(CaptchaUtil.ver(null, request));
        assertFalse(CaptchaUtil.ver("  ", request));
    }
}