package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 22:01
 */
@Data
public class HotQuestionDTO implements Comparable<HotQuestionDTO> {
    private Long questionId;

    private String title;

    private Long sortWeight;

    @Override
    public int compareTo(HotQuestionDTO o) {
        if(this.sortWeight > o.sortWeight) {
            return -1;
        } else if (this.sortWeight.equals(o.sortWeight)){
            return 0;
        } else {
            return 1;
        }
    }
}
