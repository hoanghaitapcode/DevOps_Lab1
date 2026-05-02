package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.ImageVm;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    private Product publishedProduct;
    private Product variationProduct;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand();
        brand.setId(10L);
        brand.setName("Brand A");

        Category category = new Category();
        category.setId(20L);
        category.setName("Category A");

        publishedProduct = new Product();
        publishedProduct.setId(1L);
        publishedProduct.setName("Product A");
        publishedProduct.setShortDescription("short");
        publishedProduct.setDescription("description");
        publishedProduct.setSpecification("spec");
        publishedProduct.setSku("SKU-A");
        publishedProduct.setGtin("GTIN-A");
        publishedProduct.setSlug("product-a");
        publishedProduct.setAllowedToOrder(true);
        publishedProduct.setPublished(true);
        publishedProduct.setFeatured(true);
        publishedProduct.setVisibleIndividually(true);
        publishedProduct.setStockTrackingEnabled(true);
        publishedProduct.setPrice(100.0);
        publishedProduct.setBrand(brand);
        publishedProduct.setMetaTitle("meta title");
        publishedProduct.setMetaKeyword("meta keyword");
        publishedProduct.setMetaDescription("meta description");
        publishedProduct.setTaxClassId(5L);
        publishedProduct.setThumbnailMediaId(100L);
        publishedProduct.setProductCategories(List.of(ProductCategory.builder()
            .category(category)
            .product(publishedProduct)
            .build()));
        publishedProduct.setProductImages(List.of(ProductImage.builder()
            .imageId(101L)
            .product(publishedProduct)
            .build()));

        variationProduct = new Product();
        variationProduct.setId(2L);
        variationProduct.setName("Variant A");
        variationProduct.setSlug("variant-a");
        variationProduct.setSku("VAR-SKU");
        variationProduct.setGtin("VAR-GTIN");
        variationProduct.setPrice(90.0);
        variationProduct.setPublished(true);
        variationProduct.setThumbnailMediaId(200L);
        variationProduct.setProductImages(List.of(ProductImage.builder()
            .imageId(201L)
            .product(variationProduct)
            .build()));

        publishedProduct.setHasOptions(true);
        publishedProduct.setProducts(List.of(variationProduct));
    }

    @Test
    void getProductDetailById_whenProductIsMissing_thenThrowNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(999L));
    }

    @Test
    void getProductDetailById_whenProductHasVariation_thenReturnDetailVm() {
        ProductOption option = new ProductOption();
        option.setId(300L);
        option.setName("Color");

        ProductOptionCombination combination = ProductOptionCombination.builder()
            .product(variationProduct)
            .productOption(option)
            .value("Black")
            .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(publishedProduct));
        when(productOptionCombinationRepository.findAllByProduct(variationProduct)).thenReturn(List.of(combination));
        when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/media/100"));
        when(mediaService.getMedia(101L)).thenReturn(new NoFileMediaVm(101L, "caption", "image1.png", "image/png", "/media/101"));
        when(mediaService.getMedia(200L)).thenReturn(new NoFileMediaVm(200L, "caption", "variant.png", "image/png", "/media/200"));
        when(mediaService.getMedia(201L)).thenReturn(new NoFileMediaVm(201L, "caption", "variant1.png", "image/png", "/media/201"));

        ProductDetailInfoVm detail = productDetailService.getProductDetailById(1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getBrandId()).isEqualTo(10L);
        assertThat(detail.getBrandName()).isEqualTo("Brand A");
        assertThat(detail.getCategories()).hasSize(1);
        assertThat(detail.getThumbnail()).isEqualTo(new ImageVm(100L, "/media/100"));
        assertThat(detail.getProductImages()).hasSize(1);
        assertThat(detail.getVariations()).hasSize(1);
        assertThat(detail.getVariations().getFirst().options()).containsEntry(300L, "Black");
        assertThat(detail.getVariations().getFirst().thumbnail()).isEqualTo(new ImageVm(200L, "/media/200"));
    }
}
