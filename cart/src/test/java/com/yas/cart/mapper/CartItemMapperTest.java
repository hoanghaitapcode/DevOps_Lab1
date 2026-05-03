package com.yas.cart.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.cart.model.CartItem;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartItemMapperTest {

    private final CartItemMapper mapper = new CartItemMapper();

    @Test
    void toGetVm_shouldMapEntityFields() {
        CartItem cartItem = CartItem.builder()
            .customerId("u1")
            .productId(10L)
            .quantity(2)
            .build();

        CartItemGetVm vm = mapper.toGetVm(cartItem);

        assertEquals("u1", vm.customerId());
        assertEquals(10L, vm.productId());
        assertEquals(2, vm.quantity());
    }

    @Test
    void toCartItem_fromPostVm_shouldMapRequestAndUser() {
        CartItemPostVm request = new CartItemPostVm(55L, 3);

        CartItem cartItem = mapper.toCartItem(request, "user-x");

        assertEquals("user-x", cartItem.getCustomerId());
        assertEquals(55L, cartItem.getProductId());
        assertEquals(3, cartItem.getQuantity());
    }

    @Test
    void toCartItem_fromExplicitValues_shouldCreateEntity() {
        CartItem cartItem = mapper.toCartItem("u2", 99L, 7);

        assertEquals("u2", cartItem.getCustomerId());
        assertEquals(99L, cartItem.getProductId());
        assertEquals(7, cartItem.getQuantity());
    }

    @Test
    void toGetVms_shouldMapAllEntities() {
        List<CartItem> entities = List.of(
            CartItem.builder().customerId("u1").productId(1L).quantity(1).build(),
            CartItem.builder().customerId("u1").productId(2L).quantity(2).build()
        );

        List<CartItemGetVm> result = mapper.toGetVms(entities);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).productId());
        assertEquals(2L, result.get(1).productId());
    }
}