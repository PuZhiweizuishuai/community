package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 14:23
 * 定时删除数据库在线用户表中登陆过期的用户
 */
@Component
@Slf4j
public class DeleteOnlineUserTasks {
    @Autowired
    OnlineUserService onlineUserService;

    /**
     * 每间隔两小时，清理一次过期用户
     * */
    @Scheduled(fixedRate = 7200000)
    public void reportCurrentTime() {
        onlineUserService.deleteExpireTimeUser(System.currentTimeMillis());

        log.info("定时删除登陆过期的用户，执行时间 {}", StringUtil.getNowTime());
    }
}
