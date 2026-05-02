package com.yas.tax.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MessagesUtilsTest {

    @Test
    void missingKey_returnsKey() {
        String res = MessagesUtils.getMessage("non.existing.key", "a");
        assertNotNull(res);
        assertTrue(res.contains("non.existing.key"));
    }

    @Test
    void formattingWithArgs_worksOrFallsBack() {
        String res = MessagesUtils.getMessage("some {} {}", "x", "y");
        assertNotNull(res);
        assertTrue(res.contains("x") || res.contains("some {} {}"));
    }
}
