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
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트 (리팩토링된 버전)")
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

    // 테스트 데이터
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
        // UserPrincipal 설정 (생성자 사용)
        principal = new UserPrincipal("google", "google123");

        // 테스트 사용자 생성
        user = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();

        // 테스트 구매자 DTO
        buyerDTO = new BuyerDTO("user123", true, false, null);

        // 테스트 상품들 생성
        product1 = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25000L)
                .stock(100)
                .build();

        product2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(15000L)
                .stock(50)
                .build();

        // 저장된 주문 Mock
        savedOrder = Orders.builder()
                .id("order123")
                .orderNumber("1001")
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(51000L)
                .build();

        // 주문 생성 요청 (쿠폰 15% 할인)
        requestWithCoupon = OrderCreateRequest.builder()
                .orderItems(Arrays.asList(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product2")
                                .quantity(1)
                                .build()
                ))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .couponDiscountRate(15.0)  // 15% 할인
                        .build())
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("김철수")
                        .recipientPhone("010-1234-5678")
                        .postalCode("06234")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("456호")
                        .deliveryNote("문 앞에 놓아주세요")
                        .build())
                .build();

        // 100% 할인 주문 요청
        request100Percent = OrderCreateRequest.builder()
                .orderItems(Arrays.asList(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product2")
                                .quantity(1)
                                .build()
                ))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .couponDiscountRate(100.0)  // 100% 할인
                        .build())
                .build();

        // OrderCreateResponse Mock
        mockResponse = OrderCreateResponse.builder()
                .orderId("order123")
                .orderNumber("1001")
                .totalPrice(51000L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .build();

        // 상세 조회용 테스트 데이터
        orderForDetail = Orders.builder()
                .id("order456")
                .orderNumber("2001")
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(30000L)
                .build();

        Products productForDetail = Products.builder()
                .id("product3")
                .title("고양이 사료")
                .price(15000L)
                .build();
    }

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTests {

        @Test
        @DisplayName("쿠폰 할인이 적용된 주문 생성 성공")
        void createOrderWithCouponDiscount_Success() throws JsonProcessingException {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(productRepository.findById("product2")).willReturn(Optional.of(product2));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            given(orderPendingDetailsRepository.save(any())).willReturn(any());
            // ObjectMapper Mock 설정 (checked exception 회피)
            doReturn("[]").when(objectMapper).writeValueAsString(anyList()); // orderItems JSON
            doReturn("{}").when(objectMapper).writeValueAsString(any(OrderCreateRequest.ShippingAddressRequest.class)); // shippingAddress JSON
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString()))
                    .willReturn(mockResponse);

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, requestWithCoupon);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("order123");
            assertThat(response.getTotalPrice()).isEqualTo(51000L);

            // Orders와 OrderPendingDetails 저장 검증
            verify(orderRepository).save(any(Orders.class));
            verify(orderPendingDetailsRepository).save(any());

            // ObjectMapper JSON 직렬화 검증
            verify(objectMapper, times(2)).writeValueAsString(any()); // orderItems + shippingAddress JSON 직렬화

            // ObjectMapper JSON 직렬화 검증
            verify(objectMapper).writeValueAsString(anyList()); // orderItems JSON 직렬화
            verify(objectMapper).writeValueAsString(any(OrderCreateRequest.ShippingAddressRequest.class)); // shippingAddress JSON 직렬화

            // 이벤트 발행 검증 (쿠폰 할인 정보 포함)
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.orderId()).isEqualTo("order123");
            assertThat(capturedEvent.orderNumber()).isEqualTo("1001");
            assertThat(capturedEvent.finalTotalPrice()).isEqualTo(51000L);
            assertThat(capturedEvent.getOrderItemCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("100% 쿠폰 할인으로 최소 결제 금액(1원) 적용")
        void createOrderWith100PercentDiscount_MinimumAmount() throws JsonProcessingException {
            // Given
            Orders order100Percent = Orders.builder()
                    .id("order789")
                    .orderNumber("1002")
                    .user(user)
                    .orderStatus(OrderStatus.PAYMENT_PENDING)
                    .totalPrice(1L)  // 최소 결제 금액
                    .build();

            OrderCreateResponse specificMockResponse = OrderCreateResponse.builder()
                    .orderId("order789")
                    .orderNumber("1002")
                    .totalPrice(1L)  // 최소 결제 금액
                    .orderStatus(OrderStatus.PAYMENT_PENDING)
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(productRepository.findById("product2")).willReturn(Optional.of(product2));
            given(orderRepository.save(any(Orders.class))).willReturn(order100Percent);
            given(orderPendingDetailsRepository.save(any())).willReturn(any());
            // ObjectMapper Mock 설정 (checked exception 회피)
            doReturn("[]").when(objectMapper).writeValueAsString(anyList()); // orderItems JSON
            doReturn("{}").when(objectMapper).writeValueAsString(any()); // shippingAddress JSON (null일 수도 있음)
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString()))
                    .willReturn(specificMockResponse);

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, request100Percent);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("order789");
            assertThat(response.getTotalPrice()).isEqualTo(1L);
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

            // Orders와 OrderPendingDetails 저장 검증
            verify(orderRepository).save(any(Orders.class));
            verify(orderPendingDetailsRepository).save(any());

            // 발행된 이벤트 검증
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.orderId()).isEqualTo("order789");
            assertThat(capturedEvent.orderNumber()).isEqualTo("1002");
            assertThat(capturedEvent.finalTotalPrice()).isEqualTo(1L);          // 최소 결제 금액
            assertThat(capturedEvent.getTotalPrice()).isEqualTo(1L);           // 하위 호환성 메서드
        }

        @Test
        @DisplayName("잘못된 쿠폰 할인률로 주문 생성 실패 - 100% 초과")
        void createOrderWithInvalidCouponRate_ThrowsException() throws JsonProcessingException {
            // Given
            OrderCreateRequest invalidRequest = requestWithCoupon.toBuilder()
                    .paymentInfo(requestWithCoupon.getPaymentInfo().toBuilder()
                            .couponDiscountRate(150.0)  // 150% 할인 (유효하지 않음)
                            .build())
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(productRepository.findById("product2")).willReturn(Optional.of(product2));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("쿠폰 할인률은 100%를 초과할 수 없습니다");

            // 주문 저장 및 이벤트 발행이 되지 않아야 함
            verify(orderRepository, never()).save(any());
            verify(orderPendingDetailsRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 주문 생성 실패")
        void createOrderWithNonExistentProduct_ThrowsException() throws JsonProcessingException {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.empty());  // 상품 없음

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");

            verify(orderRepository, never()).save(any());
            verify(orderPendingDetailsRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("구매자 권한이 없는 사용자의 주문 생성 실패")
        void createOrderWithoutBuyerRole_ThrowsException() throws JsonProcessingException {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());  // 구매자 권한 없음

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("구매자를 찾을 수 없습니다");

            verify(orderRepository, never()).save(any());
            verify(orderPendingDetailsRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("ObjectMapper JSON 직렬화 실패 테스트")
        void createOrder_JsonProcessingException() throws JsonProcessingException {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(productRepository.findById("product2")).willReturn(Optional.of(product2));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            // JsonProcessingException 발생 설정
            doThrow(new JsonProcessingException("JSON 직렬화 실패") {}).when(objectMapper).writeValueAsString(anyList());

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("주문 정보 저장 중 오류가 발생했습니다");

            verify(orderRepository).save(any(Orders.class));
            verify(objectMapper).writeValueAsString(anyList());
            // JSON 직렬화 실패로 인해 OrderPendingDetails 저장이 안됨
            verify(orderPendingDetailsRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 테스트")
    class GetOrderDetailTests {

        @Test
        @DisplayName("주문 상세 조회 성공")
        void getOrderDetail_Success() throws JsonProcessingException {
            // Given
            String orderNumber = "2001";
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "2001"))
                    .willReturn(Optional.of(orderForDetail));

            // When
            OrderDetailResponse response = orderService.getOrderDetail(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo("order456");
            assertThat(response.orderNumber()).isEqualTo("2001");
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(response.paymentInfo().totalPaymentAmount()).isEqualTo(30000L);

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, "2001");
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 실패")
        void getOrderDetail_OrderNotFound() throws JsonProcessingException {
            // Given
            String orderNumber = "9999";
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "9999"))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("구매자 권한 없는 사용자의 주문 조회 실패")
        void getOrderDetail_WithoutBuyerRole() throws JsonProcessingException {
            // Given
            String orderNumber = "2001";
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("구매자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 삭제 테스트")
    class DeleteOrderTests {

        @Test
        @DisplayName("배송 완료된 주문 삭제 성공")
        void deleteOrderWithDeliveredStatus_Success() throws JsonProcessingException {
            // Given
            String orderNumber = "2001";
            Orders deliveredOrder = Orders.builder()
                    .id("order789")
                    .orderNumber("2001")
                    .user(user)
                    .orderStatus(OrderStatus.DELIVERED)
                    .totalPrice(30000L)
                    .isHidden(false)
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "2001"))
                    .willReturn(Optional.of(deliveredOrder));

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.message()).contains("주문이 성공적으로 삭제되었습니다");

            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("결제 완료 상태 주문 삭제 실패 - 삭제 제한")
        void deleteOrderWithPaymentCompletedStatus_Fails() throws JsonProcessingException {
            // Given
            String orderNumber = "2001";
            Orders paymentCompletedOrder = Orders.builder()
                    .id("order789")
                    .orderNumber("2001")
                    .user(user)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                    .totalPrice(30000L)
                    .isHidden(false)
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "2001"))
                    .willReturn(Optional.of(paymentCompletedOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.deleteOrder(principal, orderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("현재 주문 상태에서는 삭제할 수 없습니다");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 삭제된 주문 재삭제 실패")
        void deleteAlreadyHiddenOrder_Fails() throws JsonProcessingException {
            // Given
            String orderNumber = "2001";
            Orders hiddenOrder = Orders.builder()
                    .id("order789")
                    .orderNumber("2001")
                    .user(user)
                    .orderStatus(OrderStatus.DELIVERED)
                    .totalPrice(30000L)
                    .isHidden(true)  // 이미 삭제됨
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "2001"))
                    .willReturn(Optional.of(hiddenOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.deleteOrder(principal, orderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 삭제된 주문입니다");

            verify(orderRepository, never()).save(any());
        }
    }
}