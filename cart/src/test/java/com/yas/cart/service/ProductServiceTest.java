package com.yas.cart.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.cart.viewmodel.ProductThumbnailVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class ProductServiceTest {

    RestClient restClient;

    ServiceUrlConfig serviceUrlConfig;

    ProductService productService;

    RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = Mockito.mock(RestClient.class);
        serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
    }

    @Test
    void getProducts_NormalCase_ReturnProductThumbnailVms() {

        List<Long> ids = List.of(1L, 2L, 3L);
        URI url = UriComponentsBuilder
            .fromUriString("http://api.yas.local/media")
            .path("/storefront/products/list-featured")
            .queryParam("productId", ids)
            .build()
            .toUri();

        when(serviceUrlConfig.product()).thenReturn("http://api.yas.local/media");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(getProductThumbnailVms()));

        List<ProductThumbnailVm> result = productService.getProducts(ids);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1);
        assertThat(result.get(1).id()).isEqualTo(2);
        assertThat(result.get(2).id()).isEqualTo(3);
    }

    @Test
    void getProductById_WhenNoProductFound_ReturnNull() {
        ProductService spyService = Mockito.spy(productService);
        doReturn(List.of()).when(spyService).getProducts(List.of(999L));

        ProductThumbnailVm result = spyService.getProductById(999L);

        assertNull(result);
    }

    @Test
    void getProductById_WhenProductsReturned_ReturnFirstProduct() {
        ProductService spyService = Mockito.spy(productService);
        List<ProductThumbnailVm> products = getProductThumbnailVms();
        doReturn(products).when(spyService).getProducts(List.of(1L));

        ProductThumbnailVm result = spyService.getProductById(1L);

        assertThat(result).isEqualTo(products.getFirst());
    }

    @Test
    void existsById_WhenProductExists_ReturnTrue() {
        ProductService spyService = Mockito.spy(productService);
        doReturn(new ProductThumbnailVm(1L, "p", "p", "t")).when(spyService).getProductById(1L);

        boolean exists = spyService.existsById(1L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenProductNotExists_ReturnFalse() {
        ProductService spyService = Mockito.spy(productService);
        doReturn(null).when(spyService).getProductById(1L);

        boolean exists = spyService.existsById(1L);

        assertThat(exists).isFalse();
    }

    @Test
    void handleProductThumbnailFallback_WhenThrowableProvided_ShouldRethrow() {
        RuntimeException exception = new RuntimeException("service unavailable");

        assertThrows(RuntimeException.class, () -> productService.handleProductThumbnailFallback(exception));
    }

    private List<ProductThumbnailVm> getProductThumbnailVms() {

        ProductThumbnailVm product1 = new ProductThumbnailVm(
            1L,
            "Product 1",
            "product-1",
            "http://example.com/product1.jpg"
        );
        ProductThumbnailVm product2 = new ProductThumbnailVm(
            2L,
            "Product 2",
            "product-2",
            "http://example.com/product2.jpg"
        );
        ProductThumbnailVm product3 = new ProductThumbnailVm(
            3L,
            "Product 3",
            "product-3",
            "http://example.com/product3.jpg"
        );

        return List.of(product1, product2, product3);
    }
}