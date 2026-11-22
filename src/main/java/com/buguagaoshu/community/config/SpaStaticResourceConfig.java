package com.buguagaoshu.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

/**
 * 让 Spring Boot 同时托管打包后的 Vue 单页应用。
 *
 * <p>普通静态文件（assets、image 等）按 classpath:/static/ 原样返回；
 * 对于 Vue history 模式下的前端路由（如 /question/1、/admin/users），
 * 磁盘上并没有对应文件，直接刷新会 404，这里统一回退到 index.html，
 * 再由 vue-router 接管渲染。
 *
 * <p>API / WebSocket / 文档等路径不参与回退，保证它们该 404 时仍然 404。
 */
@Configuration
public class SpaStaticResourceConfig implements WebMvcConfigurer {

    /** 这些前缀属于接口或真实静态资源，不能当成前端路由回退到 index.html。 */
    private static final List<String> NON_PAGE_PREFIXES = List.of(
            "api/", "ws/", "assets/", "image/", "captche", "adUrl",
            "v3/", "swagger-ui", "webjars/"
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    private static class SpaResourceResolver extends PathResourceResolver {

        private final Resource index = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }

            // 带扩展名的（js/css/png/ico…）视为真实文件请求，缺失就让它 404；
            // 接口路径也不回退；只有无扩展名的 GET 路径才当 SPA 路由处理。
            if (resourcePath.isEmpty() || resourcePath.contains(".") || isReserved(resourcePath)) {
                return null;
            }
            return index.exists() ? index : null;
        }

        private static boolean isReserved(String path) {
            return NON_PAGE_PREFIXES.stream()
                    .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix));
        }
    }
}
