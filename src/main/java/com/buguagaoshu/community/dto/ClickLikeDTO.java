package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 16:13
 */
@Data
public class ClickLikeDTO {
    private Long notifier;
    private String notifierName;
    private Long receiver;
    private Long questionId;
    private Long commentId;
    private String token;
}
