package com.buguagaoshu.community.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前后端分离后只保留登录态恢复拦截器。
 *
 * <p>管理员权限不再用路径拦截器（旧的 AdminInterceptor 会拦截普通用户删除自己问题的接口，
 * 且返回空响应而不是 401），改由各后台接口显式调用 AuthSupport#requireAdmin 判断。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;

    public WebConfig(SessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor).addPathPatterns("/api/**", "/ws/**");
    }
}
