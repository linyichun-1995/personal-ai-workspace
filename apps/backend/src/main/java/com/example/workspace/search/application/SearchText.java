package com.example.workspace.search.application;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.exception.AppException;
import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.*;
import org.springframework.http.HttpStatus;

/** The same normalization and Unicode code-point bigrams are used at write and query time. */
public final class SearchText {
    private SearchText() {}
    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("(?U)\\s+", " ").strip();
    }
    public static List<String> terms(String input) {
        String q = normalize(input);
        if (q.isEmpty()) return List.of();
        var terms = List.of(q.split(" "));
        if (q.codePointCount(0,q.length()) > 100 || terms.size()>8
                || terms.stream().anyMatch(t -> t.codePointCount(0,t.length())<2))
            throw new AppException(ErrorCode.SEARCH_QUERY_INVALID, HttpStatus.BAD_REQUEST,"每个关键词至少 2 个字符，最多 8 项、共 100 个字符");
        return terms;
    }
    public static String[] bigrams(String... fields) {
        Set<String> result = new TreeSet<>();
        for (String field : fields) {
            int[] points = normalize(field).codePoints().toArray();
            for (int i=1;i<points.length;i++) result.add(new String(points,i-1,2));
        }
        return result.toArray(String[]::new);
    }
    public record Segment(String text, boolean matched) {}
    private record Span(int start, int end) {}
    private record MappedText(String text, List<Span> spans) {}
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private static final Pattern SPACE = Pattern.compile("(?U)\\s+");

    /** Map normalized UTF-16 offsets back to complete original grapheme clusters. */
    private static MappedText mapped(String raw) {
        var clusters = GRAPHEME.matcher(raw);
        StringBuilder expanded = new StringBuilder();
        List<Span> offsets = new ArrayList<>();
        while (clusters.find()) {
            String part = Normalizer.normalize(clusters.group(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
            expanded.append(part);
            Span span = new Span(clusters.start(), clusters.end());
            for (int i = 0; i < part.length(); i++) offsets.add(span);
        }
        // Whole-string lowercasing preserves contextual forms such as Greek final sigma.
        String lowered = Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if (lowered.length() != offsets.size()) lowered = expanded.toString();
        StringBuilder text = new StringBuilder();
        List<Span> spans = new ArrayList<>();
        var whitespace = SPACE.matcher(lowered);
        int offset = 0;
        while (whitespace.find()) {
            text.append(lowered, offset, whitespace.start());
            spans.addAll(offsets.subList(offset, whitespace.start()));
            if (!text.isEmpty() && whitespace.end() < lowered.length()) {
                text.append(' ');
                spans.add(new Span(offsets.get(whitespace.start()).start(), offsets.get(whitespace.end() - 1).end()));
            }
            offset = whitespace.end();
        }
        text.append(lowered, offset, lowered.length());
        spans.addAll(offsets.subList(offset, lowered.length()));
        return new MappedText(text.toString(), spans);
    }

    public static List<Segment> snippet(String raw, List<String> terms) {
        if (raw == null || raw.isEmpty()) return List.of();
        if (terms.isEmpty()) return List.of(new Segment(take(raw, 240), false));
        var mapped = mapped(raw);
        List<Span> hits = new ArrayList<>();
        for (String term : terms) {
            if (term.isEmpty()) continue;
            int at = mapped.text().indexOf(term);
            while (at >= 0) {
                hits.add(new Span(mapped.spans().get(at).start(), mapped.spans().get(at + term.length() - 1).end()));
                at = mapped.text().indexOf(term, at + 1);
            }
        }
        hits.sort(Comparator.comparingInt(Span::start));
        int hit = hits.isEmpty() ? 0 : hits.getFirst().start();
        int start = raw.offsetByCodePoints(0, Math.max(0, raw.codePointCount(0, hit) - 60));
        int end = raw.offsetByCodePoints(start, Math.min(240, raw.codePointCount(start, raw.length())));
        List<Segment> segments = new ArrayList<>();
        int cursor = start;
        for (int i = 0; i < hits.size(); i++) {
            Span current = hits.get(i);
            if (current.end() <= start || current.start() >= end) continue;
            int from = Math.max(start, current.start());
            int until = Math.min(end, current.end());
            while (i + 1 < hits.size() && hits.get(i + 1).start() <= until) {
                until = Math.min(end, Math.max(until, hits.get(++i).end()));
            }
            if (from > cursor) segments.add(new Segment(raw.substring(cursor, from), false));
            segments.add(new Segment(raw.substring(from, until), true));
            cursor = until;
        }
        if (cursor < end) segments.add(new Segment(raw.substring(cursor, end), false));
        return segments;
    }
    public static String take(String value,int count) {
        return value.substring(0,value.offsetByCodePoints(0,Math.min(count,value.codePointCount(0,value.length()))));
    }
}
