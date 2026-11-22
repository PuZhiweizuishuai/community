package com.buguagaoshu.community.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 全局安全响应头。
 *
 * <p>Spring Boot 不会为纯 REST + 静态资源服务自动注入安全头，这里统一补齐：
 * <ul>
 *   <li>X-Frame-Options：禁止第三方站点用 iframe 嵌入本站页面（防点击劫持）</li>
 *   <li>X-Content-Type-Options: nosniff：阻止浏览器把上传文件按 HTML 嗅探解析</li>
 *   <li>Referrer-Policy：跨站跳转不泄露完整路径</li>
 *   <li>Permissions-Policy：关闭页面不需要的浏览器能力</li>
 *   <li>Strict-Transport-Security：仅在请求确为 HTTPS 时下发（Nginx 终止 TLS 后
 *       依赖 server.forward-headers-strategy=native 解析 X-Forwarded-Proto）</li>
 *   <li>Content-Security-Policy：限制脚本/样式/连接来源</li>
 * </ul>
 *
 * <p>默认 CSP 放行 unpkg（Vditor 运行时从该 CDN 拉取编辑器脚本与代码高亮主题）
 * 与 data:/blob:（图片裁剪、头像预览、录音）。
 * 需要调整时用 {@code app.security.csp} 覆盖，{@code app.security.csp-enabled=false} 可整体关闭。
 */
@Configuration
public class SecurityHeadersFilter {

    /** 默认策略：脚本只允许同源与 Vditor CDN；样式必须允许内联（Vditor / ECharts 运行时注入） */
    static final String DEFAULT_CSP =
            "default-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'; "
                    + "object-src 'none'; img-src 'self' data: blob: https:; "
                    + "media-src 'self' data: blob: https:; font-src 'self' data: https://unpkg.com; "
                    + "style-src 'self' 'unsafe-inline' https://unpkg.com; "
                    + "script-src 'self' https://unpkg.com; worker-src 'self' blob:; "
                    + "connect-src 'self' ws: wss: https:";

    @Value("${app.security.csp-enabled:true}")
    private boolean cspEnabled;

    /** 留空表示使用 {@link #DEFAULT_CSP} */
    @Value("${app.security.csp:}")
    private String customCsp;

    @Value("${app.security.hsts-enabled:true}")
    private boolean hstsEnabled;

    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilterRegistration() {
        String csp = cspEnabled ? (customCsp == null || customCsp.isBlank() ? DEFAULT_CSP : customCsp) : null;
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter((request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            res.setHeader("X-Frame-Options", "SAMEORIGIN");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
            res.setHeader("Cross-Origin-Opener-Policy", "same-origin");

            if (hstsEnabled && isSecure(req)) {
                res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }

            // 文件预览/下载接口不设 CSP：图片、PDF 由浏览器原生处理，加策略只会误伤展示
            if (csp != null && !req.getRequestURI().startsWith("/api/file/")) {
                res.setHeader("Content-Security-Policy", csp);
            }

            chain.doFilter(request, response);
        });
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    /** 反向代理（Nginx）终止 TLS 时，靠 X-Forwarded-Proto 判断原始协议 */
    private static boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && proto.toLowerCase().startsWith("https");
    }
}
