package com.buguagaoshu.community.util;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 0:28
 */
public class MyCredentialsMatcher implements CredentialsMatcher {
    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        String rawPassword = String.valueOf((char[]) token.getCredentials());
        String encodedPassword = String.valueOf(info.getCredentials());
        return StringUtil.judgePassword(rawPassword, encodedPassword);
    }

}
