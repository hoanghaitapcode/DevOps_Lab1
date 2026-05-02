package com.yas.cart.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private final TestHandler handler = new TestHandler();

    @Test
    void handleTypedFallback_shouldRethrowOriginalThrowable() {
        RuntimeException exception = new RuntimeException("boom");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.callTyped(exception));

        assertSame(exception, thrown);
    }

    @Test
    void handleBodilessFallback_shouldRethrowOriginalThrowable() {
        IllegalStateException exception = new IllegalStateException("downstream error");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> handler.callBodiless(exception));

        assertSame(exception, thrown);
    }

    private static final class TestHandler extends AbstractCircuitBreakFallbackHandler {
        String callTyped(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }

        void callBodiless(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }
    }
}
