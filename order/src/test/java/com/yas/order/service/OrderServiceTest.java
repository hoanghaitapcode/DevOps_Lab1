package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.model.Order;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

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
    void getOrderWithItemsById_found_mapsVm() {
        com.yas.order.model.OrderAddress ship = com.yas.order.model.OrderAddress.builder().id(1L).contactName("s").phone("p").addressLine1("a").city("c").zipCode("z").build();
        com.yas.order.model.OrderAddress bill = com.yas.order.model.OrderAddress.builder().id(2L).contactName("b").phone("pb").addressLine1("ab").city("cb").zipCode("zb").build();
        Order order = Order.builder().id(101L).email("e@x").totalPrice(BigDecimal.TEN).shippingAddressId(ship).billingAddressId(bill).build();
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        OrderItem oi = OrderItem.builder().id(5L).orderId(101L).productId(2L).build();
        when(orderItemRepository.findAllByOrderId(101L)).thenReturn(List.of(oi));

        OrderVm vm = svc.getOrderWithItemsById(101L);
        assertEquals(101L, vm.id());
        assertNotNull(vm.orderItemVms());
        assertEquals(1, vm.orderItemVms().size());
    }

    @Test
    void findOrderByCheckoutId_notFound_throws() {
        when(orderRepository.findByCheckoutId("c1")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> svc.findOrderByCheckoutId("c1"));
    }

    @Test
    void updateOrderPaymentStatus_completed_setsPaid() {
        Order order = Order.builder().id(200L).orderStatus(OrderStatus.PENDING).build();
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentOrderStatusVm in = PaymentOrderStatusVm.builder().orderId(200L).paymentId(111L).paymentStatus("COMPLETED").build();
        var out = svc.updateOrderPaymentStatus(in);
        assertEquals("PAID", out.orderStatus());
        assertEquals(111L, out.paymentId());
    }

    @Test
    void rejectAndAccept_orderNotFound_throws() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> svc.rejectOrder(999L, "r"));
        assertThrows(NotFoundException.class, () -> svc.acceptOrder(999L));
    }

    @Test
    void getLatestOrders_zeroOrEmpty_returnsEmpty() {
        assertTrue(svc.getLatestOrders(0).isEmpty());
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of());
        assertTrue(svc.getLatestOrders(5).isEmpty());
    }
}
