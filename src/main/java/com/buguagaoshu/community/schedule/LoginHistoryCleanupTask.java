package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.component.RedisSchedulerLock;
import com.buguagaoshu.community.service.UserLoginHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录历史保留 180 天，每天凌晨清理一次。
 */
@Component
@Slf4j
public class LoginHistoryCleanupTask {

    private static final long RETENTION_MILLIS = 180L * 24 * 3600 * 1000;

    private final UserLoginHistoryService userLoginHistoryService;
    private final RedisSchedulerLock schedulerLock;

    public LoginHistoryCleanupTask(UserLoginHistoryService userLoginHistoryService,
                                   RedisSchedulerLock schedulerLock) {
        this.userLoginHistoryService = userLoginHistoryService;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanup() {
        if (!schedulerLock.tryLock("loginHistoryCleanup", Duration.ofMinutes(10))) {
            return;
        }
        try {
            int deleted = userLoginHistoryService.cleanupBefore(System.currentTimeMillis() - RETENTION_MILLIS);
            if (deleted > 0) {
                log.info("已清理 {} 条 180 天前的登录历史", deleted);
            }
        } finally {
            schedulerLock.unlock("loginHistoryCleanup");
        }
    }
}
