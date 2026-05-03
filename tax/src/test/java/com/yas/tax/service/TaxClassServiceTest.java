package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class TaxClassServiceTest {

    @Mock
    TaxClassRepository taxClassRepository;

    TaxClassService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TaxClassService(taxClassRepository);
    }

    @Test
    void findAllTaxClasses_returnsMappedList() {
        TaxClass tc = new TaxClass(); tc.setId(1L); tc.setName("A");
        doReturn(List.of(tc)).when(taxClassRepository).findAll(any(Sort.class));

        var res = service.findAllTaxClasses();
        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).id());
    }

    @Test
    void findById_missing_throws() {
        when(taxClassRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById(5L));
    }

    @Test
    void create_whenNameExists_throwsDuplicated() {
        TaxClassPostVm post = new TaxClassPostVm(null, "name");
        when(taxClassRepository.existsByName("name")).thenReturn(true);
        assertThrows(DuplicatedException.class, () -> service.create(post));
    }

    @Test
    void create_success_callsSave() {
        TaxClassPostVm post = new TaxClassPostVm(null, "name");
        TaxClass model = post.toModel(); model.setId(2L);
        when(taxClassRepository.existsByName("name")).thenReturn(false);
        when(taxClassRepository.save(any())).thenReturn(model);

        var res = service.create(post);
        assertEquals(2L, res.getId());
    }

    @Test
    void update_whenNotFound_throws() {
        TaxClassPostVm post = new TaxClassPostVm(null, "n");
        when(taxClassRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.update(post, 9L));
    }

    @Test
    void update_whenNameConflicts_throws() {
        TaxClassPostVm post = new TaxClassPostVm(null, "n");
        TaxClass existing = new TaxClass(); existing.setId(3L); existing.setName("old");
        when(taxClassRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("n", 3L)).thenReturn(true);
        assertThrows(DuplicatedException.class, () -> service.update(post, 3L));
    }

    @Test
    void update_success_saves() {
        TaxClassPostVm post = new TaxClassPostVm(null, "n");
        TaxClass existing = new TaxClass(); existing.setId(4L); existing.setName("old");
        when(taxClassRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("n", 4L)).thenReturn(false);

        service.update(post, 4L);
        verify(taxClassRepository).save(existing);
        assertEquals("n", existing.getName());
    }

    @Test
    void delete_whenNotExists_throws() {
        when(taxClassRepository.existsById(7L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.delete(7L));
    }

    @Test
    void getPageableTaxClasses_buildsVm() {
        TaxClass t1 = new TaxClass(); t1.setId(1L); t1.setName("x");
        var page = new PageImpl<>(List.of(t1), PageRequest.of(0,1), 1);
        when(taxClassRepository.findAll(any(PageRequest.class))).thenReturn(page);

        TaxClassListGetVm vm = service.getPageableTaxClasses(0,1);
        assertNotNull(vm);
        assertEquals(1, vm.taxClassContent().size());
    }
}
