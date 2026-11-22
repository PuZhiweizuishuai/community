package com.buguagaoshu.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2020-03-29 13:56
 *
 * <p>Spring Boot 4 / Spring Data Redis 4 已切换到 Jackson 3。
 * 会话序列化开启「白名单」多态类型：只允许本项目模型与 JDK 基础类型，
 * 不再使用 enableUnsafeDefaultTyping（Redis 被写入恶意 payload 时可触发反序列化 gadget）。
 *
 * <p>注意：手工 {@code @EnableRedisHttpSession} 后 Spring Boot 的
 * {@code server.servlet.session.cookie.*} 不再生效，SESSION cookie 的属性必须通过
 * {@link CookieSerializer} 显式设置，否则生产环境的登录态 cookie 不会带 Secure 标记。
 */
@Configuration
@EnableRedisHttpSession
public class HttpSessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.buguagaoshu.community.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(validator)
                .build();
    }

    /**
     * 登录态主 cookie 的属性：生产（COOKIE_SECURE=true）必须带 Secure，
     * 同时保持 HttpOnly + SameSite=Lax 防 CSRF。
     */
    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure,
            @Value("${server.servlet.session.cookie.same-site:Lax}") String sameSite) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite(sameSite);
        return serializer;
    }
}