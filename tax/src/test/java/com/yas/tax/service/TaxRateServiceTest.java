package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class TaxRateServiceTest {

    @Mock
    TaxRateRepository taxRateRepository;
    @Mock
    TaxClassRepository taxClassRepository;
    @Mock
    LocationService locationService;

    TaxRateService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TaxRateService(locationService, taxRateRepository, taxClassRepository);
    }

    @Test
    void createTaxRate_whenTaxClassMissing_throws() {
        TaxRatePostVm post = new TaxRatePostVm(0.1, "z", 1L, 2L, 3L);
        when(taxClassRepository.existsById(1L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.createTaxRate(post));
    }

    @Test
    void createTaxRate_success_saves() {
        TaxRatePostVm post = new TaxRatePostVm(0.1, "z", 1L, 2L, 3L);
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        TaxClass tc = new TaxClass(); tc.setId(1L); tc.setName("c");
        when(taxClassRepository.getReferenceById(1L)).thenReturn(tc);
        TaxRate saved = new TaxRate();
        saved.setId(5L);
        saved.setRate(0.1);
        saved.setZipCode("z");
        saved.setCountryId(3L);
        saved.setTaxClass(tc);
        saved.setStateOrProvinceId(2L);
        when(taxRateRepository.save(any())).thenReturn(saved);

        var res = service.createTaxRate(post);
        assertEquals(5L, res.getId());
    }

    @Test
    void updateTaxRate_whenNotFound_throws() {
        TaxRatePostVm post = new TaxRatePostVm(0.1, "z", 1L, 2L, 3L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.updateTaxRate(post, 10L));
    }

    @Test
    void delete_whenNotExists_throws() {
        when(taxRateRepository.existsById(9L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.delete(9L));
    }

    @Test
    void findById_missing_throws() {
        when(taxRateRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById(8L));
    }

    @Test
    void findAll_mapsToVm() {
        TaxClass tc = new TaxClass(); tc.setId(2L); tc.setName("c");
        TaxRate tr = new TaxRate();
        tr.setId(7L);
        tr.setRate(0.2);
        tr.setZipCode("x");
        tr.setTaxClass(tc);
        tr.setCountryId(1L);
        tr.setStateOrProvinceId(11L);
        doReturn(List.of(tr)).when(taxRateRepository).findAll();
        var res = service.findAll();
        assertEquals(1, res.size());
        assertEquals(7L, res.get(0).id());
    }

    @Test
    void getPageableTaxRates_mapsWithLocationService() {
        TaxClass tc = new TaxClass(); tc.setId(2L); tc.setName("c");
        TaxRate tr = new TaxRate();
        tr.setId(7L);
        tr.setRate(0.2);
        tr.setZipCode("x");
        tr.setTaxClass(tc);
        tr.setCountryId(1L);
        tr.setStateOrProvinceId(11L);
        var page = new PageImpl<>(List.of(tr), PageRequest.of(0,1), 1);
        when(taxRateRepository.findAll(any(PageRequest.class))).thenReturn(page);

        StateOrProvinceAndCountryGetNameVm vm = new StateOrProvinceAndCountryGetNameVm(11L, "S", "C");
        when(locationService.getStateOrProvinceAndCountryNames(List.of(11L))).thenReturn(List.of(vm));

        TaxRateListGetVm listVm = service.getPageableTaxRates(0,1);
        assertNotNull(listVm);
        assertEquals(1, listVm.taxRateGetDetailContent().size());
        TaxRateGetDetailVm detail = listVm.taxRateGetDetailContent().get(0);
        assertEquals("S", detail.stateOrProvinceName());
    }

    @Test
    void getTaxPercent_returnsValueOrZero() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "z", 3L)).thenReturn(0.15);
        assertEquals(0.15, service.getTaxPercent(3L,1L,2L,"z"));
        when(taxRateRepository.getTaxPercent(1L,2L,"z",3L)).thenReturn(null);
        assertEquals(0.0, service.getTaxPercent(3L,1L,2L,"z"));
    }

    @Test
    void getBulkTaxRate_mapsBatch() {
        TaxClass tc = new TaxClass(); tc.setId(2L); tc.setName("c");
        TaxRate tr = TaxRate.builder().id(7L).rate(0.2).zipCode("x").taxClass(tc).countryId(1L).stateOrProvinceId(11L).build();
        when(taxRateRepository.getBatchTaxRates(1L, 11L, "z", Set.of(2L))).thenReturn(List.of(tr));

        var res = service.getBulkTaxRate(List.of(2L), 1L, 11L, "z");
        assertEquals(1, res.size());
    }
}
