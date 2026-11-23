package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;

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
     * 发布页话题选择器：只返回启用中的话题，支持标题关键字过滤，最多 limit 条。
     *
     * @param keyword 标题关键字，为空时按热度返回
     * @param limit   返回条数上限（内部钳制到 1~50）
     * @param viewer  当前用户：私有话题只返回已加入 / 吧务 / 创建者的话题
     * @return 精简后的话题列表
     */
    List<TagClass> searchEnabledTopics(String keyword, int limit, User viewer);

    /**
     * 搜索页话题结果：按标题关键字匹配审核通过且启用的话题，分页返回。
     * 私有话题仅在其信息对当前用户可见时出现。
     *
     * @param keyword 标题关键字
     * @param page    页码
     * @param size    每页条数
     * @param viewer  当前用户，未登录时可为 null
     * @return 话题分页结果
     */
    PaginationDto<TagClass> searchTopics(String keyword, String page, String size, User viewer);

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

    /**
     * 只返回审核通过且启用的话题。
     */
    TagClass findApprovedById(long topicId);
}
