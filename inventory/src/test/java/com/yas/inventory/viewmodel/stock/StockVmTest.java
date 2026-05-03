package com.yas.inventory.viewmodel.stock;

import static org.junit.jupiter.api.Assertions.*;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import org.junit.jupiter.api.Test;

class StockVmTest {

    @Test
    void fromModel_mapsFieldsCorrectly() {
        Warehouse wh = Warehouse.builder().id(3L).name("w").addressId(9L).build();
        Stock s = Stock.builder().id(10L).productId(55L).quantity(100L).reservedQuantity(5L).warehouse(wh).build();
        ProductInfoVm p = new ProductInfoVm(55L, "product name", "SKU-1", true);

        StockVm vm = StockVm.fromModel(s, p);

        assertEquals(10L, vm.id());
        assertEquals(55L, vm.productId());
        assertEquals("product name", vm.productName());
        assertEquals("SKU-1", vm.productSku());
        assertEquals(100L, vm.quantity());
        assertEquals(5L, vm.reservedQuantity());
        assertEquals(3L, vm.warehouseId());
    }
}
