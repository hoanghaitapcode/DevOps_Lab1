package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.product.ProductQuantityPostVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    StockRepository stockRepository = mock(StockRepository.class);
    ProductService productService = mock(ProductService.class);
    WarehouseService warehouseService = mock(WarehouseService.class);
    StockHistoryService stockHistoryService = mock(StockHistoryService.class);

    StockService svc;

    @BeforeEach
    void setUp() {
        svc = new StockService(warehouseRepository, stockRepository, productService, warehouseService, stockHistoryService);
    }

    @Test
    void addProductIntoWarehouse_existing_throws() {
        // StockPostVm(productId, warehouseId)
        StockPostVm p = new StockPostVm(2L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 2L)).thenReturn(true);

        assertThrows(StockExistingException.class, () -> svc.addProductIntoWarehouse(List.of(p)));
    }

    @Test
    void addProductIntoWarehouse_productNotFound_throws() {
        StockPostVm p = new StockPostVm(2L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 2L)).thenReturn(false);
        when(productService.getProduct(2L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> svc.addProductIntoWarehouse(List.of(p)));
    }

    @Test
    void addProductIntoWarehouse_warehouseNotFound_throws() {
        StockPostVm p = new StockPostVm(2L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 2L)).thenReturn(false);
        when(productService.getProduct(2L)).thenReturn(new ProductInfoVm(2L, "n", "s", true));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> svc.addProductIntoWarehouse(List.of(p)));
    }

    @Test
    void addProductIntoWarehouse_success_savesStocks() {
        StockPostVm p = new StockPostVm(2L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 2L)).thenReturn(false);
        when(productService.getProduct(2L)).thenReturn(new ProductInfoVm(2L, "n", "s", true));
        Warehouse w = Warehouse.builder().id(1L).name("w").addressId(5L).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(w));

        svc.addProductIntoWarehouse(List.of(p));

        verify(stockRepository).saveAll(anyList());
    }

    @Test
    void getStocksByWarehouse_mapsToVm() {
        ProductInfoVm p = new ProductInfoVm(2L, "nm", "sku", true);
        when(warehouseService.getProductWarehouse(eq(3L), eq("nm"), eq("sku"), any())).thenReturn(List.of(p));
        Warehouse w = Warehouse.builder().id(3L).name("wh").addressId(1L).build();
        Stock s = Stock.builder().id(10L).productId(2L).quantity(7L).reservedQuantity(1L).warehouse(w).build();
        when(stockRepository.findByWarehouseIdAndProductIdIn(eq(3L), anyList())).thenReturn(List.of(s));

        // call
        List<StockVm> result = svc.getStocksByWarehouseIdAndProductNameAndSku(3L, "nm", "sku");
        assertEquals(1, result.size());
        StockVm vm = result.get(0);
        assertEquals(10L, vm.id());
        assertEquals(2L, vm.productId());
    }

    @Test
    void updateProductQuantityInStock_updatesAndCallsHistoryAndProductService() {
        Warehouse w = Warehouse.builder().id(1L).name("w").addressId(2L).build();
        Stock s = Stock.builder().id(100L).productId(55L).quantity(10L).reservedQuantity(0L).warehouse(w).build();
        when(stockRepository.findAllById(List.of(100L))).thenReturn(List.of(s));

        StockQuantityVm sq = new StockQuantityVm(100L, 5L, "note");
        StockQuantityUpdateVm req = new StockQuantityUpdateVm(List.of(sq));

        svc.updateProductQuantityInStock(req);

        // quantity should be increased
        assertEquals(15L, s.getQuantity());
        verify(stockRepository).saveAll(List.of(s));
        verify(stockHistoryService).createStockHistories(List.of(s), List.of(sq));
        verify(productService).updateProductQuantity(anyList());
    }
}
