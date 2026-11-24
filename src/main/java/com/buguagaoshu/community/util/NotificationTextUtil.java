package com.buguagaoshu.community.util;

import com.buguagaoshu.community.enums.NotificationTypeEnum;

/**
 * 消息通知的文本整理。
 * <p>
 * 历史数据里系统消息的详情常常又重复一遍标题（例如标题「你的问题已提交审核」，
 * 详情又写「你的问题已提交审核，通过后将公开展示：…」），一条消息读起来像复读机。
 * 这里统一做一次去重 + 截断，新老数据都能显示成：
 * <pre>
 * [系统] 你的问题已提交审核
 *        「如何入门 Java」
 *        通过后将公开展示，可在「我的审核」查看进度
 * </pre>
 */
public final class NotificationTextUtil {

    /**
     * 详情最多展示的字数，超出截断（前端还有两行截断，这里先兜住脏数据）
     */
    public static final int MAX_DETAIL_LENGTH = 160;

    /**
     * 详情开头需要吞掉的分隔符
     */
    private static final String LEADING_PUNCTUATION = "：:，,。.、；;—- \u3000";

    /** 判断「小句是否重复了标题」时比较的结尾字数（至少 3 字相同才算重复） */
    private static final int CLAUSE_TAIL_LENGTH = 3;

    /** 一句话的结束符，用来切出第一个小句 */
    private static final String CLAUSE_SEPARATORS = "，,。：:；;！!？?";

    private NotificationTextUtil() {
    }

    /**
     * 整理详情文案。
     *
     * @param type        通知类型，决定要不要做「系统文案」的清洗
     * @param raw         落库的原始详情
     * @param objectTitle 通知指向的对象标题（问题 / 评论 / 话题），用于去掉重复引用
     */
    public static String cleanDetail(NotificationTypeEnum type, String raw, String objectTitle) {
        if (raw == null) {
            return "";
        }
        String text = raw.strip().replaceAll("\\s+", " ");
        if (text.isEmpty()) {
            return "";
        }
        // 回复 / 点赞的详情是用户原文，除了压缩空白不能动
        if (type != null && type.isGeneratedDetail()) {
            text = removeObjectQuotes(text, objectTitle);
            text = stripLeadingTitle(text, type.getName());
            text = stripLeadingDuplicateClause(text, type.getName());
            text = stripLeadingPunctuation(text);
            text = stripTrailingPunctuation(text);
        }
        return truncate(text);
    }

    /**
     * 标题式的短文案：去掉首尾空白并按需截断。
     */
    public static String shortText(String raw, int maxLength) {
        if (raw == null) {
            return "";
        }
        String text = raw.strip().replaceAll("\\s+", " ");
        if (maxLength > 0 && text.length() > maxLength) {
            return text.substring(0, maxLength) + "…";
        }
        return text;
    }

    private static String removeObjectQuotes(String text, String objectTitle) {
        if (objectTitle == null || objectTitle.length() < 2) {
            return text;
        }
        return text.replace("「" + objectTitle + "」", "")
                .replace("（内容：" + objectTitle + "）", "")
                .replace("（" + objectTitle + "）", "");
    }

    private static String stripLeadingTitle(String text, String title) {
        if (title == null || title.isEmpty() || !text.startsWith(title)) {
            return text;
        }
        return text.substring(title.length());
    }

    /**
     * 历史文案常写成「你创建的标签「X」已通过审核，…」，和标题说的是同一件事。
     * 这里按首个小句的结尾判断：与标题结尾一致就整句丢掉。
     * 小句里带「」引用的（例如标签名只出现在这里）保留，避免丢信息。
     */
    private static String stripLeadingDuplicateClause(String text, String title) {
        if (title == null || title.length() < CLAUSE_TAIL_LENGTH) {
            return text;
        }
        int end = indexOfClauseEnd(text);
        if (end <= 0) {
            return text;
        }
        String clause = text.substring(0, end);
        if (clause.indexOf('「') >= 0 || clause.length() < CLAUSE_TAIL_LENGTH) {
            return text;
        }
        String tail = title.substring(title.length() - CLAUSE_TAIL_LENGTH);
        if (!clause.endsWith(tail)) {
            return text;
        }
        String rest = text.substring(end);
        // 整句都是重复信息时保留原文，避免详情变成空白
        return rest.isBlank() ? text : rest;
    }

    private static int indexOfClauseEnd(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (CLAUSE_SEPARATORS.indexOf(text.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private static String stripLeadingPunctuation(String text) {
        int index = 0;
        while (index < text.length() && LEADING_PUNCTUATION.indexOf(text.charAt(index)) >= 0) {
            index++;
        }
        return text.substring(index);
    }

    private static String stripTrailingPunctuation(String text) {
        int end = text.length();
        while (end > 0 && LEADING_PUNCTUATION.indexOf(text.charAt(end - 1)) >= 0) {
            end--;
        }
        return text.substring(0, end);
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_DETAIL_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_DETAIL_LENGTH) + "…";
    }
}
