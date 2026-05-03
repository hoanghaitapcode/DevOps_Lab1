package com.yas.commonlibrary.viewmodel.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorVmTest {

    @Test
    void record_threeArg_constructor_setsEmptyFieldErrors() {
        ErrorVm vm = new ErrorVm("404", "Not Found", "The item was not found");
        assertEquals("404", vm.statusCode());
        assertEquals("Not Found", vm.title());
        assertEquals("The item was not found", vm.detail());
        assertNotNull(vm.fieldErrors());
        assertTrue(vm.fieldErrors().isEmpty());
    }

    @Test
    void record_fourArg_constructor_keepsFieldErrors() {
        ErrorVm vm = new ErrorVm("400", "Bad", "Bad request", List.of("f1", "f2"));
        assertEquals(2, vm.fieldErrors().size());
        assertEquals("f1", vm.fieldErrors().get(0));
    }
}
