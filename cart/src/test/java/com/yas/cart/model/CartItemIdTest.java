package com.yas.cart.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CartItemIdTest {

    @Test
    void equalsAndHashCode_shouldBeEqualForSameValues() {
        CartItemId first = new CartItemId("u1", 1L);
        CartItemId second = new CartItemId("u1", 1L);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalsAndHashCode_shouldNotBeEqualForDifferentValues() {
        CartItemId first = new CartItemId("u1", 1L);
        CartItemId second = new CartItemId("u1", 2L);

        assertNotEquals(first, second);
    }

    @Test
    void noArgsAndSetters_shouldPopulateFields() {
        CartItemId id = new CartItemId();

        id.setCustomerId("u2");
        id.setProductId(10L);

        assertEquals("u2", id.getCustomerId());
        assertEquals(10L, id.getProductId());
    }
}
