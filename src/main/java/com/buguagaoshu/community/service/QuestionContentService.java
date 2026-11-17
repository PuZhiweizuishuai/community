package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.QuestionContentDto;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.User;

import java.util.List;

public interface QuestionContentService {

    /**
     * 全量替换问题的内容段落（编辑时先删后插），sort 按列表顺序重排。
     */
    void replaceContents(long questionId, List<QuestionContent> contents);

    /**
     * 按顺序读取问题的原始段落（含明文密码，仅作者编辑回填使用）。
     */
    List<QuestionContent> listByQuestionId(long questionId);

    /**
     * 按当前访问者裁剪可见性：作者与管理员看全部；
     * 密码段落未解锁、回复可见段落未评论时 locked=true 且不下发 content。
     */
    List<QuestionContentDto> visibleContents(long questionId, long ownerId, User viewer, boolean isAdmin);

    /**
     * 校验阅读密码并返回段落内容。作者本人无需密码；
     * 密码错误按 段落+IP 计数限流，防止暴力尝试。
     */
    String unlock(long questionId, long contentId, String password, User viewer, String ip);
}