package com.yas.webhook.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

public class HmacUtilsTest {

    @Test
    void hash_isDeterministic_andNotNull() throws NoSuchAlgorithmException, InvalidKeyException {
        String a = HmacUtils.hash("payload", "secret");
        String b = HmacUtils.hash("payload", "secret");
        assertNotNull(a);
        assertEquals(a, b);
    }

    @Test
    void hash_differsForDifferentInput() throws NoSuchAlgorithmException, InvalidKeyException {
        String a = HmacUtils.hash("payload1", "secret");
        String b = HmacUtils.hash("payload2", "secret");
        assertNotEquals(a, b);
    }
}
