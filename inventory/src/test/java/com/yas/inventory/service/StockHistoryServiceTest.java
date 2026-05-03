package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StockHistoryServiceTest {

    StockHistoryRepository stockHistoryRepository = mock(StockHistoryRepository.class);
    ProductService productService = mock(ProductService.class);
    StockHistoryService svc;

    @BeforeEach
    void setUp() {
        svc = new StockHistoryService(stockHistoryRepository, productService);
    }

    @Test
    void createStockHistories_savesOnlyMatching() {
        Warehouse w = Warehouse.builder().id(1L).name("w").addressId(2L).build();
        Stock s = Stock.builder().id(10L).productId(55L).quantity(3L).warehouse(w).build();

        StockQuantityVm vm = new StockQuantityVm(10L, 4L, "n");

        svc.createStockHistories(List.of(s), List.of(vm));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(captor.capture());
        List<StockHistory> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(4L, saved.get(0).getAdjustedQuantity());
    }

    @Test
    void getStockHistories_mapsToVm() {
        Warehouse w = Warehouse.builder().id(1L).name("w").addressId(2L).build();
        StockHistory sh = StockHistory.builder().id(99L).productId(77L).adjustedQuantity(5L).note("note").warehouse(w).build();
        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(77L, 1L)).thenReturn(List.of(sh));
        when(productService.getProduct(77L)).thenReturn(new ProductInfoVm(77L, "pname", "sku", true));

        var res = svc.getStockHistories(77L, 1L);
        assertEquals(1, res.data().size());
        assertEquals("pname", res.data().get(0).productName());
    }
}
