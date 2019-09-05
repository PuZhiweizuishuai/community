package com.buguagaoshu.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * @author Pu Zhiwei
 * spring boot 启动类
 * EnableScheduling 是一个开启 spring 定时任务的注解
 * */
@SpringBootApplication
@EnableScheduling
public class CommunityApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }
}
