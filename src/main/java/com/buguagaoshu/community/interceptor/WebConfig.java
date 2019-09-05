package com.buguagaoshu.community.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-21 11:50
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final SessionInterceptor sessionInterceptor;

    private final AdminInterceptor adminInterceptor;

    @Autowired
    public WebConfig(SessionInterceptor sessionInterceptor, AdminInterceptor adminInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor).
                addPathPatterns("/","/user/**","/publish","/question/**","/search/**");
        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**").excludePathPatterns("/admin", "/admin/login");
    }
}