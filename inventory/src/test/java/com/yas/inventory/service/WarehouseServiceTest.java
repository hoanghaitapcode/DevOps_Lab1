package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarehouseServiceTest {

    WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    StockRepository stockRepository = mock(StockRepository.class);
    ProductService productService = mock(ProductService.class);
    LocationService locationService = mock(LocationService.class);

    WarehouseService svc;

    @BeforeEach
    void setUp() {
        svc = new WarehouseService(warehouseRepository, stockRepository, productService, locationService);
    }

    @Test
    void findAllWarehouses_maps() {
        Warehouse w = Warehouse.builder().id(7L).name("wh").addressId(11L).build();
        when(warehouseRepository.findAll()).thenReturn(List.of(w));

        List<WarehouseGetVm> res = svc.findAllWarehouses();
        assertEquals(1, res.size());
        assertEquals(7L, res.get(0).id());
    }

    @Test
    void getProductWarehouse_marksExistWhenProductIdsContains() {
        when(stockRepository.getProductIdsInWarehouse(3L)).thenReturn(List.of(2L));
        ProductInfoVm pv1 = new ProductInfoVm(2L, "n", "s", false);
        ProductInfoVm pv2 = new ProductInfoVm(3L, "n2", "s2", false);
        when(productService.filterProducts("nm", "sku", List.of(2L), null)).thenReturn(List.of(pv1, pv2));

        List<ProductInfoVm> out = svc.getProductWarehouse(3L, "nm", "sku", null);
        assertEquals(2, out.size());
        // first should be marked existInWh true
        assertTrue(out.stream().anyMatch(p -> p.id() == 2L && p.existInWh()));
    }

    @Test
    void findById_returnsDetailVm() {
        Warehouse w = Warehouse.builder().id(5L).name("W").addressId(22L).build();
        when(warehouseRepository.findById(5L)).thenReturn(Optional.of(w));
        AddressDetailVm addr = AddressDetailVm.builder().id(22L).contactName("c").phone("p").addressLine1("a").city("ci").zipCode("z").districtId(1L).districtName("d").stateOrProvinceId(1L).stateOrProvinceName("s").countryId(1L).countryName("co").build();
        when(locationService.getAddressById(22L)).thenReturn(addr);

        var detail = svc.findById(5L);
        assertEquals(5L, detail.id());
        assertEquals("c", detail.contactName());
    }

    @Test
    void create_whenNameExists_throws() {
        WarehousePostVm post = new WarehousePostVm(null, "n", "c", "p", "a1", "a2", "ci", "z", 1L, 1L, 1L);
        when(warehouseRepository.existsByName("n")).thenReturn(true);
        assertThrows(DuplicatedException.class, () -> svc.create(post));
    }

    @Test
    void delete_findsAndDeletesAndDeletesAddress() {
        Warehouse w = Warehouse.builder().id(9L).name("wh").addressId(33L).build();
        when(warehouseRepository.findById(9L)).thenReturn(Optional.of(w));

        svc.delete(9L);

        verify(warehouseRepository).deleteById(9L);
        verify(locationService).deleteAddress(33L);
    }
}
