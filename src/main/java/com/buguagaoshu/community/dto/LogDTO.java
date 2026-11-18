package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-07 21:07
 */
@Data
public class LogDTO {
    @NotBlank(message = "请选择要删除的日志")
    private String title;

    private String url;
    private String time;
    private Boolean show;

    /**
     * 删除日志时使用
     * */
    @NotBlank(message = "请输入管理员密码")
    private String password;
}
