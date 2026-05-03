package com.yas.inventory.service;

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
        assertThrows(RuntimeException.class, () -> h.callBodiless(new RuntimeException("boom")));
    }

    @Test
    void handleTypedFallback_rethrowsThrowable() {
        TestHandler h = new TestHandler();
        assertThrows(IllegalStateException.class, () -> h.callTyped(new IllegalStateException("err")));
    }
}
