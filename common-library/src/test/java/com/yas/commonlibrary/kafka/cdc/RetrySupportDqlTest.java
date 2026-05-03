package com.yas.commonlibrary.kafka.cdc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RetrySupportDqlTest {

    @Test
    void annotation_shouldHaveRetryableTopicMeta() {
        // ensure the annotation class loads and is present
        assertNotNull(RetrySupportDql.class.getAnnotation(org.springframework.kafka.annotation.RetryableTopic.class));
    }
}
