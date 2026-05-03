package com.yas.commonlibrary.kafka.cdc;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class BaseCdcConsumerUnitTest {

    private static class TestConsumer extends BaseCdcConsumer<String, String> {
        // expose protected methods for testing via public wrappers
        public void callProcessMessage(String record, MessageHeaders headers, Consumer<String> consumer) {
            processMessage(record, headers, consumer);
        }

        public void callProcessMessage(String key, String value, MessageHeaders headers, BiConsumer<String, String> consumer) {
            processMessage(key, value, headers, consumer);
        }
    }

    @Test
    void processMessage_singleArg_callsConsumer() {
        TestConsumer c = new TestConsumer();
        Map<String, Object> map = new HashMap<>();
        map.put(KafkaHeaders.RECEIVED_KEY, "k-1");
        MessageHeaders headers = new MessageHeaders(map);

        AtomicBoolean invoked = new AtomicBoolean(false);
        c.callProcessMessage("value-1", headers, v -> invoked.set(true));

        assertTrue(invoked.get());
    }

    @Test
    void processMessage_keyValue_callsBiConsumer() {
        TestConsumer c = new TestConsumer();
        Map<String, Object> map = new HashMap<>();
        map.put(KafkaHeaders.RECEIVED_KEY, "k-2");
        MessageHeaders headers = new MessageHeaders(map);

        AtomicBoolean invoked = new AtomicBoolean(false);
        c.callProcessMessage("key-1", "value-2", headers, (k, v) -> invoked.set(k.equals("key-1") && v.equals("value-2")));

        assertTrue(invoked.get());
    }
}
