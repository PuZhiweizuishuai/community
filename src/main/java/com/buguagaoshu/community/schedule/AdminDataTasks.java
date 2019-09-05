package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.model.AdminData;
import com.buguagaoshu.community.service.AdminDataService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 20:23
 */
@Component
@Slf4j
public class AdminDataTasks {

    final
    AdminDataService adminDataService;

    final
    UserService userService;

    final
    QuestionService questionService;

    @Autowired
    public AdminDataTasks(AdminDataService adminDataService, UserService userService, QuestionService questionService) {
        this.adminDataService = adminDataService;
        this.userService = userService;
        this.questionService = questionService;
    }

    /**
     * 定时设置管理员数据
     * 默认六小时一次
     */
    @Scheduled(fixedRate = 21600000)
    public void setAdminData() {
        AdminData adminData = new AdminData();
        adminData.setTime(System.currentTimeMillis());
        adminData.setQuestionCount(questionService.getAllQuestionCount());
        adminData.setUserCount(userService.getAlluserCount());
        try {
            AdminData lastData = adminDataService.getBestNetAdminData();
            if (lastData == null) {
                adminData.setUserAddCount(adminData.getUserCount());
                adminData.setQuestionAddCount(adminData.getQuestionCount());
            } else {
                adminData.setUserAddCount(adminData.getUserCount() - lastData.getUserCount());
                adminData.setQuestionAddCount(adminData.getQuestionCount() - lastData.getQuestionCount());
            }
            adminDataService.insertAdminData(adminData);
            log.info("管理员数据同步完成 {}", new Date());
        } catch (Exception e) {
            log.error("管理员数据同步失败 {}", e.getMessage());
        }

    }
}
