package com.buguagaoshu.community.model;

import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:19
 * 用户类
 * 使用 JSR-303 进行数据检验
 */
@Data
public class User {
    private long id = -1;

    @Length(max = 20, message = "用户名不能超过20个字符")
    @NotNull(message = "不能为空")
    @Pattern(regexp = "^\\w+$", message = "用户名只能是字母，数字，下划线")
    private String userId;

    @Length(max = 20, message = "昵称不能超过20个字符")
    @NotNull(message = "不能为空")
    private String userName;

    @Pattern(regexp = "^([A-Z]|[a-z]|[0-9]|[`~!@#$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“'。，、？]){6,30}$",
            message = "密码只能是大写或小写字母，数字及标点符号组成长度至少6位的字符串。")
    @Length(max = 30, min = 6, message = "密码最长不超过30个字符,最短不少于6个字符！")
    @NotNull(message = "密码不能为空")
    private String password;

    @Email(message = "邮箱格式不正确！")
    @NotNull(message = "邮箱不能为空")
    private String email;

    private int age;

    @Pattern(regexp = "^(?:(?!0000)[0-9]{4}([-/.]?)(?:(?:0?[1-9]|1[0-2])\\1(?:0?[1-9]|1[0-9]|2[0-8])|(?:0?[13-9]|1[0-2])\\1(?:29|30)|(?:0?[13578]|1[02])\\1(?:31))|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)([-/.]?)0?2\\2(?:29))$",
            message = "日期格式不正确，应为YYYY-MM-DD")
    @NotNull(message = "日期不能为空！")
    private String birthday;

    @NotNull(message = "性别不能为空")
    private String sex;
    private String school;
    private String major;
    private String creationTime;
    private String headUrl;
    private String lastTime;
    private String userTopPhotoUrl;

    /**
     * 一句话介绍
     * */
    private String simpleSelfIntroduction;

    /**
     * 自我介绍
     * */
    private String selfIntroduction;

    /**
     * 爱好
     * */
    private String likes;

    private int power;

    /**
     * 清除敏感数据
     * */
    public void clean() {
        this.password = null;
    }
}
