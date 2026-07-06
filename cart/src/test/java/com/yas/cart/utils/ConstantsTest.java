package com.yas.cart.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void errorCodes_shouldMatchExpectedValues() {
        assertEquals("NOT_FOUND_PRODUCT", Constants.ErrorCode.NOT_FOUND_PRODUCT);
        assertEquals("NOT_EXISTING_ITEM_IN_CART", Constants.ErrorCode.NOT_EXISTING_ITEM_IN_CART);
        assertEquals("NOT_EXISTING_PRODUCT_IN_CART", Constants.ErrorCode.NOT_EXISTING_PRODUCT_IN_CART);
        assertEquals("NON_EXISTING_CART_ITEM", Constants.ErrorCode.NON_EXISTING_CART_ITEM);
        assertEquals("ADD_CART_ITEM_FAILED", Constants.ErrorCode.ADD_CART_ITEM_FAILED);
        assertEquals("DUPLICATED_CART_ITEMS_TO_DELETE", Constants.ErrorCode.DUPLICATED_CART_ITEMS_TO_DELETE);
    }
}
