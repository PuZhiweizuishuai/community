package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.EmailAuthDtos;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.EmailTokenService;
import com.buguagaoshu.community.service.MailService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * 邮件相关认证接口：找回密码、重置密码、注册邮箱验证码、能力开关。
 * 重置链接为 opaque token（Redis 存储，30 分钟过期，使用即销毁），重置成功吊销全部会话。
 */
@RestController
@RequestMapping("/api/auth")
public class EmailAuthApiController {

    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final MailService mailService;
    private final EmailTokenService emailTokenService;
    private final RateLimitService rateLimitService;
    private final SystemConfigService systemConfigService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public EmailAuthApiController(UserService userService, OnlineUserService onlineUserService,
                                  MailService mailService, EmailTokenService emailTokenService,
                                  RateLimitService rateLimitService, SystemConfigService systemConfigService) {
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.mailService = mailService;
        this.emailTokenService = emailTokenService;
        this.rateLimitService = rateLimitService;
        this.systemConfigService = systemConfigService;
    }

    /** 前端按此开关决定是否展示邮箱验证码 / 找回密码入口 / 注册表单 */
    @GetMapping("/features")
    public ResultDTO<Map<String, Object>> features() {
        return ResultDTO.okOf(Map.of(
                "mailEnabled", mailService.isEnabled(),
                "registerEnabled", systemConfigService.isRegisterEnabled()));
    }

    /**
     * 忘记密码：发送重置链接。无论邮箱是否存在都返回成功，避免账号枚举。
     */
    @PostMapping("/forgot-password")
    public ResultDTO<Object> forgotPassword(@Valid @RequestBody EmailAuthDtos.ForgotPasswordRequest body,
                                            HttpServletRequest request) {
        if (!CaptchaUtil.ver(body.getCaptcha(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        if (!mailService.isEnabled()) {
            return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR.getCode(),
                    "邮件服务未启用，无法自助找回密码，请联系管理员");
        }
        String email = body.getEmail().trim().toLowerCase();
        if (!rateLimitService.tryAcquire("forgot:" + email, 3, Duration.ofMinutes(15))) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        User user = userService.selectUserByEmail(email);
        if (user != null) {
            String token = emailTokenService.createResetToken(user.getId());
            String link = frontendBaseUrl + "/reset-password?token=" + token;
            mailService.send(email, "不挂高数社区 · 找回密码",
                    "你好 " + user.getUserName() + "：\n\n"
                            + "点击下面的链接重置密码（30 分钟内有效，仅可使用一次）：\n"
                            + link + "\n\n"
                            + "如果这不是你本人的操作，请忽略本邮件。\n");
        }
        return ResultDTO.okOf();
    }

    /**
     * 重置密码：消费 token，更新密码并吊销该用户全部「记住我」token。
     */
    @PostMapping("/reset-password")
    public ResultDTO<Object> resetPassword(@Valid @RequestBody EmailAuthDtos.ResetPasswordRequest body) {
        Long userId = emailTokenService.consumeResetToken(body.getToken());
        if (userId == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "重置链接无效或已过期，请重新申请");
        }
        User user = userService.selectUserById(userId);
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        userService.updateUserPasswordById(userId, body.getNewPassword());
        onlineUserService.deleteOnlineUserById(userId);
        return ResultDTO.okOf();
    }

    /**
     * 发送注册邮箱验证码（仅邮件服务启用时需要）。
     */
    @PostMapping("/send-register-code")
    public ResultDTO<Object> sendRegisterCode(@Valid @RequestBody EmailAuthDtos.SendRegisterCodeRequest body,
                                              HttpServletRequest request) {
        if (!systemConfigService.isRegisterEnabled()) {
            return ResultDTO.errorOf(CustomizeErrorCode.FORBIDDEN.getCode(), "站点已关闭新用户注册");
        }
        if (!CaptchaUtil.ver(body.getCaptcha(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        if (!mailService.isEnabled()) {
            return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR.getCode(), "邮件服务未启用");
        }
        String email = body.getEmail().trim().toLowerCase();
        if (userService.selectUserByEmail(email) != null) {
            return ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "该邮箱已被注册，请直接登录");
        }
        if (!rateLimitService.tryAcquire("regcode:" + email, 3, Duration.ofMinutes(10))) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        String code = emailTokenService.createRegisterCode(email);
        mailService.send(email, "不挂高数社区 · 注册验证码",
                "你的注册验证码是：" + code + "\n\n10 分钟内有效，请勿泄露给他人。\n");
        return ResultDTO.okOf();
    }
}