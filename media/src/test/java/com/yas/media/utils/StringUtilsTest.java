package com.yas.media.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void hasText_whenNull_thenFalse() {
        assertThat(StringUtils.hasText(null)).isFalse();
    }

    @Test
    void hasText_whenBlank_thenFalse() {
        assertThat(StringUtils.hasText("   ")).isFalse();
    }

    @Test
    void hasText_whenText_thenTrue() {
        assertThat(StringUtils.hasText("media")).isTrue();
    }
}
