package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 14:56
 */
@Data
public class UserDto implements Comparable<UserDto> {
    private long id = -1;


    private String userId;


    private String userName;

    private String email;

    private int age;


    private String birthday;

    private String sex;
    private String school;
    private String major;
    private String headUrl;

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
     * 问题数
     * */
    private long questionCount;

    /**
     * 关注数
     * */
    private long followCount;

    /**
     * 粉丝数
     * */
    private long fansCount;


    /**
     * 获赞数
     * */
    private long likeCount;


    /**
     * 排序权值
     * */
    private long sortWeight;

    @Override
    public int compareTo(UserDto o) {
        if(this.sortWeight > o.sortWeight) {
            return -1;
        } else if (this.sortWeight == o.sortWeight){
            return 0;
        } else {
            return 1;
        }
    }
}
