package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("주문 서비스 단위 테스트 (EDA + 쿠폰 할인)")
class OrderServiceTest {

    @InjectMocks
    OrderServiceImpl orderService;

    // EDA 전환 후 의존성들
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock BuyerRepository buyerRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TossPaymentResponseBuilder tossPaymentResponseBuilder;

    // 테스트 데이터
    Users user;
    BuyerDTO buyerDTO;
    UserPrincipal principal;
    Products product1, product2;
    OrderCreateRequest requestWithCoupon, requestWithoutCoupon;
    Orders savedOrder;
    OrderCreateResponse mockResponse;
    Orders orderForDetail;
    OrderItems orderItem;
    Products productForDetail;
    final Long orderNumber = 20250630123456L;

    @BeforeEach
    void setUp() {
        // 사용자 데이터
        user = Users.builder()
                .id("user123")
                .name("김철수")
                .provider("google")
                .providerId("google123")
                .role(Role.ROLE_BUYER)
                .build();

        // 구매자 DTO (BuyerRepository 응답용) - OffsetDateTime 사용
        buyerDTO = new BuyerDTO("user123", true, false, OffsetDateTime.now(ZoneOffset.UTC));

        principal = new UserPrincipal("google", "google123");

        // 상품 데이터 (원가만 사용)
        product1 = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25_000L)
                .stock(100)
                .build();

        product2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(10_000L)
                .stock(50)
                .build();

        // 쿠폰 할인 있는 주문 요청 (15% 할인)
        requestWithCoupon = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(2)  // 25,000 * 2 = 50,000원
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product2")
                                .quantity(1)  // 10,000 * 1 = 10,000원
                                .build()))
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("김철수")
                        .recipientPhone("010-1234-5678")
                        .postalCode("06234")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("456호")
                        .deliveryNote("문 앞에 놓아주세요")
                        .build())
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료 외 1건")
                        .couponDiscountRate(15.0)  // 15% 할인
                        .customerName("김철수")
                        .customerEmail("test@example.com")
                        .build())
                .build();

        // 쿠폰 할인 없는 주문 요청
        requestWithoutCoupon = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(1)  // 25,000원
                                .build()))
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("김철수")
                        .recipientPhone("010-1234-5678")
                        .postalCode("06234")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("456호")
                        .deliveryNote("문 앞에 놓아주세요")
                        .build())
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료")
                        .couponDiscountRate(null)  // 할인 없음
                        .customerName("김철수")
                        .customerEmail("test@example.com")
                        .build())
                .build();

        // 저장된 주문 데이터
        savedOrder = Orders.builder()
                .id("order123")
                .orderNumber(20250625123456789L)
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(51_000L)  // 15% 할인 적용된 금액 (60,000 * 0.85)
                .build();

        // 응답 데이터
        mockResponse = OrderCreateResponse.builder()
                .orderId("order123")
                .orderNumber(20250625123456789L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(51_000L)
                .build();

        // 주문 상세 조회용 상품
        productForDetail = Products.builder()
                .id("product3")
                .title("고양이 사료")
                .price(15000L)
                .stock(80)
                .build();

        // 주문 아이템
        orderItem = OrderItems.builder()
                .id("orderItem1")
                .products(productForDetail)
                .quantity(2)
                .price(15000L)
                .build();

        // 주문 상세 조회용 주문
        orderForDetail = Orders.builder()
                .id("order123")
                .orderNumber(orderNumber)
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(33000L) // 상품가격 30000 + 배송비 3000
                .recipientName("김철수")
                .recipientPhone("010-1234-5678")
                .shippingAddress("서울시 강남구 테헤란로 123")
                .detailAddress("456호")
                .deliveryNote("문 앞에 놓아주세요")
                .isHidden(false)
                .hiddenAt(null)
                .orderItems(List.of(orderItem))
                .build();

        // OrderItems와 Orders 간 양방향 연관관계 설정
        orderItem = OrderItems.builder()
                .id("orderItem1")
                .products(productForDetail)
                .quantity(2)
                .price(15000L)
                .orders(orderForDetail)
                .build();
    }

    @Test
    @DisplayName("쿠폰 할인이 적용된 주문 생성 성공")
    void createOrderWithCouponDiscount_Success() {
        // Given
        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.of(buyerDTO));
        given(userRepository.getReferenceById("user123")).willReturn(user);
        given(productRepository.findById("product1")).willReturn(Optional.of(product1));
        given(productRepository.findById("product2")).willReturn(Optional.of(product2));
        given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);
        given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString()))
                .willReturn(mockResponse);

        // When
        OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, requestWithCoupon);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order123");
        assertThat(response.getTotalPrice()).isEqualTo(51_000L);  // 15% 할인 적용

        // 이벤트 발행 검증 (쿠폰 할인 정보 포함)
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOriginalTotalPrice()).isEqualTo(60_000L);  // 원가
        assertThat(capturedEvent.getCouponDiscountRate()).isEqualTo(15.0);    // 할인률
        assertThat(capturedEvent.getFinalTotalPrice()).isEqualTo(51_000L);    // 최종 가격
        assertThat(capturedEvent.getTotalPrice()).isEqualTo(51_000L);         // 하위 호환성 메서드
        assertThat(capturedEvent.isCouponApplied()).isTrue();

        // 할인 금액 계산 검증
        Long expectedDiscountAmount = 60_000L - 51_000L;
        assertThat(expectedDiscountAmount).isEqualTo(9_000L);
    }

    @Test
    @DisplayName("쿠폰 할인 없는 주문 생성 성공")
    void createOrderWithoutCouponDiscount_Success() {
        // Given
        Orders orderWithoutDiscount = Orders.builder()
                .id("order456")
                .orderNumber(20250625123456790L)
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(25_000L)  // 할인 없음
                .build();

        OrderCreateResponse responseWithoutDiscount = OrderCreateResponse.builder()
                .orderId("order456")
                .orderNumber(20250625123456790L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(25_000L)
                .build();

        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.of(buyerDTO));
        given(userRepository.getReferenceById("user123")).willReturn(user);
        given(productRepository.findById("product1")).willReturn(Optional.of(product1));
        given(orderRepository.save(any(Orders.class))).willReturn(orderWithoutDiscount);
        given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString()))
                .willReturn(responseWithoutDiscount);

        // When
        OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, requestWithoutCoupon);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order456");
        assertThat(response.getTotalPrice()).isEqualTo(25_000L);  // 할인 없음

        // 이벤트 발행 검증 (할인 없음)
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOriginalTotalPrice()).isEqualTo(25_000L);  // 원가
        assertThat(capturedEvent.getCouponDiscountRate()).isNull();            // 할인률 없음
        assertThat(capturedEvent.getFinalTotalPrice()).isEqualTo(25_000L);     // 최종 가격
        assertThat(capturedEvent.getTotalPrice()).isEqualTo(25_000L);          // 하위 호환성 메서드
        assertThat(capturedEvent.isCouponApplied()).isFalse();
    }

    @Test
    @DisplayName("100% 쿠폰 할인 시 최소 결제 금액 1원 보장")
    void createOrderWith100PercentDiscount_MinimumPayment() {
        // Given
        OrderCreateRequest request100Percent = requestWithCoupon.toBuilder()
                .paymentInfo(requestWithCoupon.getPaymentInfo().toBuilder()
                        .couponDiscountRate(100.0)  // 100% 할인
                        .build())
                .build();

        Orders orderWith1Won = Orders.builder()
                .id("order789")
                .orderNumber(20250625123456791L)
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(1L)  // 최소 결제 금액 1원
                .build();

        // 이 테스트 케이스에 맞는 mockResponse를 생성합니다.
        OrderCreateResponse specificMockResponse = OrderCreateResponse.builder()
                .orderId("order789")
                .orderNumber(20250625123456791L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(1L)
                .build();

        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.of(buyerDTO));
        given(userRepository.getReferenceById("user123")).willReturn(user);
        given(productRepository.findById("product1")).willReturn(Optional.of(product1));
        given(productRepository.findById("product2")).willReturn(Optional.of(product2));
        given(orderRepository.save(any(Orders.class))).willReturn(orderWith1Won);
        // buildTossPaymentResponse가 이 테스트에 맞는 응답을 반환하도록 설정합니다.
        given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(Orders.class), any(), anyString()))
                .willReturn(specificMockResponse);

        // When
        OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, request100Percent);

        // Then

        // 1. 반환된 response 객체를 직접 검증하는 코드 (추가된 부분)
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order789");
        assertThat(response.getTotalPrice()).isEqualTo(1L);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

        // 2. 발행된 이벤트를 검증하는 코드 (기존 코드)
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOriginalTotalPrice()).isEqualTo(60_000L);  // 원가
        assertThat(capturedEvent.getCouponDiscountRate()).isEqualTo(100.0);    // 100% 할인
        assertThat(capturedEvent.getFinalTotalPrice()).isEqualTo(1L);          // 최소 결제 금액
        assertThat(capturedEvent.getTotalPrice()).isEqualTo(1L);               // 하위 호환성 메서드
        assertThat(capturedEvent.isCouponApplied()).isTrue();
    }

    @Test
    @DisplayName("잘못된 쿠폰 할인률로 주문 생성 실패 - 100% 초과")
    void createOrderWithInvalidCouponRate_ThrowsException() {
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 생성 실패")
    void createOrderWithNonExistentProduct_ThrowsException() {
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("구매자 권한이 없는 사용자의 주문 생성 실패")
    void createOrderWithoutBuyerRole_ThrowsException() {
        // Given
        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.empty());  // 구매자 권한 없음

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, requestWithCoupon))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("구매자를 찾을 수 없거나 권한이 없습니다");

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Nested
    @DisplayName("주문 상세 조회 테스트")
    class OrderDetailTests {

        @Test
        @DisplayName("✅ 주문 상세 조회 성공")
        void getOrderDetail_Success() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.of(orderForDetail));

            // When
            OrderDetailResponse response = orderService.getOrderDetail(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo("order123");
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);

            // 받는 사람 정보 검증
            assertThat(response.recipientInfo()).isNotNull();
            assertThat(response.recipientInfo().recipientName()).isEqualTo("김철수");
            assertThat(response.recipientInfo().recipientPhone()).isEqualTo("010-1234-5678");
            assertThat(response.recipientInfo().fullAddress()).contains("서울시 강남구 테헤란로 123");
            assertThat(response.recipientInfo().deliveryRequest()).isEqualTo("문 앞에 놓아주세요");

            // 주문 상품 정보 검증
            assertThat(response.orderItems()).hasSize(1);
            OrderDetailResponse.OrderItemDetail orderItemDetail = response.orderItems().get(0);
            assertThat(orderItemDetail.productName()).isEqualTo("고양이 사료");
            assertThat(orderItemDetail.quantity()).isEqualTo(2);
            assertThat(orderItemDetail.unitPrice()).isEqualTo(15000L);
            assertThat(orderItemDetail.totalPrice()).isEqualTo(30000L);

            // 결제 정보 검증
            assertThat(response.paymentInfo()).isNotNull();
            assertThat(response.paymentInfo().totalProductPrice()).isEqualTo(30000L);

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, orderNumber);
        }

        @Test
        @DisplayName("❌ 주문 상세 조회 실패 - 구매자 없음")
        void getOrderDetail_BuyerNotFound() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("구매자 정보를 찾을 수 없습니다.");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository, never()).findById(anyString());
            verify(orderRepository, never()).findOrderDetailByUserAndOrderNumber(any(), any());
        }

        @Test
        @DisplayName("❌ 주문 상세 조회 실패 - 사용자 없음")
        void getOrderDetail_UserNotFound() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("사용자 정보를 찾을 수 없습니다.");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository, never()).findOrderDetailByUserAndOrderNumber(any(), any());
        }

        @Test
        @DisplayName("❌ 주문 상세 조회 실패 - 주문 없음")
        void getOrderDetail_OrderNotFound() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDetail(principal, orderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("주문을 찾을 수 없습니다.");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, orderNumber);
        }

        @Test
        @DisplayName("✅ 배송지 정보가 없는 경우 기본값 반환")
        void getOrderDetail_WithoutShippingInfo() {
            // Given
            Orders orderWithoutShipping = Orders.builder()
                    .id(orderForDetail.getId())
                    .orderNumber(orderForDetail.getOrderNumber())
                    .user(orderForDetail.getUser())
                    .orderStatus(orderForDetail.getOrderStatus())
                    .totalPrice(orderForDetail.getTotalPrice())
                    .recipientName(null)
                    .recipientPhone(null)
                    .shippingAddress(null)
                    .detailAddress(null)
                    .deliveryNote(null)
                    .isHidden(orderForDetail.getIsHidden())
                    .hiddenAt(orderForDetail.getHiddenAt())
                    .orderItems(orderForDetail.getOrderItems())
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.of(orderWithoutShipping));

            // When
            OrderDetailResponse response = orderService.getOrderDetail(principal, orderNumber);

            // Then
            assertThat(response.recipientInfo().recipientName()).isEqualTo("수령인 미등록");
            assertThat(response.recipientInfo().recipientPhone()).isEqualTo("연락처 미등록");
            assertThat(response.recipientInfo().fullAddress()).isEqualTo("주소 미등록");
            assertThat(response.recipientInfo().deliveryRequest()).isEqualTo("배송 요청사항 없음");
        }
    }

    @Nested
    @DisplayName("주문 내역 삭제 테스트")
    class OrderDeleteTests {

        @Test
        @DisplayName("✅ 주문 내역 삭제 성공")
        void deleteOrder_Success() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.of(orderForDetail));

            // 숨김 처리된 주문 반환을 위한 설정
            Orders hiddenOrder = Orders.builder()
                    .id(orderForDetail.getId())
                    .orderNumber(orderForDetail.getOrderNumber())
                    .user(orderForDetail.getUser())
                    .orderStatus(orderForDetail.getOrderStatus())
                    .totalPrice(orderForDetail.getTotalPrice())
                    .recipientName(orderForDetail.getRecipientName())
                    .recipientPhone(orderForDetail.getRecipientPhone())
                    .shippingAddress(orderForDetail.getShippingAddress())
                    .detailAddress(orderForDetail.getDetailAddress())
                    .deliveryNote(orderForDetail.getDeliveryNote())
                    .isHidden(true)
                    .hiddenAt(LocalDateTime.now())
                    .orderItems(orderForDetail.getOrderItems())
                    .build();
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(hiddenOrder);

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.orderId()).isEqualTo("order123");
            assertThat(response.hiddenAt()).isNotNull();
            assertThat(response.message()).isEqualTo("주문 내역이 성공적으로 삭제되었습니다.");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, orderNumber);
            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 주문 내역 삭제 실패 - 구매자 없음")
        void deleteOrder_BuyerNotFound() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.orderId()).isNull();
            assertThat(response.hiddenAt()).isNull();
            assertThat(response.message()).isEqualTo("구매자를 찾을 수 없거나 권한이 없습니다");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository, never()).findById(anyString());
            verify(orderRepository, never()).findOrderDetailByUserAndOrderNumber(any(), any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 주문 내역 삭제 실패 - 사용자 없음")
        void deleteOrder_UserNotFound() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.empty());

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.message()).isEqualTo("사용자를 찾을 수 없습니다");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository, never()).findOrderDetailByUserAndOrderNumber(any(), any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 주문 내역 삭제 실패 - 주문 없음")
        void deleteOrder_OrderNotFound() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.empty());

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.message()).isEqualTo("주문을 찾을 수 없거나 접근 권한이 없습니다");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, orderNumber);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 주문 내역 삭제 실패 - 이미 숨겨진 주문")
        void deleteOrder_AlreadyHidden() {
            // Given
            Orders hiddenOrder = Orders.builder()
                    .id(orderForDetail.getId())
                    .orderNumber(orderForDetail.getOrderNumber())
                    .user(orderForDetail.getUser())
                    .orderStatus(orderForDetail.getOrderStatus())
                    .totalPrice(orderForDetail.getTotalPrice())
                    .recipientName(orderForDetail.getRecipientName())
                    .recipientPhone(orderForDetail.getRecipientPhone())
                    .shippingAddress(orderForDetail.getShippingAddress())
                    .detailAddress(orderForDetail.getDetailAddress())
                    .deliveryNote(orderForDetail.getDeliveryNote())
                    .isHidden(true)
                    .hiddenAt(LocalDateTime.now().minusDays(1))
                    .orderItems(orderForDetail.getOrderItems())
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.of(hiddenOrder));

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.message()).isEqualTo("이미 삭제된 주문 내역입니다");

            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).findById("user123");
            verify(orderRepository).findOrderDetailByUserAndOrderNumber(user, orderNumber);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 주문 내역 삭제 실패 - 예상치 못한 오류")
        void deleteOrder_UnexpectedError() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.findById("user123"))
                    .willReturn(Optional.of(user));
            given(orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber))
                    .willReturn(Optional.of(orderForDetail));
            given(orderRepository.save(any(Orders.class)))
                    .willThrow(new RuntimeException("데이터베이스 연결 오류"));

            // When
            OrderDeleteResponse response = orderService.deleteOrder(principal, orderNumber);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.orderNumber()).isEqualTo(orderNumber);
            assertThat(response.message()).isEqualTo("주문 내역 삭제 중 서버 오류가 발생했습니다");

            verify(orderRepository).save(any(Orders.class));
        }
    }

    @Nested
    @DisplayName("엔티티 메서드 테스트")
    class EntityMethodTests {

        @Test
        @DisplayName("✅ Orders 엔티티 숨김 기능 메서드 테스트")
        void testOrderHiddenMethods() {
            // Given
            Orders testOrder = Orders.builder()
                    .id("testOrder")
                    .orderNumber(12345L)
                    .isHidden(false)
                    .hiddenAt(null)
                    .build();

            // When & Then - 초기 상태 확인
            assertThat(testOrder.isOrderHidden()).isFalse();
            assertThat(testOrder.canBeHidden()).isTrue();

            // When - 숨김 처리
            testOrder.hideOrder();

            // Then - 숨김 처리 후 상태 확인
            assertThat(testOrder.isOrderHidden()).isTrue();
            assertThat(testOrder.canBeHidden()).isFalse();
            assertThat(testOrder.getHiddenAt()).isNotNull();

            // When - 숨김 해제
            testOrder.unhideOrder();

            // Then - 숨김 해제 후 상태 확인
            assertThat(testOrder.isOrderHidden()).isFalse();
            assertThat(testOrder.canBeHidden()).isTrue();
            assertThat(testOrder.getHiddenAt()).isNull();
        }

        @Test
        @DisplayName("✅ Orders 엔티티 배송지 정보 메서드 테스트")
        void testOrderShippingMethods() {
            // Given
            Orders testOrder = Orders.builder()
                    .id("testOrder")
                    .build();

            // When
            testOrder.setShippingInfo(
                    "홍길동",
                    "010-9999-8888",
                    "12345",
                    "서울시 종로구 종로 1",
                    "101동 202호",
                    "경비실에 맡겨주세요"
            );

            // Then
            assertThat(testOrder.getRecipientName()).isEqualTo("홍길동");
            assertThat(testOrder.getRecipientPhone()).isEqualTo("010-9999-8888");
            assertThat(testOrder.getPostalCode()).isEqualTo("12345");
            assertThat(testOrder.getShippingAddress()).isEqualTo("서울시 종로구 종로 1");
            assertThat(testOrder.getDetailAddress()).isEqualTo("101동 202호");
            assertThat(testOrder.getDeliveryNote()).isEqualTo("경비실에 맡겨주세요");

            // 전체 배송 주소 조합 테스트
            String fullAddress = testOrder.getFullShippingAddress();
            assertThat(fullAddress).isEqualTo("서울시 종로구 종로 1 101동 202호");
        }

        @Test
        @DisplayName("✅ 전체 배송 주소 조합 - 상세 주소 없는 경우")
        void testFullShippingAddress_WithoutDetailAddress() {
            // Given
            Orders testOrder = Orders.builder()
                    .shippingAddress("서울시 강남구 테헤란로 123")
                    .detailAddress(null)
                    .build();

            // When
            String fullAddress = testOrder.getFullShippingAddress();

            // Then
            assertThat(fullAddress).isEqualTo("서울시 강남구 테헤란로 123");
        }

        @Test
        @DisplayName("✅ 전체 배송 주소 조합 - 기본 주소 없는 경우")
        void testFullShippingAddress_WithoutBaseAddress() {
            // Given
            Orders testOrder = Orders.builder()
                    .shippingAddress(null)
                    .detailAddress("101동 202호")
                    .build();

            // When
            String fullAddress = testOrder.getFullShippingAddress();

            // Then
            assertThat(fullAddress).isEmpty();
        }
    }
}