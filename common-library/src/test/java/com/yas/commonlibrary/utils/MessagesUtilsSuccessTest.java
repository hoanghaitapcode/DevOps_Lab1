package com.yas.commonlibrary.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesUtilsSuccessTest {

    @Test
    void getMessage_whenKeyExists_returnsFormattedMessage() {
        String res = MessagesUtils.getMessage("greet", "World");
        assertEquals("Hello World", res);

        String res2 = MessagesUtils.getMessage("item.count", 5);
        assertEquals("You have 5 items", res2);
    }
}
