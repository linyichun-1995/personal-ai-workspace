package com.example.workspace.search;

import static org.junit.jupiter.api.Assertions.*;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.search.application.SearchText;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SearchTextTest {
    @Test void shortChineseAndMixedUnicodeAreNotLost() {
        assertTrue(Arrays.asList(SearchText.bigrams("缓存ＡＩ😀方案")).containsAll(Arrays.asList(SearchText.bigrams("缓存","ai","😀方"))));
        assertFalse(Arrays.asList(SearchText.bigrams("缓存","方案")).contains("存方"));
        assertThrows(AppException.class,()->SearchText.terms("缓"));
        assertEquals(java.util.List.of("缓存","ai"),SearchText.terms(" 缓存　ＡＩ "));
        assertEquals(java.util.List.of("%_"),SearchText.terms("%_"));
    }
    @Test void snippetsStayPlainAndBounded() {
        String text="<script>缓存</script>";
        assertEquals(text,SearchText.snippet(text,java.util.List.of("缓存")).stream().map(SearchText.Segment::text).collect(java.util.stream.Collectors.joining()));
        assertEquals(240,SearchText.snippet("😀".repeat(300),java.util.List.of()).getFirst().text().codePointCount(0,480));
    }

    @Test void highlightsNormalizedHitsAtTheirOriginalOffsets() {
        String prefix = "前文".repeat(200) + "\n  ";
        String raw = prefix + "ＡＩ 缓存 Cafe\u0301 😀方案 ABC\t\n   DEF";
        var segments = SearchText.snippet(raw, SearchText.terms("ai 缓存 café 😀方 abc def"));
        String excerpt = segments.stream().map(SearchText.Segment::text).collect(java.util.stream.Collectors.joining());
        String marked = segments.stream().filter(SearchText.Segment::matched).map(SearchText.Segment::text)
                .collect(java.util.stream.Collectors.joining("|"));
        assertTrue(excerpt.contains("ＡＩ 缓存 Cafe\u0301 😀方案 ABC\t\n   DEF"));
        assertEquals("ＡＩ|缓存|Cafe\u0301|😀方|ABC|DEF", marked);
        assertTrue(excerpt.codePointCount(0, excerpt.length()) <= 240);
        assertTrue(raw.contains(excerpt));
    }

    @Test void compatibilityExpansionAndOverlappingMatchesKeepWholeOriginalCharacters() {
        var segments = SearchText.snippet("资料 ﬃ ＡＢＣＤ 缓存缓存", java.util.List.of("ffi", "ab", "bc", "缓存"));
        assertEquals("ﬃ|ＡＢＣ|缓存缓存", segments.stream().filter(SearchText.Segment::matched)
                .map(SearchText.Segment::text).collect(java.util.stream.Collectors.joining("|")));
        assertEquals("资料 ﬃ ＡＢＣＤ 缓存缓存", segments.stream().map(SearchText.Segment::text)
                .collect(java.util.stream.Collectors.joining()));
    }
}
