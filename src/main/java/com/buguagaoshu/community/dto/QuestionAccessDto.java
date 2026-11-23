package com.buguagaoshu.community.dto;

/**
 * 当前访问者对某个问题的访问能力。
 *
 * @param visible       问题是否可见（false 统一按 404 处理）
 * @param canViewContent 是否可读取正文、评论等具体内容
 * @param loginRequired 是否仅因未登录而无法查看（登录后即可查看）
 */
public record QuestionAccessDto(boolean visible, boolean canViewContent, boolean loginRequired) {
}
