package com.buguagaoshu.community.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * 客户端 IP 解析：只有请求来自可信代理层时才解析 X-Forwarded-For，
 * 否则一律使用 TCP 层 remoteAddr，防止客户端伪造 IP 绕过限流/审计。
 * 兼容 IPv6（不再按长度 15 截断）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 13:29
 */
public class IpUtil {

    /** 可信代理地址，由 app.trusted-proxies 配置注入；默认只信任本机（开发代理） */
    private static volatile Set<String> trustedProxies = Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    public static void setTrustedProxies(Set<String> proxies) {
        trustedProxies = proxies == null ? Set.of() : Set.copyOf(proxies);
    }

    public static String getIpAddr(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && trustedProxies.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // 多级代理时第一个地址是客户端 IP
                String first = forwarded.split(",")[0].trim();
                if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first)) {
                    return first;
                }
            }
        }
        return remoteAddr == null ? "" : remoteAddr;
    }
}