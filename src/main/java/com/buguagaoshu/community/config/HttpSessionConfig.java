package com.buguagaoshu.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2020-03-29 13:56
 *
 * <p>Spring Boot 4 / Spring Data Redis 4 已切换到 Jackson 3，
 * 原 Jackson2JsonRedisSerializer 被标记为待删除。这里改用基于 Jackson 3 的
 * GenericJacksonJsonRedisSerializer，并开启默认类型信息，保证会话中的
 * User、Question 等自定义对象反序列化后仍是原类型，而不是 LinkedHashMap。
 */
@Configuration
@EnableRedisHttpSession
public class HttpSessionConfig {
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();
    }
}
