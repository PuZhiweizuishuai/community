package com.buguagaoshu.community.dto;


import lombok.Data;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 22:23
 */
@Data
public class TagDTO {
    private String categoryName;

    private List<String> tags;
}