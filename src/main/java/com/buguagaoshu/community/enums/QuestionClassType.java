package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-07 14:29
 * 问题分类
 */
public enum QuestionClassType {
    /**
     *
     * */
    ALL(0, "全部"),

    /**
     *
     * */
    PUT_QUESTION(1, "提问"),

    /**
     *
     * */
    TALK_OVER(2, "讨论"),

    /**
     *
     * */
    SHARE(3, "分享"),

    /**
     *
     *
     * */
    ADVISE(4, "建议"),


    /**
     *
     * */
    BUG(5, "Bug"),

    /**
     *
     * */
    WATER(6, "灌水");

    int type;

    String name;

    QuestionClassType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static String getNameStr(Integer type) {
        for(QuestionClassType questionClassType : QuestionClassType.values()) {
            if(questionClassType.getType() == type) {
                return questionClassType.getName();
            }
        }
        return "";
    }
}
