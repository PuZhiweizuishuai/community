package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.ChatMessage;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.MailService;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台系统配置：注册开关、SMTP 邮件配置。
 * SMTP 密码只写不读：查询接口仅返回是否已设置，保存时留空表示沿用原密码。
 */
@RestController
@RequestMapping("/api/admin/config")
public class SystemConfigAdminApiController {

    public record SaveRequest(Boolean registerEnabled, Boolean chatEnabled, Boolean reviewEnabled,
                              Boolean mailEnabled, String mailHost,
                              Integer mailPort, String mailUsername, String mailPassword, String mailFrom,
                              Boolean mailSsl) {
    }

    public record TestMailRequest(String to) {
    }

    private final SystemConfigService configService;
    private final MailService mailService;
    private final RateLimitService rateLimitService;
    private final SimpMessageSendingOperations messagingTemplate;

    public SystemConfigAdminApiController(SystemConfigService configService, MailService mailService,
                                          RateLimitService rateLimitService,
                                          SimpMessageSendingOperations messagingTemplate) {
        this.configService = configService;
        this.mailService = mailService;
        this.rateLimitService = rateLimitService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public ResultDTO<Map<String, Object>> get(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        SystemConfigService.MailSettings mail = configService.mailSettings();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("registerEnabled", configService.isRegisterEnabled());
        data.put("chatEnabled", configService.isChatEnabled());
        data.put("reviewEnabled", configService.isReviewEnabled());
        data.put("mailEnabled", mail.enabled());
        data.put("mailHost", mail.host());
        data.put("mailPort", mail.port());
        data.put("mailUsername", mail.username());
        data.put("mailFrom", mail.from());
        data.put("mailSsl", mail.ssl());
        data.put("mailPasswordSet", StringUtils.isNotBlank(mail.password()));
        return ResultDTO.okOf(data);
    }

    @PostMapping("/save")
    public ResultDTO<Object> save(@RequestBody SaveRequest body, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        if (body == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        SystemConfigService.MailSettings current = configService.mailSettings();
        boolean wasChatEnabled = configService.isChatEnabled();
        boolean chatEnabled = body.chatEnabled() != null ? body.chatEnabled() : wasChatEnabled;
        boolean mailEnabled = body.mailEnabled() != null ? body.mailEnabled() : current.enabled();
        String host = body.mailHost() != null ? body.mailHost().trim() : current.host();
        int port = body.mailPort() != null ? body.mailPort() : current.port();
        if (port < 1 || port > 65535) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "SMTP 端口需为 1~65535");
        }
        if (mailEnabled && StringUtils.isBlank(host)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "启用邮件服务前请先填写 SMTP 服务器地址");
        }

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put(SystemConfigService.REGISTER_ENABLED,
                String.valueOf(body.registerEnabled() != null ? body.registerEnabled() : configService.isRegisterEnabled()));
        updates.put(SystemConfigService.CHAT_ENABLED, String.valueOf(chatEnabled));
        updates.put(SystemConfigService.REVIEW_ENABLED,
                String.valueOf(body.reviewEnabled() != null ? body.reviewEnabled() : configService.isReviewEnabled()));
        updates.put(SystemConfigService.MAIL_ENABLED, String.valueOf(mailEnabled));
        updates.put(SystemConfigService.MAIL_HOST, host);
        updates.put(SystemConfigService.MAIL_PORT, String.valueOf(port));
        updates.put(SystemConfigService.MAIL_USERNAME,
                body.mailUsername() != null ? body.mailUsername().trim() : current.username());
        updates.put(SystemConfigService.MAIL_FROM,
                body.mailFrom() != null ? body.mailFrom().trim() : current.from());
        updates.put(SystemConfigService.MAIL_SSL,
                String.valueOf(body.mailSsl() != null ? body.mailSsl() : current.ssl()));
        // 密码留空 = 不修改
        if (StringUtils.isNotBlank(body.mailPassword())) {
            updates.put(SystemConfigService.MAIL_PASSWORD, body.mailPassword());
        }
        configService.save(updates);
        // 从开启切换为关闭时，通知在线用户立即退出，避免继续留在聊天室
        if (wasChatEnabled && !chatEnabled) {
            ChatMessage closed = new ChatMessage();
            closed.setType(ChatMessage.MessageType.DISABLED);
            closed.setContent("聊天室已被管理员关闭");
            messagingTemplate.convertAndSend("/topic/public", closed);
        }
        return ResultDTO.okOf();
    }

    /** 用当前已保存的配置发送测试邮件，便于管理员验证 SMTP 是否可用 */
    @PostMapping("/test-mail")
    public ResultDTO<Object> testMail(@RequestBody TestMailRequest body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        if (body == null || StringUtils.isBlank(body.to()) || !StringUtil.checkEmail(body.to().trim())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请输入有效的收件邮箱");
        }
        if (!rateLimitService.tryAcquire("testmail:" + admin.getId(), 5, Duration.ofMinutes(10))) {
            throw new CustomizeException(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        mailService.send(body.to().trim(), "不挂高数社区 · 测试邮件",
                "这是一封来自管理后台「系统配置」的测试邮件。\n\n收到本邮件说明 SMTP 配置正确。\n");
        return ResultDTO.okOf();
    }
}