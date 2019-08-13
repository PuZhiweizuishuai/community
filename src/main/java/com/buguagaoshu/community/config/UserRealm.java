package com.buguagaoshu.community.config;


import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 20:50
 * 自定义 Realm
 */
public class UserRealm extends AuthorizingRealm {
    @Autowired
    private UserService userService;

    /**
     * 执行授权逻辑
     * */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        System.out.println("执行授权逻辑");
        return null;
    }


    /**
     * 执行认证逻辑
     * */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        // 判断用户名密码
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        User user = userService.selectUserByEmail(token.getUsername());
        if(user == null) {
            // 直接 return null 为用户名不存在错误
            throw new UnknownAccountException("用户不存在！");
        }
        // System.out.println(token.getPassword());
        /*
        if(StringUtil.judgePassword(new String(token.getPassword()), user.getPassword())) {
            System.out.println(token.getPassword());
            return new SimpleAuthenticationInfo(user, user.getPassword(), getName());
        } else {
            // 密码错误
            throw new IncorrectCredentialsException("密码错误");
        }
        */
        return new SimpleAuthenticationInfo(token.getUsername(), user.getPassword(), getName());
    }
}
