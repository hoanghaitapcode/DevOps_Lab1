package com.yas.commonlibrary.kafka.cdc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseCdcConsumerTest {

    static class TestConsumer extends BaseCdcConsumer<String, String> {
        private final AtomicReference<String> last = new AtomicReference<>();
        private final AtomicReference<String> lastKey = new AtomicReference<>();

        public void callProcessMessageRecord(String record) {
            Map<String, Object> headers = new HashMap<>();
            headers.put(KafkaHeaders.RECEIVED_KEY, "rk");
            processMessage(record, new MessageHeaders(headers), r -> last.set(r));
        }

        public void callProcessMessageKeyValue(String key, String value) {
            Map<String, Object> headers = new HashMap<>();
            headers.put(KafkaHeaders.RECEIVED_KEY, key);
            processMessage(key, value, new MessageHeaders(headers), (k, v) -> { lastKey.set(k); last.set(v); });
        }
    }

    @Test
    void processMessage_variants_invokeConsumers() {
        TestConsumer tc = new TestConsumer();

        tc.callProcessMessageRecord("payload");
        assertEquals("payload", tc.last.get());

        tc.callProcessMessageKeyValue("k1", "v1");
        assertEquals("k1", tc.lastKey.get());
        assertEquals("v1", tc.last.get());
    }
}
