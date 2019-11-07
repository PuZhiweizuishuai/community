package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-07 21:07
 */
@Data
public class LogDTO {
    private String title;
    private String url;
    private String time;
    private Boolean show;

    /**
     * 删除日志时使用
     * */
    private String password;
}
