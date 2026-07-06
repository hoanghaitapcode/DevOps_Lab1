package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductConverterTest {

    @Test
    void toSlug_shouldNormalizeAndTrim() {
        String input = "  MacBook Pro 2026!  ";
        String slug = ProductConverter.toSlug(input);

        assertThat(slug).isEqualTo("macbook-pro-2026-");
    }

    @Test
    void toSlug_shouldCollapseMultipleDashesAndRemoveLeadingDash() {
        String input = " @@Starting--with###bad--chars";
        String slug = ProductConverter.toSlug(input);

        // expected flow: non-alnum -> '-', collapse '--' -> '-', leading '-' removed
        assertThat(slug).isEqualTo("starting-with-bad-chars");
    }
}
