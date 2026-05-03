package com.yas.commonlibrary.kafka.cdc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetrySupportDqlAnnotationTest {

    @RetrySupportDql
    static class AnnotatedClass {}

    @Test
    void annotation_defaults_arePresent() {
        RetrySupportDql ann = AnnotatedClass.class.getAnnotation(RetrySupportDql.class);
        assertNotNull(ann);
        assertEquals("4", ann.attempts());
        assertEquals("true", ann.autoCreateTopics());
        assertEquals("", ann.listenerContainerFactory());
        assertNotNull(ann.exclude());
        assertEquals(0, ann.exclude().length);
    }
}
