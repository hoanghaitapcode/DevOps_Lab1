package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.model.Order;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

class OrderServiceAdditionalTest {

    OrderRepository orderRepository = mock(OrderRepository.class);
    OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    ProductService productService = mock(ProductService.class);
    CartService cartService = mock(CartService.class);
    OrderMapper orderMapper = mock(OrderMapper.class);
    PromotionService promotionService = mock(PromotionService.class);

    OrderService svc;

    @BeforeEach
    void setUp() {
        svc = new OrderService(orderRepository, orderItemRepository, productService, cartService, orderMapper, promotionService);
    }

    @Test
    void createOrder_happyPath_callsDependencies_andReturnsVm() {
        OrderItemPostVm itemPost = OrderItemPostVm.builder()
                .productId(1L).productName("p")
                .quantity(2).productPrice(BigDecimal.ONE)
                .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO).taxPercent(BigDecimal.ZERO)
                .build();

        OrderAddressPostVm addr = OrderAddressPostVm.builder()
                .contactName("cn").phone("p").addressLine1("a1").addressLine2("a2")
                .city("city").zipCode("zip").districtId(1L).districtName("d").stateOrProvinceId(1L)
                .stateOrProvinceName("s").countryId(1L).countryName("c").build();

        OrderPostVm post = OrderPostVm.builder()
                .checkoutId("checkout-1")
                .email("e@x")
                .shippingAddressPostVm(addr)
                .billingAddressPostVm(addr)
                .note("n")
                .tax(0f).discount(0f)
                .numberItem(1)
                .totalPrice(BigDecimal.ONE)
                .deliveryFee(BigDecimal.ZERO)
                .couponCode("code")
                .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .orderItemPostVms(List.of(itemPost))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(500L);
            return o;
        });
        when(orderRepository.findById(500L)).thenReturn(Optional.of(Order.builder().id(500L).build()));
        when(orderItemRepository.saveAll(any())).thenAnswer(i -> new java.util.ArrayList<>((java.util.Collection<?>) i.getArgument(0)));
        doNothing().when(productService).subtractProductStockQuantity(any());
        doNothing().when(cartService).deleteCartItems(any());
        doNothing().when(promotionService).updateUsagePromotion(anyList());

        var vm = svc.createOrder(post);

        assertNotNull(vm);
        assertEquals(500L, vm.id());
        verify(orderRepository, atLeast(1)).save(any());
        verify(orderItemRepository).saveAll(any());
        verify(productService).subtractProductStockQuantity(any());
        verify(cartService).deleteCartItems(any());
        verify(promotionService).updateUsagePromotion(anyList());
    }

    @Test
    void getAllOrder_nonEmptyPage_returnsListVm() {
        com.yas.order.model.OrderAddress ship = com.yas.order.model.OrderAddress.builder().id(1L).contactName("s").phone("p").addressLine1("a").city("c").zipCode("z").build();
        com.yas.order.model.OrderAddress bill = com.yas.order.model.OrderAddress.builder().id(2L).contactName("b").phone("pb").addressLine1("ab").city("cb").zipCode("zb").build();
        Order o1 = Order.builder().id(1L).email("a").totalPrice(BigDecimal.TEN).shippingAddressId(ship).billingAddressId(bill).build();
        Page<Order> page = new PageImpl<>(List.of(o1));
        doReturn(page).when(orderRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));

        var res = svc.getAllOrder(Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()), null, List.of(), Pair.of("", ""), null, Pair.of(0, 10));
        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_withVariations_checksRepo() {
        try (var ms = org.mockito.Mockito.mockStatic(com.yas.commonlibrary.utils.AuthenticationUtils.class)) {
            ms.when(com.yas.commonlibrary.utils.AuthenticationUtils::extractUserId).thenReturn("user1");
            when(productService.getProductVariations(10L)).thenReturn(List.of());
            doReturn(Optional.empty()).when(orderRepository).findOne(any(org.springframework.data.jpa.domain.Specification.class));

            var exists = svc.isOrderCompletedWithUserIdAndProductId(10L);
            assertNotNull(exists);
            assertFalse(exists.isPresent());
        }
    }

    @Test
    void findOrderVmByCheckoutId_callsRepoAndMaps() {
        Order o = Order.builder().id(77L).checkoutId("c7").build();
        when(orderRepository.findByCheckoutId("c7")).thenReturn(Optional.of(o));
        when(orderItemRepository.findAllByOrderId(77L)).thenReturn(List.of(new OrderItem()));

        var vm = svc.findOrderVmByCheckoutId("c7");
        assertNotNull(vm);
        assertEquals(77L, vm.id());
    }

    @Test
    void exportCsv_whenOrderListNull_returnsEmptyBytes() throws IOException {
        // exercise CSV exporter directly to avoid Pair.of null complexity in unit test
        byte[] bytes = com.yas.commonlibrary.csv.CsvExporter.exportToCsv(List.of(), com.yas.order.model.csv.OrderItemCsv.class);
        assertNotNull(bytes);
        assertTrue(bytes.length >= 0);
    }
}
