package com.yas.cart.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CartItemTest {

    @Test
    void builder_shouldCreateCartItemWithProvidedFields() {
        CartItem cartItem = CartItem.builder()
            .customerId("user-a")
            .productId(101L)
            .quantity(4)
            .build();

        assertEquals("user-a", cartItem.getCustomerId());
        assertEquals(101L, cartItem.getProductId());
        assertEquals(4, cartItem.getQuantity());
    }

    @Test
    void setters_shouldUpdateFields() {
        CartItem cartItem = new CartItem();

        cartItem.setCustomerId("user-b");
        cartItem.setProductId(202L);
        cartItem.setQuantity(8);

        assertEquals("user-b", cartItem.getCustomerId());
        assertEquals(202L, cartItem.getProductId());
        assertEquals(8, cartItem.getQuantity());
    }
}
