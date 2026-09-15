package com.example.workspace.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void usesDefaultsAndCapsSize() {
        PageQuery query = new PageQuery(null, 10_000, null);
        assertThat(query.pageOrDefault()).isEqualTo(1);
        assertThat(query.sizeOrDefault()).isEqualTo(PageQuery.MAX_SIZE);
        assertThat(query.offset()).isZero();
    }
}
