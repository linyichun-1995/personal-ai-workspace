package com.example.workspace.tag;

import static org.junit.jupiter.api.Assertions.*;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.tag.application.TagService;
import org.junit.jupiter.api.Test;

class TagNamesTest {
    @Test void normalizationAndUnicodeLimits() {
        assertEquals("ai 方案", TagService.normalizedName(" ＡＩ 方案 "));
        assertEquals("😀".repeat(30),TagService.displayName("😀".repeat(30)));
        assertThrows(AppException.class, () -> TagService.displayName("😀".repeat(31)));
        assertThrows(AppException.class, () -> TagService.displayName("a\nb"));
        assertThrows(AppException.class, () -> TagService.displayName("　"));
    }
}
