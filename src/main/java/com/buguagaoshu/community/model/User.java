package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:19
 * 用户类
 * 使用 JSR-303 进行数据检验
 */
@Data
@Table(value = "users", camelToUnderline = false)
public class User {
    @Id(keyType = KeyType.Auto)
    private long id = -1;


    private String userId;


    private String userName;


    private String password;

    private String email;

    private int age;


    private String birthday;

    private String sex;
    private String school;
    private String major;
    private long creationTime;
    private String headUrl;
    private long lastTime;
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

    /**
     * 关注数
     * */
    private long followCount;

    /**
     * 粉丝数
     * */
    private long fansCount;

    private long likeCount;

    /** 个人主页隐私开关：1 公开，0 隐藏 */
    private int showFollowQuestions = 1;

    private int showFollowTopics = 1;

    private int showFans = 1;

    private int showFollowingUsers = 1;

    @Column(ignore = true)
    private int power;

    /** 用户角色（仅用于后台列表展示，不落库） */
    @Column(ignore = true)
    private List<Long> roleIds;

    @Column(ignore = true)
    private List<String> roleCodes;

    @Column(ignore = true)
    private List<String> roleNames;

    /** 封禁状态（仅后台用户列表装配，不落库） */
    @Column(ignore = true)
    private int banType;

    @Column(ignore = true)
    private long banEndTime;

    @Column(ignore = true)
    private String banReason;

    /**
     * 清除敏感数据
     * */
    public void clean() {
        this.password = null;
    }
}
