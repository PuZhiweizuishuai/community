package com.buguagaoshu.community.service;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 邮件发送。SMTP 参数来自后台「系统配置」（system_configs 表），
 * 未启用或未填写服务器地址时 isEnabled() 返回 false，调用方应给出明确提示。
 */
@Service
@Slf4j
public class MailService {

    private final SystemConfigService configService;

    private volatile JavaMailSenderImpl cachedSender;
    private volatile String cachedSenderKey;

    public MailService(SystemConfigService configService) {
        this.configService = configService;
    }

    public boolean isEnabled() {
        SystemConfigService.MailSettings settings = configService.mailSettings();
        return settings.enabled() && StringUtils.isNotBlank(settings.host());
    }

    public void send(String to, String subject, String text) {
        SystemConfigService.MailSettings settings = configService.mailSettings();
        if (!settings.enabled() || StringUtils.isBlank(settings.host())) {
            throw new CustomizeException(CustomizeErrorCode.SYS_ERROR.getCode(), "邮件服务未启用，请联系管理员");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = StringUtils.isNotBlank(settings.from()) ? settings.from() : settings.username();
            if (StringUtils.isNotBlank(from)) {
                message.setFrom(from);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            sender(settings).send(message);
        } catch (Exception e) {
            log.error("邮件发送失败 to={}: {}", to, e.getMessage());
            throw new CustomizeException(CustomizeErrorCode.SYS_ERROR.getCode(), "邮件发送失败，请检查邮件配置或稍后重试");
        }
    }

    /** 配置不变时复用发送器；后台修改 SMTP 后按新配置重建 */
    private JavaMailSender sender(SystemConfigService.MailSettings settings) {
        String key = String.join("|", settings.host(), String.valueOf(settings.port()),
                settings.username(), settings.password(), String.valueOf(settings.ssl()));
        JavaMailSenderImpl local = cachedSender;
        if (local != null && key.equals(cachedSenderKey)) {
            return local;
        }
        synchronized (this) {
            if (cachedSender == null || !key.equals(cachedSenderKey)) {
                JavaMailSenderImpl impl = new JavaMailSenderImpl();
                impl.setHost(settings.host());
                impl.setPort(settings.port());
                impl.setUsername(settings.username());
                impl.setPassword(settings.password());
                impl.setDefaultEncoding("UTF-8");
                Properties props = impl.getJavaMailProperties();
                props.put("mail.smtp.auth", String.valueOf(StringUtils.isNotBlank(settings.username())));
                props.put("mail.smtp.connectiontimeout", "10000");
                props.put("mail.smtp.timeout", "10000");
                props.put("mail.smtp.writetimeout", "10000");
                if (settings.ssl()) {
                    props.put("mail.smtp.ssl.enable", "true");
                } else {
                    props.put("mail.smtp.starttls.enable", "true");
                }
                cachedSender = impl;
                cachedSenderKey = key;
            }
            return cachedSender;
        }
    }
}