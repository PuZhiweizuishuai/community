package com.buguagaoshu.community.exception;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:25
 */
public interface ICustomizeErrorCode {
    /**
     * 返回错误信息
     *
     * @return 错误信息
     */
    String getMessage();

    /**
     * 返回错误代码
     *
     * @return 错误代码
     */
    Integer getCode();
}
