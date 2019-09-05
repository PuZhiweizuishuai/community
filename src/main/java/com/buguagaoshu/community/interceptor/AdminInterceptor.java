package com.buguagaoshu.community.interceptor;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 19:28
 */
@Service
@Slf4j
public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            log.error("服务器拦截了来自 {} 访问管理员页面的请求！", IpUtil.getIpAddr(request));
            return false;
        }
        return true;
    }
}
