package com.buguagaoshu.community.dto;


import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 15:31
 */
@Data
public class HotTagDTO implements Comparable {
    private String name;
    private Long priority;

    @Override
    public int compareTo(Object o) {
        if(this.priority > ((HotTagDTO) o).getPriority()) {
            return 1;
        } else if (this.priority.equals(((HotTagDTO) o).getPriority())){
            return 0;
        } else {
            return -1;
        }
    }
}
