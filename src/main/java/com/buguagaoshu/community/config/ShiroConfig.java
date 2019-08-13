package com.buguagaoshu.community.config;

import com.buguagaoshu.community.util.MyCredentialsMatcher;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 20:44
 * Shiro 配置类
 */
@Configuration
public class ShiroConfig {
    /**
     * 创建 ShiroFilterFactoryBean
     **/
    @Bean
    public ShiroFilterFactoryBean getShiroFilterFactoryBean(@Qualifier("securityManager") DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        // 设置安全管理器
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        /**
         * 添加 Shiro 内置过滤器
         * Shiro 内置过滤器，可以实现权限相关的拦截器
         *     常用的过滤器：
         *         anon：无需认证（登陆）可以访问
         *         authc：必须认证，才可以访问
         *         user：使用 rememberMe 的功能可以直接访问
         *         perms：该资源必须得到资源权限才可以访问
         *         role：该资源必须得到角色权限才可以访问
         * */
        LinkedHashMap<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/api/update/*","authc");
        // 修改跳转页面
        shiroFilterFactoryBean.setLoginUrl("/sign-in");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterMap);
        return shiroFilterFactoryBean;
    }


    /**
     * 创建 DefaultWebSecurityManager 安全管理器
     * */
    @Bean(name = "securityManager")
    public DefaultWebSecurityManager getDefaultWebSecurityManager(@Qualifier("userRealm") UserRealm userRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // 关联 realm
        securityManager.setRealm(userRealm);
        return securityManager;
    }

    /**
     * 创建 Realm 对象
     * */
    @Bean(name = "userRealm")
    public UserRealm getRealm() {
        return new UserRealm();
    }

    @Bean
    public MyCredentialsMatcher myCredentialsMatcher() {
        return new MyCredentialsMatcher();
    }
}
