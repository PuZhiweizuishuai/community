package com.buguagaoshu.community.config;


import com.buguagaoshu.community.component.MyLocaleResolver;
import com.buguagaoshu.community.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.LocaleResolver;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 16:18
 * 自定义配置
 */
@Configuration
public class MyConfig {

    /**
     * 将 MyLocaleResolver 添加到容器
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new MyLocaleResolver();
    }


    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 默认池大小为 1，7 个定时任务会互相阻塞
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("community-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}