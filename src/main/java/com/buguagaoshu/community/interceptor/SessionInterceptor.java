package com.buguagaoshu.community.interceptor;

import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-21 12:09
 */
@Service
public class SessionInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Autowired
    OnlineUserService onlineUserService;

    @Autowired
    UserPermissionService userPermissionService;

    @Autowired
    JwtUtil jwtUtil;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**
         * TODO 待改进优化, 对于 token 的使用
         * */
        String token = null;
        if (request.getCookies() != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("token")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        /**
         * TODO 优化登陆逻辑
         * */
        request.getSession().setAttribute("checkOnline", false);
        if (token != null && !token.equals("")) {
            try {
                Claims claims = jwtUtil.parseJWT(token);
                User suser = (User) request.getSession().getAttribute("user");
                if(suser == null) {
                    User user = userService.selectUserById(Long.valueOf(claims.getSubject()));
                    UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
                    user.setPower(userPermission.getPower());
                    request.getSession().setAttribute("user", user);
                }
            } catch (Exception e) {
                request.getSession().removeAttribute("user");
            }

            /* TODO 待优化
            OnlineUser onlineUser = onlineUserService.selectOnlineUserByToken(token);
            if(onlineUser != null) {
                User user = userService.selectUserById(onlineUser.getId());
                UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
                user.setPower(userPermission.getPower());
                request.getSession().setAttribute("user", user);
            } else {
                request.getSession().removeAttribute("user");
                request.getSession().setAttribute("checkOnline", true);
            }
            */
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
