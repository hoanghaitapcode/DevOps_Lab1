package com.yas.cart.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewModelRecordsTest {

    @Test
    void cartItemPostVm_builderShouldSetValues() {
        CartItemPostVm vm = CartItemPostVm.builder()
            .productId(1L)
            .quantity(2)
            .build();

        assertEquals(1L, vm.productId());
        assertEquals(2, vm.quantity());
    }

    @Test
    void cartItemDeleteVm_builderShouldSetValues() {
        CartItemDeleteVm vm = CartItemDeleteVm.builder()
            .productId(2L)
            .quantity(3)
            .build();

        assertEquals(2L, vm.productId());
        assertEquals(3, vm.quantity());
    }

    @Test
    void cartItemPutVm_constructorShouldSetQuantity() {
        CartItemPutVm vm = new CartItemPutVm(4);

        assertEquals(4, vm.quantity());
    }

    @Test
    void cartItemGetVm_builderShouldSetValues() {
        CartItemGetVm vm = CartItemGetVm.builder()
            .customerId("u1")
            .productId(10L)
            .quantity(5)
            .build();

        assertEquals("u1", vm.customerId());
        assertEquals(10L, vm.productId());
        assertEquals(5, vm.quantity());
    }

    @Test
    void productThumbnailVm_builderShouldSetValues() {
        ProductThumbnailVm vm = ProductThumbnailVm.builder()
            .id(99L)
            .name("p")
            .slug("p-slug")
            .thumbnailUrl("thumb")
            .build();

        assertEquals(99L, vm.id());
        assertEquals("p", vm.name());
        assertEquals("p-slug", vm.slug());
        assertEquals("thumb", vm.thumbnailUrl());
    }
}
