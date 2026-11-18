package com.buguagaoshu.community.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 登录态恢复 + 封禁拦截 + RBAC 权限拦截。
 *
 * <p>顺序：SessionInterceptor 恢复登录态 → BanInterceptor 拦截封禁用户的写操作
 * → PermissionInterceptor 校验权限点。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;
    private final BanInterceptor banInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebConfig(SessionInterceptor sessionInterceptor, BanInterceptor banInterceptor,
                     PermissionInterceptor permissionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
        this.banInterceptor = banInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor).addPathPatterns("/api/**", "/ws/**");
        registry.addInterceptor(banInterceptor).addPathPatterns("/api/**").order(1);
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**").order(2);
    }
}
