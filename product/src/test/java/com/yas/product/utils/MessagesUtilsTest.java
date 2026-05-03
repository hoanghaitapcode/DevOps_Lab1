package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenKeyExists_thenFormatMessage() {
        String msg = MessagesUtils.getMessage("PRODUCT_NOT_FOUND", "123");
        assertThat(msg).contains("Product");
        assertThat(msg).contains("123");
    }

    @Test
    void getMessage_whenKeyMissing_thenReturnKey() {
        String msg = MessagesUtils.getMessage("NON_EXISTENT_CODE", "x");
        assertThat(msg).isEqualTo("NON_EXISTENT_CODE");
    }
}
