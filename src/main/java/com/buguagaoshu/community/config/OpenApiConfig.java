package com.buguagaoshu.community.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档元信息。Swagger UI: /swagger-ui.html，JSON: /v3/api-docs。
 * 生产环境可用 springdoc.api-docs.enabled=false / springdoc.swagger-ui.enabled=false 关闭。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI communityOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("不挂高数社区 API")
                .description("前后端分离 REST 接口文档；会话认证基于 SESSION cookie，「记住我」为 HttpOnly token cookie")
                .version("1.0"));
    }
}