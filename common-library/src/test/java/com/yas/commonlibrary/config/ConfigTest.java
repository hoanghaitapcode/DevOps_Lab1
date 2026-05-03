package com.yas.commonlibrary.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void serviceUrlConfig_recordsValues() {
        ServiceUrlConfig config = new ServiceUrlConfig("media", "product");
        assertEquals("media", config.media());
        assertEquals("product", config.product());
    }

    @Test
    void corsConfig_createsConfigurer() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://example.com");

        WebMvcConfigurer webMvcConfigurer = config.corsConfigure();
        assertNotNull(webMvcConfigurer);

        CorsRegistry registry = new CorsRegistry();
        webMvcConfigurer.addCorsMappings(registry);
    }
}
