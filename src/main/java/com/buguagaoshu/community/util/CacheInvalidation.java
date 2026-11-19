package com.buguagaoshu.community.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 缓存失效统一入口：事务内注册到 afterCommit，避免「失效后、提交前」的旧值回填窗口；
 * 无事务上下文时立即执行。
 */
public final class CacheInvalidation {

    private CacheInvalidation() {
    }

    public static void afterCommit(Runnable... actions) {
        if (actions == null || actions.length == 0) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Runnable action : actions) {
                        action.run();
                    }
                }
            });
        } else {
            for (Runnable action : actions) {
                action.run();
            }
        }
    }
}
