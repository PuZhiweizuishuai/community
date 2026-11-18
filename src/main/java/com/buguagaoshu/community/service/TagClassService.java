package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.TagClass;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:34
 */
public interface TagClassService {

    /**
     * 获取当前类型的话题
     * @param type 类型
     * @return 话题列表
     * */
    List<TagClass> getTagClassByTypeNotUserPage(String type);

    /**
     * 按话题 ID 调整讨论数（原子增量）。话题为单选，不再拆标签字符串。
     *
     * @param topicId 话题 ID
     * @param count   增加或减少数量
     * @return 更新的行数
     */
    int updateTalkCount(long topicId, int count);

    /**
     * 跟新关注数
     * @param tagClass 话题
     * @return 结果
     * */
    int updateFollowCount(TagClass tagClass);

    /**
     * 按 ID 查话题；缓存未命中时回退数据库。
     */
    TagClass findById(long topicId);
}
