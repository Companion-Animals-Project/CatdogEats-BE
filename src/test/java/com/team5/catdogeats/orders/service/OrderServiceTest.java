package com.team5.catdogeats.orders.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.impl.OrderServiceImpl;
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderPendingDetailsRepository orderPendingDetailsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TossPaymentResponseBuilder tossPaymentResponseBuilder;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ShipmentRepository shipmentRepository;

    private UserPrincipal principal;
    private Users user;
    private BuyerDTO buyerDTO;
    private Products product1;
    private Products product2;
    private Orders savedOrder;
    private Orders orderForDetail;
    private OrderCreateRequest requestWithCoupon;
    private OrderCreateRequest request100Percent;
    private OrderCreateResponse mockResponse;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal("google", "google123");
        user = Users.builder().id("user123").provider("google").providerId("google123").name("김철수").role(Role.ROLE_BUYER).build();
        buyerDTO = new BuyerDTO("user123", true, false, null);

        product1 = Products.builder().id("product1").title("강아지 사료").price(25000L).stock(100).build();
        product2 = Products.builder().id("product2").title("고양이 간식").price(15000L).stock(50).build();

        savedOrder = Orders.builder().id("order123").orderNumber("1001").user(user).orderStatus(OrderStatus.PAYMENT_PENDING).totalPrice(55250L).build();

        requestWithCoupon = OrderCreateRequest.builder()
                .orderItems(Arrays.asList(
                        OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(2).build(),
                        OrderCreateRequest.OrderItemRequest.builder().productId("product2").quantity(1).build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder().couponDiscountRate(15.0).build())
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder().recipientName("김철수").recipientPhone("010-1234-5678").postalCode("06234").streetAddress("서울시 강남구 테헤란로 123").detailAddress("456호").deliveryNote("문 앞에 놓아주세요").build())
                .build();

        request100Percent = OrderCreateRequest.builder()
                .orderItems(Collections.singletonList(OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(1).build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder().couponDiscountRate(100.0).build())
                .build();

        mockResponse = OrderCreateResponse.builder().orderId("order123").orderNumber("1001").totalPrice(55250L).orderStatus(OrderStatus.PAYMENT_PENDING).build();

        orderForDetail = Orders.builder().id("order456").orderNumber("2001").user(user).orderStatus(OrderStatus.PAYMENT_COMPLETED).totalPrice(30000L).orderItems(new ArrayList<>()).build();
    }

    // ... CreateOrderTests and GetOrderDetailTests are unchanged ...
    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTests {

        @Test
        @DisplayName("쿠폰 할인이 적용된 주문 생성 성공")
        void createOrderWithCouponDiscount_Success() throws Exception {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(productRepository.findById("product2")).willReturn(Optional.of(product2));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            doReturn("[]").when(objectMapper).writeValueAsString(any());
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString())).willReturn(mockResponse);

            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, requestWithCoupon);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("order123");
            assertThat(response.getTotalPrice()).isEqualTo(55250L);
            verify(orderRepository).save(any(Orders.class));
            verify(orderPendingDetailsRepository).save(any());
            verify(objectMapper, times(2)).writeValueAsString(any());
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            OrderCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.orderId()).isEqualTo("order123");
            assertThat(capturedEvent.finalTotalPrice()).isEqualTo(55250L);
        }

        @Test
        @DisplayName("100% 쿠폰 할인으로 최소 결제 금액(1원) 적용")
        void createOrderWith100PercentDiscount_MinimumAmount() throws Exception {
            Orders order100PercentEntity = Orders.builder().id("order789").orderNumber("1002").user(user).orderStatus(OrderStatus.PAYMENT_PENDING).totalPrice(1L).build();
            OrderCreateResponse specificMockResponse = OrderCreateResponse.builder().orderId("order789").orderNumber("1002").totalPrice(1L).orderStatus(OrderStatus.PAYMENT_PENDING).build();
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(orderRepository.save(any(Orders.class))).willReturn(order100PercentEntity);
            doReturn("[]").when(objectMapper).writeValueAsString(any());
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString())).willReturn(specificMockResponse);

            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, request100Percent);

            assertThat(response).isNotNull();
            assertThat(response.getTotalPrice()).isEqualTo(1L);
            verify(orderRepository).save(any(Orders.class));
            verify(orderPendingDetailsRepository).save(any());
        }

        @Test
        @DisplayName("잘못된 쿠폰 할인률로 주문 생성 실패 - 100% 초과")
        void createOrderWithInvalidCouponRate_ThrowsException() {
            OrderCreateRequest invalidRequest = requestWithCoupon.toBuilder().paymentInfo(requestWithCoupon.getPaymentInfo().toBuilder().couponDiscountRate(150.0).build()).build();
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById(anyString())).willReturn(Optional.of(product1));

            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("쿠폰 할인율은 100%를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 주문 생성 실패")
        void createOrderWithNonExistentProduct_ThrowsException() {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다: product1");
        }

        @Test
        @DisplayName("구매자 권한이 없는 사용자의 주문 생성 실패")
        void createOrderWithoutBuyerRole_ThrowsException() {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("구매자를 찾을 수 없거나 권한이 없습니다");
        }

        @Test
        @DisplayName("ObjectMapper JSON 직렬화 실패 테스트")
        void createOrder_JsonProcessingException() throws Exception {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById(anyString())).willReturn(Optional.of(product1));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            doThrow(new JsonProcessingException("JSON 직렬화 실패") {}).when(objectMapper).writeValueAsString(anyList());

            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("주문 정보 저장 중 오류가 발생했습니다")
                    .hasCauseInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 테스트")
    class GetOrderDetailTests {
        @Test
        @DisplayName("주문 상세 조회 성공")
        void getOrderDetail_Success() {
            String orderNumber = "2001";
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)).willReturn(Optional.of(orderForDetail));
            given(shipmentRepository.findByOrders(any(Orders.class))).willReturn(Optional.empty());

            OrderDetailResponse response = orderService.getOrderDetail(principal, orderNumber);

            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo("order456");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, "2001");
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 실패")
        void getOrderDetail_OrderNotFound() {
            String orderNumber = "9999";
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("주문을 찾을 수 없거나 접근 권한이 없습니다.");
        }

        @Test
        @DisplayName("구매자 권한 없는 사용자의 주문 조회 실패")
        void getOrderDetail_WithoutBuyerRole() {
            String orderNumber = "2001";
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("구매자를 찾을 수 없거나 권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 삭제 테스트")
    class DeleteOrderTests {
        private final String orderNumber = "2001";

        @BeforeEach
        void setUp() {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId(anyString(), anyString())).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById(anyString())).willReturn(user);
        }

        @Test
        @DisplayName("배송 완료된 주문 삭제 성공")
        void deleteOrderWithDeliveredStatus_Success() {
            // Given
            Orders deliveredOrder = Orders.builder().id("order789").orderNumber(orderNumber).user(user).orderStatus(OrderStatus.DELIVERED).isHidden(false).build();
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)).willReturn(Optional.of(deliveredOrder));
            // ⭐️ 수정: save가 호출될 때, 저장될 객체를 그대로 반환하도록 설정
            given(orderRepository.save(any(Orders.class))).willAnswer(invocation -> {
                Orders orderToSave = invocation.getArgument(0);
                return orderToSave;
            });

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response.success()).isTrue();
            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertTrue(orderCaptor.getValue().getIsHidden());
        }

        @Test
        @DisplayName("결제 완료 상태 주문 삭제 실패 - 삭제 제한")
        void deleteOrderWithPaymentCompletedStatus_Fails() {
            // Given
            Orders paymentCompletedOrder = Orders.builder().id("order789").orderNumber(orderNumber).user(user).orderStatus(OrderStatus.PAYMENT_COMPLETED).isHidden(false).build();
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)).willReturn(Optional.of(paymentCompletedOrder));

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            // ⭐️ 수정: 예외 대신 응답 객체의 상태와 `save` 미호출을 검증
            assertThat(response.success()).isFalse();
            assertThat(response.message()).contains("결제가 완료된 주문은 삭제할 수 없습니다");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 삭제된 주문 재삭제 실패")
        void deleteAlreadyHiddenOrder_Fails() {
            // Given
            Orders hiddenOrder = Orders.builder().id("order789").orderNumber(orderNumber).user(user).orderStatus(OrderStatus.DELIVERED).isHidden(true).build();
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)).willReturn(Optional.of(hiddenOrder));

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            // ⭐️ 수정: 예외 대신 응답 객체의 상태와 `save` 미호출을 검증
            assertThat(response.success()).isFalse();
            assertThat(response.message()).contains("이미 삭제된 주문");
            verify(orderRepository, never()).save(any());
        }
    }
}