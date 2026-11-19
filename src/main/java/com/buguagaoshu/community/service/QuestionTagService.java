package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.QuestionTagDto;
import com.buguagaoshu.community.model.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 标签核心服务：发帖解析、审核状态机、帖子冗余标签列同步与使用数维护。
 *
 * <p>约定：questions.tag 是冗余展示列，唯一写入入口是 {@link #refreshTagNames(long)}，
 * 任何标签关系或审核状态变化都必须调用它，避免冗余列与关联表漂移。
 */
public interface QuestionTagService {

    /**
     * 把用户输入的标签名解析为标签实体：
     * 已存在直接复用，未通过的重置为待审核，不存在的新建为待审核。
     */
    List<Tag> resolveTags(List<String> names, long userId);

    /**
     * 用给定标签全量替换帖子的标签关联，并同步冗余列与标签使用数。
     */
    void replaceQuestionTags(long questionId, List<Tag> tags);

    /**
     * 发帖/编辑入口：解析标签名并替换关联。
     */
    void saveQuestionTags(long questionId, List<String> names, long userId);

    /** 按关联表重算 questions.tag 冗余列（只包含已通过标签） */
    void refreshTagNames(long questionId);

    /** 批量重算 questions.tag 冗余列，单条 SQL 覆盖整批帖子 */
    void refreshTagNames(Collection<Long> questionIds);

    /** 帖子关联的全部标签（含未通过，仅供管理场景） */
    List<Tag> tagsOfQuestion(long questionId);

    /** 详情页标签：已通过 + 访问者自己的待审核标签 */
    List<QuestionTagDto> tagsForDetail(long questionId, Long viewerId, boolean author);

    /** 列表批量装配：仅已通过标签，避免 N+1 */
    Map<Long, List<QuestionTagDto>> approvedTagsOfQuestions(Collection<Long> questionIds);

    /** 按关联表重算标签使用数 */
    void refreshUsageCount(Collection<Long> tagIds);

    /** 发布页建议/热门标签：只返回审核通过的标签 */
    List<Tag> approvedTags(int limit);
}
