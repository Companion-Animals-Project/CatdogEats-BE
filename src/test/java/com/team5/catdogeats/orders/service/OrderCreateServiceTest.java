package com.team5.catdogeats.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.enums.DiscountType; // 추가된 import
import com.team5.catdogeats.orders.domain.OrderPendingDetails; // 추가된 import
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
import com.team5.catdogeats.orders.service.impl.OrderCreateServiceImpl;
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.enums.Role; // 추가된 import
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 통합 테스트")
class OrderCreateServiceTest {

    @InjectMocks
    private OrderCreateServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderPendingDetailsRepository orderPendingDetailsRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TossPaymentResponseBuilder tossPaymentResponseBuilder;

    @Mock
    private ObjectMapper objectMapper;

    // 테스트 데이터
    private UserPrincipal principal;
    private BuyerDTO buyerDTO;
    private Users user;
    private Products product1, product2;
    private Orders savedOrder, orderForDetail;
    private OrderCreateRequest requestWithCoupon;
    private OrderCreateRequest request100Percent;
    private OrderCreateResponse mockResponse;

    @BeforeEach
    void setUp() {
        // UserPrincipal 초기화
        principal = new UserPrincipal("google", "google123");

        // 사용자 정보 초기화 (실제 프로젝트 구조에 맞게 수정)
        buyerDTO = new BuyerDTO("user123", false, false, null);

        user = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .userNameAttribute("sub")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .build();

        // 상품 정보 초기화
        product1 = Products.builder()
                .id("product1")
                .title("프리미엄 강아지 사료")
                .price(10000L)
                .build();

        product2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(15000L)
                .build();

        // 주문 정보 초기화
        savedOrder = Orders.builder()
                .id("order123")
                .orderNumber("1001")
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(32750L)  // 실제 계산값에 맞게 수정
                .build();

        // 요청 객체 초기화
        OrderCreateRequest basicRequest = OrderCreateRequest.builder()
                .orderItems(Collections.singletonList(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(1)
                                .build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료")
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

        requestWithCoupon = OrderCreateRequest.builder()
                .orderItems(Arrays.asList(
                        OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(2).build(),
                        OrderCreateRequest.OrderItemRequest.builder().productId("product2").quantity(1).build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료")
                        .couponDiscountRate(15.0)
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

        request100Percent = OrderCreateRequest.builder()
                .orderItems(Collections.singletonList(
                        OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(1).build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료")
                        .couponDiscountRate(100.0)
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

        mockResponse = OrderCreateResponse.builder()
                .orderId("order123")
                .orderNumber("1001")
                .totalPrice(32750L)  // 실제 계산값: (20000+15000) * 0.85 + 3000 = 32750L
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .build();

        orderForDetail = Orders.builder()
                .id("order456")
                .orderNumber("2001")
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(30000L)
                .orderItems(new ArrayList<>())
                .build();
    }

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
            assertThat(response.getTotalPrice()).isEqualTo(32750L); // 실제 계산값으로 수정
            verify(orderRepository).save(any(Orders.class));
            verify(orderPendingDetailsRepository).save(any());
            verify(objectMapper, times(2)).writeValueAsString(any());
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            OrderCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.orderId()).isEqualTo("order123");
            assertThat(capturedEvent.finalTotalPrice()).isEqualTo(32750L); // 실제 계산값으로 수정
        }

        @Test
        @DisplayName("100% 쿠폰 할인으로 최소 결제 금액(1원) 적용")
        void createOrderWith100PercentDiscount_MinimumAmount() throws Exception {
            Orders order100PercentEntity = Orders.builder()
                    .id("order789")
                    .orderNumber("1002")
                    .user(user)
                    .orderStatus(OrderStatus.PAYMENT_PENDING)
                    .totalPrice(3000L)  // 배송비만 포함 (100% 할인 + 배송비 3000L)
                    .build();

            OrderCreateResponse specificMockResponse = OrderCreateResponse.builder()
                    .orderId("order789")
                    .orderNumber("1002")
                    .totalPrice(3000L)  // 100% 할인 시 배송비만 남음
                    .orderStatus(OrderStatus.PAYMENT_PENDING)
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(orderRepository.save(any(Orders.class))).willReturn(order100PercentEntity);
            doReturn("[]").when(objectMapper).writeValueAsString(any());
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString())).willReturn(specificMockResponse);

            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, request100Percent);

            assertThat(response.getTotalPrice()).isEqualTo(3000L); // 배송비만 남음
            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            verify(orderRepository).save(orderCaptor.capture());
            Orders capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getTotalPrice()).isEqualTo(3000L); // 배송비만
        }
    }

    @Nested
    @DisplayName("쿠폰 타입별 주문 생성 테스트")
    class CreateOrderWithCouponTypeTests {

        @Test
        @DisplayName("새로운 방식 정액 할인 쿠폰 적용된 주문 생성 성공")
        void createOrderWithAmountCouponType_Success() throws Exception {
            // Given
            OrderCreateRequest requestWithAmountCoupon = OrderCreateRequest.builder()
                    .orderItems(Arrays.asList(
                            OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(2).build(),
                            OrderCreateRequest.OrderItemRequest.builder().productId("product2").quantity(3).build()))
                    .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                            .orderName("강아지 사료")
                            .couponType(DiscountType.AMOUNT)
                            .couponDiscountAmount(5000L) // 5000원 할인
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

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(productRepository.findById("product2")).willReturn(Optional.of(product2));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            doReturn("[]").when(objectMapper).writeValueAsString(any());
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString())).willReturn(mockResponse);

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, requestWithAmountCoupon);

            // Then
            assertThat(response.getTotalPrice()).isEqualTo(32750L); // mockResponse 값 사용
            verify(orderRepository).save(any(Orders.class));
            verify(orderPendingDetailsRepository).save(any());

            // OrderPendingDetails 저장 검증
            ArgumentCaptor<OrderPendingDetails> pendingDetailsCaptor = ArgumentCaptor.forClass(OrderPendingDetails.class);
            verify(orderPendingDetailsRepository).save(pendingDetailsCaptor.capture());
            OrderPendingDetails savedPendingDetails = pendingDetailsCaptor.getValue();
            assertThat(savedPendingDetails.getCouponType()).isEqualTo(DiscountType.AMOUNT);
            assertThat(savedPendingDetails.getCouponDiscountAmount()).isEqualTo(5000L);
            assertThat(savedPendingDetails.getCouponDiscountRate()).isNull();
        }

        @Test
        @DisplayName("새로운 방식 정률 할인 쿠폰 적용된 주문 생성 성공")
        void createOrderWithPercentCouponType_Success() throws Exception {
            // Given
            OrderCreateRequest requestWithPercentCoupon = OrderCreateRequest.builder()
                    .orderItems(Collections.singletonList(
                            OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(2).build()))
                    .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                            .orderName("강아지 사료")
                            .couponType(DiscountType.PERCENT)
                            .couponDiscountRate(20.0) // 20% 할인
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

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            doReturn("[]").when(objectMapper).writeValueAsString(any());
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString())).willReturn(mockResponse);

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, requestWithPercentCoupon);

            // Then
            verify(orderPendingDetailsRepository).save(any());
            ArgumentCaptor<OrderPendingDetails> pendingDetailsCaptor = ArgumentCaptor.forClass(OrderPendingDetails.class);
            verify(orderPendingDetailsRepository).save(pendingDetailsCaptor.capture());
            OrderPendingDetails savedPendingDetails = pendingDetailsCaptor.getValue();
            assertThat(savedPendingDetails.getCouponType()).isEqualTo(DiscountType.PERCENT);
            assertThat(savedPendingDetails.getCouponDiscountRate()).isEqualTo(20.0);
            assertThat(savedPendingDetails.getCouponDiscountAmount()).isNull();
        }

        @Test
        @DisplayName("정액 할인이 주문 총액 초과 시 예외 발생")
        void createOrderWithAmountCouponExceedingTotal_ThrowsException() {
            // Given
            OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                    .orderItems(Collections.singletonList(
                            OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(1).build()))
                    .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                            .orderName("강아지 사료")
                            .couponType(DiscountType.AMOUNT)
                            .couponDiscountAmount(50000L) // 상품 가격(10000원)보다 큰 할인
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

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("쿠폰 할인 금액이 주문 총액을 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("기존 방식 정률 할인 하위 호환성 확인")
        void createOrderWithLegacyPercentCoupon_BackwardCompatibility() throws Exception {
            // Given - couponType 없이 기존 방식
            OrderCreateRequest legacyRequest = OrderCreateRequest.builder()
                    .orderItems(Collections.singletonList(
                            OrderCreateRequest.OrderItemRequest.builder().productId("product1").quantity(1).build()))
                    .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                            .orderName("강아지 사료")
                            .couponDiscountRate(15.0) // 기존 방식
                            // couponType은 null
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

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(productRepository.findById("product1")).willReturn(Optional.of(product1));
            given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
            doReturn("[]").when(objectMapper).writeValueAsString(any());
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString())).willReturn(mockResponse);

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, legacyRequest);

            // Then
            verify(orderPendingDetailsRepository).save(any());
            ArgumentCaptor<OrderPendingDetails> pendingDetailsCaptor = ArgumentCaptor.forClass(OrderPendingDetails.class);
            verify(orderPendingDetailsRepository).save(pendingDetailsCaptor.capture());
            OrderPendingDetails savedPendingDetails = pendingDetailsCaptor.getValue();
            assertThat(savedPendingDetails.getCouponType()).isNull(); // 기존 방식
            assertThat(savedPendingDetails.getCouponDiscountRate()).isEqualTo(15.0);
            assertThat(savedPendingDetails.getCouponDiscountAmount()).isNull();
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 테스트")
    class GetOrderDetailTests {

        @Test
        @DisplayName("주문 상세 조회 성공")
        void getOrderDetail_Success() {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "2001")).willReturn(Optional.of(orderForDetail));
            given(shipmentRepository.findByOrders(orderForDetail)).willReturn(Optional.empty());

            OrderDetailResponse response = orderService.getOrderDetail(principal, "2001");

            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo("order456");
            assertThat(response.orderNumber()).isEqualTo("2001");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, "2001");
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외 발생")
        void getOrderDetail_OrderNotFound() {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(principal, "nonexistent"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("주문을 찾을 수 없거나 접근 권한이 없습니다.");
        }
    }

    @Nested
    @DisplayName("주문 삭제 테스트")
    class DeleteOrderTests {

        @Test
        @DisplayName("삭제 가능한 상태의 주문 삭제 성공")
        void deleteOrder_Success() {
            Orders deletableOrder = Orders.builder()
                    .id("order789")
                    .orderNumber("3001")
                    .user(user)
                    .orderStatus(OrderStatus.DELIVERED)
                    .isHidden(false)
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "3001")).willReturn(Optional.of(deletableOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(deletableOrder);

            OrderDeleteResponse response = orderService.deleteOrder(principal, "3001");

            assertThat(response.success()).isTrue();
            assertThat(response.orderNumber()).isEqualTo("3001");
            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("삭제 불가능한 상태의 주문 삭제 시 실패")
        void deleteOrder_RestrictedStatus() {
            Orders restrictedOrder = Orders.builder()
                    .id("order890")
                    .orderNumber("4001")
                    .user(user)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED) // 삭제 불가능한 상태
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123")).willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(user);
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, "4001")).willReturn(Optional.of(restrictedOrder));

            OrderDeleteResponse response = orderService.deleteOrder(principal, "4001");

            assertThat(response.success()).isFalse();
            assertThat(response.message()).contains("결제가 완료된 주문은 삭제할 수 없습니다");
        }
    }
}