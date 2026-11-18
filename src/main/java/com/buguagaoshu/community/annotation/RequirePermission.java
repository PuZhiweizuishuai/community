package com.buguagaoshu.community.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需的 RBAC 权限编码。
 *
 * <p>可标注在 Controller 类或方法上，方法上的声明优先。{@code admin:*} 权限会额外
 * 校验 {@code admin:access}；{@code content:*} / {@code file:*} 属于普通用户权限，
 * 不要求后台访问权限。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 权限编码，见 {@code PermissionCodes} */
    String value();
}
