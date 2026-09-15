package com.example.workspace.common.util;

public final class MarkdownSummaries {

    public static final int MAX_LENGTH = 500;

    private MarkdownSummaries() {
    }

    public static String from(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = content
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1")
                .replaceAll("[#*_>~]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH);
    }
}
