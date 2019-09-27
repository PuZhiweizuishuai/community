package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-27 20:46
 */
public enum CommentSortTypeEnum {
    /**
     * 默认按时间顺序排序
     * */
    OLD(0),

    /**
     * 按时间倒序排序
     * */
    NEW(1),

    /**
     * 最多点赞
     * */
    BEST_LIKE(2),

    /**
     * 最多评论
     * */
    BEST_COMMENT(3);

    int type;

    CommentSortTypeEnum(int type) {
        this.type = type;
    }

    public Integer getTypeCode() {
        return type;
    }

    public static CommentSortTypeEnum getType(String type) {
        int typeCode;
        try {
            typeCode = Integer.valueOf(type);
        } catch (Exception e) {
            return OLD;
        }
        for(CommentSortTypeEnum i : CommentSortTypeEnum.values()) {
            if(i.getTypeCode().equals(typeCode)) {
                return i;
            }
        }
        return OLD;
    }
}
