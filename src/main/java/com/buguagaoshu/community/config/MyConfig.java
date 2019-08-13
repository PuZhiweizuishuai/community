package com.buguagaoshu.community.config;


import com.buguagaoshu.community.component.MyLocaleResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     * */
    @Bean
    public LocaleResolver localeResolver() {
        return new MyLocaleResolver();
    }
}