package com.yas.tax.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

class TaxClassControllerTest {

    TaxClassService service = mock(TaxClassService.class);
    TaxClassController controller;

    @BeforeEach
    void setUp() {
        controller = new TaxClassController(service);
    }

    @Test
    void listTaxClasses_returnsOk() {
        when(service.findAllTaxClasses()).thenReturn(List.of(new TaxClassVm(1L, "n")));
        ResponseEntity<List<TaxClassVm>> res = controller.listTaxClasses();
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getTaxClass_returnsDetail() {
        when(service.findById(2L)).thenReturn(new TaxClassVm(2L, "x"));
        ResponseEntity<TaxClassVm> res = controller.getTaxClass(2L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(2L, res.getBody().id());
    }

    @Test
    void createTaxClass_returnsCreated() {
        TaxClass tc = new TaxClass(); tc.setId(10L); tc.setName("n");
        when(service.create(any())).thenReturn(tc);
        ResponseEntity<TaxClassVm> res = controller.createTaxClass(new TaxClassPostVm(null, "n"), UriComponentsBuilder.fromPath(""));
        assertEquals(201, res.getStatusCode().value());
        assertEquals("n", res.getBody().name());
    }

    @Test
    void update_and_delete_returnNoContent_andCallService() {
        TaxClassPostVm post = new TaxClassPostVm(null, "n");
        ResponseEntity<Void> r1 = controller.updateTaxClass(3L, post);
        verify(service).update(post, 3L);
        assertEquals(204, r1.getStatusCode().value());

        ResponseEntity<Void> r2 = controller.deleteTaxClass(5L);
        verify(service).delete(5L);
        assertEquals(204, r2.getStatusCode().value());
    }
}
