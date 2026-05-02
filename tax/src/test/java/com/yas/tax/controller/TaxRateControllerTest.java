package com.yas.tax.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yas.tax.model.TaxRate;
import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

class TaxRateControllerTest {

    TaxRateService service = mock(TaxRateService.class);
    TaxRateController controller;

    @BeforeEach
    void setUp() {
        controller = new TaxRateController(service);
    }

    @Test
    void list_and_get_and_create_update_delete_flow() {
        // test pageable listing
        TaxRateGetDetailVm detail = new TaxRateGetDetailVm(1L, 0.1, "z", "c", "s", "co");
        TaxRateListGetVm listVm = new TaxRateListGetVm(List.of(detail), 0, 10, 1, 1, true);
        when(service.getPageableTaxRates(0, 10)).thenReturn(listVm);
        var r1 = controller.getPageableTaxRates(0, 10);
        assertEquals(200, r1.getStatusCode().value());

        when(service.findById(2L)).thenReturn(new TaxRateVm(2L, 0.2, "x", 2L, 3L, 4L));
        ResponseEntity<TaxRateVm> r2 = controller.getTaxRate(2L);
        assertEquals(200, r2.getStatusCode().value());

        TaxRate tr = new TaxRate(); tr.setId(5L); tr.setZipCode("p");
        TaxClass tc = new TaxClass(); tc.setId(7L); tr.setTaxClass(tc);
        when(service.createTaxRate(any())).thenReturn(tr);
        ResponseEntity<TaxRateVm> r3 = controller.createTaxRate(new TaxRatePostVm(0.1, "z", 2L, 3L, 4L), UriComponentsBuilder.fromPath(""));
        assertEquals(201, r3.getStatusCode().value());

        ResponseEntity<Void> r4 = controller.updateTaxRate(5L, new TaxRatePostVm(0.2, "y", 2L, 3L, 4L));
        verify(service).updateTaxRate(any(), eq(5L));
        assertEquals(204, r4.getStatusCode().value());

        ResponseEntity<Void> r5 = controller.deleteTaxRate(6L);
        verify(service).delete(6L);
        assertEquals(204, r5.getStatusCode().value());
    }
}
