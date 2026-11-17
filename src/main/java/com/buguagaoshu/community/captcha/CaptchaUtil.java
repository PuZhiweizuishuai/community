package com.buguagaoshu.community.captcha;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Locale;

/**
 * 验证码工具，保持与旧版 {@code com.wf.captcha.utils.CaptchaUtil} 完全一致的调用契约：
 * out() 输出图片、ver() 校验、clear() 清除，因此 Controller / Service 无需改动业务逻辑。
 *
 * <p>验证码文本以小写形式存放在 HttpSession（由 Spring Session 持久化到 Redis），
 * 校验忽略大小写，5 分钟过期；会话中只放 String / Long，兼容 JSON 序列化。
 */
public final class CaptchaUtil {

    public static final String SESSION_KEY = "captcha";
    public static final String SESSION_TIME_KEY = "captcha:createTime";

    /** 验证码有效期：5 分钟 */
    private static final long VALIDITY_MILLIS = 5 * 60 * 1000L;

    private CaptchaUtil() {
    }

    /**
     * 生成验证码并写入响应，同时把答案保存到会话。
     */
    public static void out(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ImageCaptcha captcha = ImageCaptcha.generate();
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_KEY, captcha.text().toLowerCase(Locale.ROOT));
        session.setAttribute(SESSION_TIME_KEY, System.currentTimeMillis());

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
        response.setContentLength(captcha.png().length);
        response.getOutputStream().write(captcha.png());
        response.flushBuffer();
    }

    /**
     * 校验用户输入的验证码，忽略大小写；无论成败校验后立即清除，杜绝同一验证码复用。
     */
    public static boolean ver(String code, HttpServletRequest request) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_KEY);
        Object created = session.getAttribute(SESSION_TIME_KEY);
        clear(request);
        if (!(expected instanceof String expectedCode)) {
            return false;
        }
        if (created instanceof Long createTime
                && System.currentTimeMillis() - createTime > VALIDITY_MILLIS) {
            return false;
        }
        return code.trim().toLowerCase(Locale.ROOT).equals(expectedCode);
    }

    /**
     * 清除会话中的验证码，防止重复使用。
     */
    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
            session.removeAttribute(SESSION_TIME_KEY);
        }
    }
}
