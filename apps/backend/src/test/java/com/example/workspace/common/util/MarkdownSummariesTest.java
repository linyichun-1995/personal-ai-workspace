package com.example.workspace.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownSummariesTest {

    @Test
    void stripsMarkdownAndTruncates() {
        assertThat(MarkdownSummaries.from(null)).isEmpty();
        assertThat(MarkdownSummaries.from("# 标题\n\n**加粗** 和 [链接](https://example.com)")).isEqualTo("标题 加粗 和 链接");
    }
}
