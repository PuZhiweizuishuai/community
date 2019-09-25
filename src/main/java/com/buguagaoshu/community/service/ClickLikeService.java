package com.buguagaoshu.community.service;


import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.model.ClickLike;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 16:11
 */
public interface ClickLikeService {
    /**
     * 向数据库插入点赞信息
     * @param clickLikeDTO 点赞信息
     * @return 结果
     */
    ClickLikeTypeEnum createClickLike(ClickLikeDTO clickLikeDTO);

    /**
     * 判断是否以及点过赞
     * */
    Boolean isClickLikeQuestion(ClickLike clickLike);
}
