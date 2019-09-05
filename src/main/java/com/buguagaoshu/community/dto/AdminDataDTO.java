package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 20:38
 */
@Data
public class AdminDataDTO {
    private Long adminId;
    private String time;
    private Long questionCount;
    private Long userCount;
    private Long userAddCount;
    private Long questionAddCount;
}
