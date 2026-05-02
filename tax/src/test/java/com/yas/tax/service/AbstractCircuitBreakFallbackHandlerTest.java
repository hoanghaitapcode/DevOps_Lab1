package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    static class TestHandler extends AbstractCircuitBreakFallbackHandler {
        void exposeBodiless(Throwable t) throws Throwable { handleBodilessFallback(t); }
        Object exposeTyped(Throwable t) throws Throwable { return handleTypedFallback(t); }
    }

    @Test
    void bodiless_rethrowsOriginal() {
        TestHandler h = new TestHandler();
        Throwable ex = new RuntimeException("boom");
        assertThrows(RuntimeException.class, () -> h.exposeBodiless(ex));
    }

    @Test
    void typed_rethrowsOriginal() {
        TestHandler h = new TestHandler();
        Throwable ex = new IllegalStateException("err");
        assertThrows(IllegalStateException.class, () -> h.exposeTyped(ex));
    }
}
