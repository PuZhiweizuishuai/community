package com.buguagaoshu.community.component;

import com.buguagaoshu.community.util.IpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 把 app.trusted-proxies（逗号分隔的代理 IP 白名单）注入 IpUtil。
 * 生产部署在 Nginx/SLB 后面时，把代理层地址配进来才会解析 X-Forwarded-For。
 */
@Component
public class IpUtilConfigurer {

    public IpUtilConfigurer(@Value("${app.trusted-proxies:127.0.0.1,::1}") String proxies) {
        Set<String> set = Arrays.stream(proxies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        IpUtil.setTrustedProxies(set);
    }
}