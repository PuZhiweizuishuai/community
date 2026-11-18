package com.buguagaoshu.community.dto;

import lombok.Data;

import java.util.List;

/**
 * 发布页标签建议分组：categoryName 为分组名，tags 为组内标签名。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class TagDTO {
    private String categoryName;
    private List<String> tags;
}
