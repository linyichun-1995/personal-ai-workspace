package com.example.workspace.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void generatesVersion7VariantRfc4122() {
        UUID id = UuidV7.next();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void isTimeOrdered() {
        UUID first = UuidV7.next();
        UUID second = UuidV7.next();
        assertThat(first.getMostSignificantBits() >>> 16)
                .isLessThanOrEqualTo(second.getMostSignificantBits() >>> 16);
    }
}
