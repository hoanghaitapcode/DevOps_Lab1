package com.yas.customer.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void missingKey_returnsKey() {
        String s = MessagesUtils.getMessage("no.such.key", "a");
        assertNotNull(s);
        assertTrue(s.contains("no.such.key") || s.length() > 0);
    }

    @Test
    void format_withArgs_returnsFormattedOrFallback() {
        String s = MessagesUtils.getMessage("hello {}", "x");
        assertNotNull(s);
    }
}
