package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 18:46
 * 管理数据
 */
@Data
public class AdminData {
    private Long adminId;
    private Long time;
    private Long questionCount;
    private Long userCount;
    private Long userAddCount;
    private Long questionAddCount;
}
