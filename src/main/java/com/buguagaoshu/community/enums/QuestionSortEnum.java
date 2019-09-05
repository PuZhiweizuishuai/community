package com.buguagaoshu.community.enums;

import com.buguagaoshu.community.model.Question;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 17:22
 */
public enum QuestionSortEnum {
    /**
     * 零回复
     */
    NO_REPLY(1),

    /**
     * 热门
     */
    HOT_QUESTION(2),

    /**
     * 新问题
     */
    NEW_QUESTION(3);


    private int type;


    public int getType() {
        return type;
    }


    QuestionSortEnum(int type) {
        this.type = type;
    }
}
