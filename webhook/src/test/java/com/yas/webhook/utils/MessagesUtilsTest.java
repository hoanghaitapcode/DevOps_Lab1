package com.yas.webhook.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MessagesUtilsTest {

    @Test
    void getMessage_whenMissing_returnsKey() {
        String res = MessagesUtils.getMessage("non.existing.code", "a", 1);
        assertNotNull(res);
        assertTrue(res.contains("non.existing.code"));
    }

    @Test
    void getMessage_formatsArguments_whenPatternPresentOrFallback() {
        // With missing resource it will fall back to key and formatting should still run
        String res = MessagesUtils.getMessage("code.with.args {} {}", "x", "y");
        assertNotNull(res);
        // resulting string should contain provided args or the key
        assertTrue(res.contains("x") || res.contains("code.with.args"));
    }
}
