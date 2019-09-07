package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-07 15:07
 * 问题的排序方法
 */
public enum  QuestionSortType {
    /**
     * */
    NO_COMMENT(0, "零回复"),

    /**
     * */
    HOT_QUESTION(1, "热门"),

    /**
     *
     * */
    RECOMMENDED_QUESTION(2, "推荐"),

    /**
     * */
    NEW_QUESTION(3, "最新");


    int type;
    String name;

    QuestionSortType(int type, String name) {
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
}
