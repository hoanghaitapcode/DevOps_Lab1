package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductRelated;
import com.yas.product.model.enumeration.FilterExistInWhSelection;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.ImageVm;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductCheckoutListVm;
import com.yas.product.viewmodel.product.ProductDetailGetVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductFeatureGetVm;
import com.yas.product.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductInfoVm;
import com.yas.product.viewmodel.product.ProductListGetFromCategoryVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductPutVm;
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.product.viewmodel.product.ProductQuantityPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductVariationGetVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private MediaService mediaService;

	@Mock
	private BrandRepository brandRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductCategoryRepository productCategoryRepository;

	@Mock
	private ProductImageRepository productImageRepository;

	@Mock
	private ProductOptionRepository productOptionRepository;

	@Mock
	private ProductOptionValueRepository productOptionValueRepository;

	@Mock
	private ProductOptionCombinationRepository productOptionCombinationRepository;

	@Mock
	private ProductRelatedRepository productRelatedRepository;

	@InjectMocks
	private ProductService productService;

	private Brand brand;
	private Category category;
	private Product product;

	@BeforeEach
	void setUp() {
		brand = new Brand();
		brand.setId(11L);
		brand.setName("Brand X");
		brand.setSlug("brand-x");

		category = new Category();
		category.setId(21L);
		category.setName("Category X");
		category.setSlug("category-x");

		product = new Product();
		product.setId(1L);
		product.setName("Product A");
		product.setSlug("product-a");
		product.setSku("SKU-A");
		product.setGtin("GTIN-A");
		product.setShortDescription("short");
		product.setDescription("description");
		product.setSpecification("spec");
		product.setAllowedToOrder(true);
		product.setPublished(true);
		product.setFeatured(true);
		product.setVisibleIndividually(true);
		product.setStockTrackingEnabled(true);
		product.setPrice(100.0);
		product.setTaxClassId(5L);
		product.setThumbnailMediaId(100L);
		product.setCreatedOn(ZonedDateTime.parse("2026-04-28T00:00:00Z"));
		product.setBrand(brand);
		product.setProductCategories(List.of(ProductCategory.builder()
			.product(product)
			.category(category)
			.build()));
		product.setProductImages(List.of(ProductImage.builder()
			.product(product)
			.imageId(101L)
			.build()));
	}

	@Test
	void getLatestProducts_whenCountIsZero_thenReturnEmptyList() {
		assertThat(productService.getLatestProducts(0)).isEmpty();
		verifyNoInteractions(productRepository);
	}

	@Test
	void getLatestProducts_whenProductsExist_thenReturnMappedList() {
		when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of(product));

		List<ProductListVm> result = productService.getLatestProducts(1);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo(1L);
		assertThat(result.getFirst().name()).isEqualTo("Product A");
	}

	@Test
	void getProductById_whenProductExists_thenReturnDetail() {
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/media/100"));
		when(mediaService.getMedia(101L)).thenReturn(new NoFileMediaVm(101L, "caption", "image1.png", "image/png", "/media/101"));

		ProductDetailVm result = productService.getProductById(1L);

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.brandId()).isEqualTo(11L);
		assertThat(result.categories()).hasSize(1);
		assertThat(result.thumbnailMedia()).isEqualTo(new ImageVm(100L, "/media/100"));
		assertThat(result.productImageMedias()).hasSize(1);
	}

	@Test
	void getProductById_whenProductMissing_thenThrowNotFoundException() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> productService.getProductById(99L));
	}

	@Test
	void getProductSlug_whenParentExists_thenReturnParentSlug() {
		Product parent = new Product();
		parent.setId(2L);
		parent.setSlug("parent-slug");
		product.setParent(parent);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		assertThat(productService.getProductSlug(1L)).isEqualTo(new ProductSlugGetVm("parent-slug", 1L));
	}

	@Test
	void getProductSlug_whenNoParent_thenReturnOwnSlug() {
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		assertThat(productService.getProductSlug(1L)).isEqualTo(new ProductSlugGetVm("product-a", null));
	}

	@Test
	void getProductsForWarehouse_shouldMapSimpleInfo() {
		when(productRepository.findProductForWarehouse("lap", "sku", List.of(1L), "ALL"))
			.thenReturn(List.of(product));

		List<ProductInfoVm> result = productService.getProductsForWarehouse("lap", "sku", List.of(1L), FilterExistInWhSelection.ALL);

		assertThat(result).containsExactly(new ProductInfoVm(1L, "Product A", "SKU-A"));
	}

	@Test
	void updateProductQuantity_shouldUpdateMatchedProducts() {
		ProductQuantityPostVm quantityVm = new ProductQuantityPostVm(1L, 7L);
		when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

		productService.updateProductQuantity(List.of(quantityVm));

		assertThat(product.getStockQuantity()).isEqualTo(7L);
		verify(productRepository).saveAll(List.of(product));
	}

	@Test
	void restoreStockQuantity_shouldAddBackStock() {
		ProductQuantityPutVm quantityVm = new ProductQuantityPutVm(1L, 3L);
		product.setStockQuantity(5L);
		when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

		productService.restoreStockQuantity(List.of(quantityVm));

		assertThat(product.getStockQuantity()).isEqualTo(8L);
		verify(productRepository).saveAll(List.of(product));
	}

	@Test
	void subtractStockQuantity_shouldNotGoBelowZero() {
		ProductQuantityPutVm quantityVm = new ProductQuantityPutVm(1L, 9L);
		product.setStockQuantity(5L);
		when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

		productService.subtractStockQuantity(List.of(quantityVm));

		assertThat(product.getStockQuantity()).isEqualTo(0L);
		verify(productRepository).saveAll(List.of(product));
	}

	@Test
	void deleteProduct_whenChildProduct_thenDeleteCombinationsAndSave() {
		Product parent = new Product();
		parent.setId(2L);
		product.setParent(parent);
		ProductOption option = new ProductOption();
		option.setId(3L);
		ProductOptionCombination combination = ProductOptionCombination.builder()
			.product(product)
			.productOption(option)
			.value("Black")
			.build();
		product.setRelatedProducts(List.of());
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productOptionCombinationRepository.findAllByProduct(product)).thenReturn(List.of(combination));

		productService.deleteProduct(1L);

		assertThat(product.isPublished()).isFalse();
		verify(productOptionCombinationRepository).deleteAll(List.of(combination));
		verify(productRepository).save(product);
	}

	@Test
	void getProductDetail_whenProductExists_thenReturnDetailVm() {
		when(productRepository.findBySlugAndIsPublishedTrue("product-a")).thenReturn(Optional.of(product));
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/media/100"));
		when(mediaService.getMedia(101L)).thenReturn(new NoFileMediaVm(101L, "caption", "image1.png", "image/png", "/media/101"));

		ProductDetailGetVm result = productService.getProductDetail("product-a");

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.name()).isEqualTo("Product A");
		assertThat(result.brandName()).isEqualTo("Brand X");
		assertThat(result.productCategories()).containsExactly("Category X");
		assertThat(result.thumbnailMediaUrl()).isEqualTo("/media/100");
		assertThat(result.productImageMediaUrls()).containsExactly("/media/101");
	}

	@Test
	void getProductsByBrand_shouldMapThumbnails() {
		when(brandRepository.findBySlug("brand-x")).thenReturn(Optional.of(brand));
		when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(product));
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/thumb"));

		List<ProductThumbnailVm> result = productService.getProductsByBrand("brand-x");

		assertThat(result).containsExactly(new ProductThumbnailVm(1L, "Product A", "product-a", "/thumb"));
	}

	@Test
	void getProductsCheckoutList_shouldReturnPageData() {
		when(productRepository.findAllPublishedProductsByIds(anyList(), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 1), 1));
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/thumb"));

		ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 1, List.of(1L));

		assertThat(result.productCheckoutListVms()).hasSize(1);
		ProductCheckoutListVm checkout = result.productCheckoutListVms().getFirst();
		assertThat(checkout.id()).isEqualTo(1L);
		assertThat(checkout.thumbnailUrl()).isEqualTo("/thumb");
	}

	@Test
	void getProductByIds_shouldMapProducts() {
		when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

		List<ProductListVm> result = productService.getProductByIds(List.of(1L));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo(1L);
	}

	@Test
	void getProductByCategoryIds_shouldMapProducts() {
		when(productRepository.findByCategoryIdsIn(List.of(21L))).thenReturn(List.of(product));

		List<ProductListVm> result = productService.getProductByCategoryIds(List.of(21L));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().slug()).isEqualTo("product-a");
	}

	@Test
	void getProductByBrandIds_shouldMapProducts() {
		when(productRepository.findByBrandIdsIn(List.of(11L))).thenReturn(List.of(product));

		List<ProductListVm> result = productService.getProductByBrandIds(List.of(11L));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().name()).isEqualTo("Product A");
	}

	@Test
	void getProductsWithFilter_shouldReturnPagedResult() {
		Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
		when(productRepository.getProductsWithFilter("lap", "Brand X", PageRequest.of(0, 10)))
			.thenReturn(page);

		ProductListGetVm result = productService.getProductsWithFilter(0, 10, "Lap", "Brand X");

		assertThat(result.productContent()).hasSize(1);
		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	void getProductsFromCategory_shouldReturnPagedThumbnails() {
		Page<ProductCategory> page = new PageImpl<>(List.of(ProductCategory.builder()
			.product(product)
			.category(category)
			.build()), PageRequest.of(0, 10), 1);
		when(categoryRepository.findBySlug("category-x")).thenReturn(Optional.of(category));
		when(productCategoryRepository.findAllByCategory(PageRequest.of(0, 10), category)).thenReturn(page);
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/thumb"));

		ProductListGetFromCategoryVm result = productService.getProductsFromCategory(0, 10, "category-x");

		assertThat(result.productContent()).hasSize(1);
		assertThat(result.productContent().getFirst().thumbnailUrl()).isEqualTo("/thumb");
	}

	@Test
	void getListFeaturedProducts_shouldMapThumbnails() {
		Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
		when(productRepository.getFeaturedProduct(PageRequest.of(0, 10))).thenReturn(page);
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/thumb"));

		ProductFeatureGetVm result = productService.getListFeaturedProducts(0, 10);

		assertThat(result.productList()).hasSize(1);
		assertThat(result.totalPage()).isEqualTo(1);
	}

	@Test
	void getProductsByMultiQuery_shouldReturnPage() {
		Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
		when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
			eq("lap"), eq("category-x"), eq(1.0), eq(200.0), eq(PageRequest.of(0, 10))))
			.thenReturn(page);
		when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "caption", "thumb.png", "image/png", "/thumb"));

		ProductsGetVm result = productService.getProductsByMultiQuery(0, 10, "Lap", "category-x", 1.0, 200.0);

		assertThat(result.productContent()).hasSize(1);
		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	void getFeaturedProductsById_shouldReturnParentThumbnailWhenCurrentMissing() {
		Product parent = new Product();
		parent.setId(2L);
		parent.setThumbnailMediaId(999L);
		Product variant = new Product();
		variant.setId(3L);
		variant.setName("Variant");
		variant.setSlug("variant");
		variant.setPrice(20.0);
		variant.setParent(parent);
		variant.setThumbnailMediaId(300L);
		when(productRepository.findAllByIdIn(List.of(3L))).thenReturn(List.of(variant));
		when(mediaService.getMedia(300L)).thenReturn(new NoFileMediaVm(300L, "caption", "file.png", "image/png", ""));
		when(productRepository.findById(2L)).thenReturn(Optional.of(parent));
		when(mediaService.getMedia(999L)).thenReturn(new NoFileMediaVm(999L, "caption", "parent.png", "image/png", "/parent-thumb"));

		List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(3L));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().thumbnailUrl()).isEqualTo("/parent-thumb");
	}

	@Test
	void getProductVariationsByParentId_whenHasOptions_shouldReturnVariations() {
		Product variation = new Product();
		variation.setId(5L);
		variation.setName("Var 1");
		variation.setSlug("var-1");
		variation.setSku("SKU-1");
		variation.setGtin("GTIN-1");
		variation.setPrice(50.0);
		variation.setPublished(true);
		variation.setThumbnailMediaId(500L);
		variation.setProductImages(List.of(ProductImage.builder().imageId(501L).product(variation).build()));

		Product parent = new Product();
		parent.setId(1L);
		parent.setHasOptions(true);
		parent.setProducts(List.of(variation));

		ProductOption option = new ProductOption();
		option.setId(77L);
		ProductOptionCombination combination = ProductOptionCombination.builder()
			.product(variation)
			.productOption(option)
			.value("XL")
			.build();

		when(productRepository.findById(1L)).thenReturn(Optional.of(parent));
		when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));
		when(mediaService.getMedia(500L)).thenReturn(new NoFileMediaVm(500L, "caption", "thumb.png", "image/png", "/v-thumb"));
		when(mediaService.getMedia(501L)).thenReturn(new NoFileMediaVm(501L, "caption", "img.png", "image/png", "/v-image"));

		List<ProductVariationGetVm> result = productService.getProductVariationsByParentId(1L);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().options()).containsEntry(77L, "XL");
	}

	@Test
	void getProductEsDetailById_shouldReturnMappedData() {
		ProductAttributeGroup group = new ProductAttributeGroup();
		group.setId(1L);
		group.setName("General");
		ProductAttribute attribute = ProductAttribute.builder().id(2L).name("Color").productAttributeGroup(group).build();
		ProductAttributeValue attributeValue = new ProductAttributeValue();
		attributeValue.setId(3L);
		attributeValue.setProduct(product);
		attributeValue.setProductAttribute(attribute);
		attributeValue.setValue("Black");
		product.setAttributeValues(List.of(attributeValue));

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		var result = productService.getProductEsDetailById(1L);

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.brand()).isEqualTo("Brand X");
		assertThat(result.categories()).containsExactly("Category X");
		assertThat(result.attributes()).containsExactly("Color");
	}

	@Test
	void exportProducts_shouldMapData() {
		when(productRepository.getExportingProducts("lap", "Brand X")).thenReturn(List.of(product));

		var result = productService.exportProducts("Lap", "Brand X");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo(1L);
		assertThat(result.getFirst().brandName()).isEqualTo("Brand X");
	}

	@Test
	void createProduct_shouldSaveAndReturnProductDetail() {
		ProductPostVm postVm = new ProductPostVm(
			"Product New", "product-new", 11L, List.of(21L), "short", "desc", "spec", "SKU-N", "GTIN-N",
			1.0, null, 1.0, 1.0, 1.0, 100.0, true, true, true, true, true,
			"meta", "meta", "meta", 100L, List.of(101L), List.of(), List.of(), List.of(), List.of(), 5L
		);

		when(brandRepository.findById(11L)).thenReturn(Optional.of(brand));
		when(categoryRepository.findAllById(List.of(21L))).thenReturn(List.of(category));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product p = invocation.getArgument(0);
			p.setId(2L);
			return p;
		});

		ProductGetDetailVm result = productService.createProduct(postVm);

		assertThat(result.id()).isEqualTo(2L);
		assertThat(result.name()).isEqualTo("Product New");
		verify(productImageRepository).saveAll(anyList());
		verify(productCategoryRepository).saveAll(anyList());
	}

	@Test
	void createProduct_whenSlugExists_shouldThrowDuplicatedException() {
		ProductPostVm postVm = new ProductPostVm(
			"Product New", "product-a", 11L, List.of(21L), "short", "desc", "spec", "SKU-N", "GTIN-N",
			1.0, null, 1.0, 1.0, 1.0, 100.0, true, true, true, true, true,
			"meta", "meta", "meta", 100L, List.of(101L), List.of(), List.of(), List.of(), List.of(), 5L
		);
		when(productRepository.findBySlugAndIsPublishedTrue("product-a")).thenReturn(Optional.of(product));

		assertThrows(com.yas.commonlibrary.exception.DuplicatedException.class, () -> productService.createProduct(postVm));
	}

	@Test
	void updateProduct_shouldUpdateAndSave() {
		ProductPutVm putVm = new ProductPutVm(
			"Product Updated", "product-a", 150.0, true, true, true, true, true, 11L, List.of(21L),
			"short updated", "desc updated", "spec updated", "SKU-U", "GTIN-U", 2.0, null, 2.0, 2.0, 2.0,
			"meta", "meta", "meta", 100L, List.of(101L), List.of(), List.of(), List.of(), List.of(), 5L
		);

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		productService.updateProduct(1L, putVm);

		assertThat(product.getName()).isEqualTo("Product Updated");
		assertThat(product.getPrice()).isEqualTo(150.0);
	}

	@Test
	void updateProduct_whenProductNotFound_shouldThrowNotFoundException() {
		ProductPutVm putVm = new ProductPutVm(
			"Product Updated", "product-a", 150.0, true, true, true, true, true, 11L, List.of(21L),
			"short updated", "desc updated", "spec updated", "SKU-U", "GTIN-U", 2.0, null, 2.0, 2.0, 2.0,
			"meta", "meta", "meta", 100L, List.of(101L), List.of(), List.of(), List.of(), List.of(), 5L
		);
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> productService.updateProduct(99L, putVm));
	}
}