package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.TagClass;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:34
 */
public interface TagClassService {
    PaginationDto<TagClass> getTagClassByType(String type, String page, String size);


    /**
     * 获取当前类型的话题
     * @param type 类型
     * @return 话题列表
     * */
    List<TagClass> getTagClassByTypeNotUserPage(String type);

    /**
     * 更新当前话题的讨论数
     *
     * @param tag 标签
     * @param count 增加或减少数量
     * @return 1
     */
    int updateTalkCount(String tag, int count);

    /**
     * 修改问题后对话题讨论数量的调整
     *
     * @param newQuestionTag 修改后的标签
     * @param oldQuestionTag 修改前的标签
     * @return 1
     */
    int alterQuestionTalkCount(String[] newQuestionTag, String[] oldQuestionTag);
}
