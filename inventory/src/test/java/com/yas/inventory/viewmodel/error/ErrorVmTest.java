package com.yas.inventory.viewmodel.error;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void threeArgCtor_initializesEmptyFieldErrors() {
        ErrorVm e = new ErrorVm("500", "title", "detail");
        assertEquals("500", e.statusCode());
        assertEquals("title", e.title());
        assertEquals("detail", e.detail());
        assertNotNull(e.fieldErrors());
        assertTrue(e.fieldErrors().isEmpty());
    }

    @Test
    void fullCtor_keepsProvidedFieldErrors() {
        List<String> errors = List.of("f1");
        ErrorVm e = new ErrorVm("400", "t", "d", errors);
        assertEquals(errors, e.fieldErrors());
    }
}
