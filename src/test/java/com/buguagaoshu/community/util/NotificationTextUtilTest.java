package com.buguagaoshu.community.util;

import com.buguagaoshu.community.enums.NotificationTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTextUtilTest {

    @Test
    void removesTitleRepeatedByLegacyContent() {
        // 历史数据：详情又把标题写了一遍，只保留后面的补充说明
        String detail = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.REVIEW_QUESTION_PENDING,
                "你的问题已提交审核，通过后将公开展示：「如何入门 Java」",
                "如何入门 Java");
        assertEquals("通过后将公开展示", detail);
    }

    @Test
    void removesQuotedObjectTitle() {
        String detail = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.TOPIC_REVIEW_PASS,
                "你创建的话题「前端开发」已通过审核，你已成为吧主",
                "前端开发");
        assertEquals("你已成为吧主", detail);
    }

    @Test
    void keepsUserGeneratedContentUntouched() {
        // 回复 / 点赞的详情是用户原文，即使开头碰巧一样也不能删
        String raw = "赞了你的问题，说得太对了";
        assertEquals(raw, NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.LIKE_QUESTION, raw, "如何入门 Java"));
    }

    @Test
    void dropsLeadingClauseThatRepeatsTheTitle() {
        // 首个小句与标题说的是同一件事，整句丢掉，只留下处理结果
        String report = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.REPORT_ACCEPTED,
                "你举报的问题已受理，相关内容已同步删除。",
                null);
        assertEquals("相关内容已同步删除", report);

        String invite = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.TOPIC_INVITE,
                "吧主邀请你加入话题「Java」，请前往话题页接受邀请",
                "Java");
        assertEquals("请前往话题页接受邀请", invite);
    }

    @Test
    void keepsClauseThatCarriesUniqueInfo() {
        // 标签名只出现在这个小句里，不能删
        String detail = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.TAG_REVIEW_PASS,
                "你创建的标签「消息中心」已通过审核，所有关联帖子现在会公开显示该标签",
                null);
        assertEquals("你创建的标签「消息中心」已通过审核，所有关联帖子现在会公开显示该标签", detail);
    }

    @Test
    void trimsWhitespaceAndTruncatesLongText() {
        String detail = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.REVIEW_COMMENT_PENDING,
                "  第一行\n\n第二行  ", null);
        assertEquals("第一行 第二行", detail);

        String longText = "内".repeat(NotificationTextUtil.MAX_DETAIL_LENGTH + 20);
        String trimmed = NotificationTextUtil.cleanDetail(
                NotificationTypeEnum.SYSTEM_NOTIFICATION, longText, null);
        assertTrue(trimmed.endsWith("…"));
        assertEquals(NotificationTextUtil.MAX_DETAIL_LENGTH + 1, trimmed.length());
    }

    @Test
    void blankDetailBecomesEmptyString() {
        assertEquals("", NotificationTextUtil.cleanDetail(NotificationTypeEnum.SYSTEM_NOTIFICATION, null, null));
        assertEquals("", NotificationTextUtil.cleanDetail(NotificationTypeEnum.SYSTEM_NOTIFICATION, "   ", null));
    }

    @Test
    void shortTextUsesSingleEllipsis() {
        assertEquals("一二三…", NotificationTextUtil.shortText("一二三四五", 3));
        assertEquals("一二三", NotificationTextUtil.shortText("一二三", 3));
    }
}
