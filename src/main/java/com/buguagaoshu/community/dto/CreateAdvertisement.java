package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 23:05
 */
@Data
public class CreateAdvertisement {
    private Long id;
    private String title;
    private String url;
    private String image;
    private Long time;
    private String position;
}
