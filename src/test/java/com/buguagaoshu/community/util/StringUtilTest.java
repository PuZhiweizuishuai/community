package com.buguagaoshu.community.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {

    @Test
    void sanitizeEscapesLikeWildcards() {
        assertEquals("a\\%b\\_c", StringUtil.sanitizeSearchKeyword("a%b_c"));
        assertEquals("keyword", StringUtil.sanitizeSearchKeyword("  keyword  "));
    }

    @Test
    void sanitizeRejectsEmptyAndOverlong() {
        assertNull(StringUtil.sanitizeSearchKeyword(null));
        assertNull(StringUtil.sanitizeSearchKeyword("   "));
        assertNull(StringUtil.sanitizeSearchKeyword("x".repeat(51)));
    }

    @Test
    void fulltextSanitizeStripsBooleanOperators() {
        assertEquals("java spring", StringUtil.sanitizeFulltextKeyword("+java -spring"));
        assertEquals("考试资料", StringUtil.sanitizeFulltextKeyword("考试资料"));
        assertNull(StringUtil.sanitizeFulltextKeyword("+++"));
        assertNull(StringUtil.sanitizeFulltextKeyword(null));
        assertNull(StringUtil.sanitizeFulltextKeyword("x".repeat(51)));
    }

    @Test
    void bcryptRoundTrip() {
        String encoded = StringUtil.BCryptPassword("secret123");
        assertTrue(StringUtil.judgePassword("secret123", encoded));
        assertFalse(StringUtil.judgePassword("wrong", encoded));
    }

    @Test
    void emailValidation() {
        assertTrue(StringUtil.checkEmail("a_b-c@example.com"));
        assertFalse(StringUtil.checkEmail("not-an-email"));
    }
}