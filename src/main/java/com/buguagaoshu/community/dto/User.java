package com.buguagaoshu.community.dto;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:19
 * 用户类
 * 使用 JSR-303 进行数据检验
 */
public class User {
    private long id = -1;

    @Length(max = 20, message = "用户名不能超过20个字符")
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
    private String creationTime;
    private String headUrl;
    private String lastTime;

    public User() {

    }

    public User(long id, String userName, String password, String email, String birthday, String sex, String school, String creationTime, String headUrl, String lastTime) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.birthday = birthday;
        this.sex = sex;
        this.school = school;
        this.creationTime = creationTime;
        this.headUrl = headUrl;
        this.lastTime = lastTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getcreationTime() {
        return creationTime;
    }

    public void setCreateTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
