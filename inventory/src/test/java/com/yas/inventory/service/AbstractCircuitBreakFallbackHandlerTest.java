package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    static class TestHandler extends AbstractCircuitBreakFallbackHandler {
        // expose protected methods via public wrappers
        public void callBodiless(Throwable t) throws Throwable {
            handleBodilessFallback(t);
        }

        public Object callTyped(Throwable t) throws Throwable {
            return handleTypedFallback(t);
        }
    }

    @Test
    void handleBodilessFallback_rethrowsThrowable() {
        TestHandler h = new TestHandler();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> h.callBodiless(new RuntimeException("boom")));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void handleTypedFallback_rethrowsThrowable() {
        TestHandler h = new TestHandler();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> h.callTyped(new IllegalStateException("err")));
        assertEquals("err", ex.getMessage());
    }

    @Test
    void handleBodilessFallback_rethrowsCheckedException() {
        TestHandler h = new TestHandler();
        Exception ex = assertThrows(Exception.class, () -> h.callBodiless(new Exception("checked")));
        assertEquals("checked", ex.getMessage());
    }

    @Test
    void handleTypedFallback_rethrowsCheckedException() {
        TestHandler h = new TestHandler();
        Exception ex = assertThrows(Exception.class, () -> h.callTyped(new Exception("checked")));
        assertEquals("checked", ex.getMessage());
    }
}
