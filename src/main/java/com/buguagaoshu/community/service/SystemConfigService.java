package com.buguagaoshu.community.service;

import com.buguagaoshu.community.mapper.SystemConfigMapper;
import com.buguagaoshu.community.model.SystemConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统配置（注册开关、SMTP 邮件等）读写入口。
 * 读走 30 秒 TTL 内存缓存（与 AdminPermissionChecker 同策略，多实例部署时
 * 其它节点最迟 30 秒感知变更）；保存后立即刷新本机缓存。
 */
@Service
@Slf4j
public class SystemConfigService {

    public static final String REGISTER_ENABLED = "register.enabled";
    public static final String CHAT_ENABLED = "chat.enabled";
    /** 内容审核开关：开启后问题与评论需管理员审核通过才公开展示 */
    public static final String REVIEW_ENABLED = "review.enabled";
    public static final String MAIL_ENABLED = "mail.enabled";
    public static final String MAIL_HOST = "mail.host";
    public static final String MAIL_PORT = "mail.port";
    public static final String MAIL_USERNAME = "mail.username";
    public static final String MAIL_PASSWORD = "mail.password";
    public static final String MAIL_FROM = "mail.from";
    public static final String MAIL_SSL = "mail.ssl";
    /** 一次性导入 yml 邮件配置的标记，避免管理员清空配置后被反复回填 */
    private static final String MAIL_IMPORTED = "mail.imported";

    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final SystemConfigMapper mapper;

    @Value("${spring.mail.host:}")
    private String ymlMailHost;
    @Value("${spring.mail.port:465}")
    private int ymlMailPort;
    @Value("${spring.mail.username:}")
    private String ymlMailUsername;
    @Value("${spring.mail.password:}")
    private String ymlMailPassword;
    @Value("${app.mail.enabled:false}")
    private boolean ymlMailEnabled;
    @Value("${app.mail.from:}")
    private String ymlMailFrom;

    private record CachedValues(Map<String, String> values, long expireAt) {
    }

    private volatile CachedValues cached;

    public SystemConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    public record MailSettings(boolean enabled, String host, int port,
                               String username, String password, String from, boolean ssl) {
    }

    public boolean isRegisterEnabled() {
        return !"false".equalsIgnoreCase(get(REGISTER_ENABLED));
    }

    /** 聊天室默认开启；只有显式保存为 false 时才关闭 */
    public boolean isChatEnabled() {
        return !"false".equalsIgnoreCase(get(CHAT_ENABLED));
    }

    /** 内容审核默认关闭；只有显式保存为 true 时才开启 */
    public boolean isReviewEnabled() {
        return "true".equalsIgnoreCase(get(REVIEW_ENABLED));
    }

    public MailSettings mailSettings() {
        Map<String, String> values = values();
        int port;
        try {
            port = Integer.parseInt(values.getOrDefault(MAIL_PORT, "465").trim());
        } catch (NumberFormatException e) {
            port = 465;
        }
        return new MailSettings(
                "true".equalsIgnoreCase(values.getOrDefault(MAIL_ENABLED, "false")),
                values.getOrDefault(MAIL_HOST, ""),
                port,
                values.getOrDefault(MAIL_USERNAME, ""),
                values.getOrDefault(MAIL_PASSWORD, ""),
                values.getOrDefault(MAIL_FROM, ""),
                !"false".equalsIgnoreCase(values.getOrDefault(MAIL_SSL, "true")));
    }

    public String get(String key) {
        return values().getOrDefault(key, "");
    }

    public void save(Map<String, String> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        updates.forEach((key, value) -> mapper.upsert(new SystemConfig(key, value, now)));
        reload();
    }

    public synchronized void reload() {
        cached = new CachedValues(load(), System.currentTimeMillis() + CACHE_TTL_MILLIS);
    }

    private Map<String, String> values() {
        CachedValues local = cached;
        long now = System.currentTimeMillis();
        if (local == null || local.expireAt() <= now) {
            synchronized (this) {
                local = cached;
                if (local == null || local.expireAt() <= System.currentTimeMillis()) {
                    importYmlMailOnce();
                    reload();
                    local = cached;
                }
            }
        }
        return local.values();
    }

    private Map<String, String> load() {
        Map<String, String> values = new HashMap<>();
        mapper.selectAll().forEach(item -> values.put(item.getConfigKey(),
                item.getConfigValue() == null ? "" : item.getConfigValue()));
        return values;
    }

    /** 旧部署通过环境变量/yml 配置 SMTP：首次读取时一次性迁入数据库，之后以库中配置为准 */
    private void importYmlMailOnce() {
        SystemConfig imported = mapper.selectOneById(MAIL_IMPORTED);
        if (imported != null && StringUtils.isNotBlank(imported.getConfigValue()) || StringUtils.isBlank(ymlMailHost)) {
            return;
        }
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put(MAIL_ENABLED, String.valueOf(ymlMailEnabled));
        updates.put(MAIL_HOST, ymlMailHost);
        updates.put(MAIL_PORT, String.valueOf(ymlMailPort));
        updates.put(MAIL_USERNAME, ymlMailUsername);
        updates.put(MAIL_PASSWORD, ymlMailPassword);
        updates.put(MAIL_FROM, ymlMailFrom);
        updates.put(MAIL_IMPORTED, "true");
        long now = System.currentTimeMillis();
        updates.forEach((key, value) -> mapper.upsert(new SystemConfig(key, value, now)));
        log.info("已将 yml/环境变量中的 SMTP 配置导入 system_config 表");
    }
}