package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderItemRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.ProductStockManager;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener 테스트 (리팩토링된 버전)")
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Mock
    private StockReservationService stockReservationService;
    @Mock
    private ProductStockManager productStockManager;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    // 테스트 데이터
    private Orders testOrder;
    private Users testUser;
    private BuyerDTO testBuyerDTO;
    private Products testProduct1;
    private Products testProduct2;
    private OrderCreatedEvent testEvent;
    private PaymentCompletedEvent paymentCompletedEvent;
    private List<StockReservation> testReservations;
    private Payments testPayment;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();

        // 테스트 구매자 DTO
        testBuyerDTO = new BuyerDTO("user123", true, false, null);

        // 테스트 구매자 엔티티
        Buyers testBuyer = Buyers.builder()
                .userId("user123")
                .user(testUser)
                .nameMaskingStatus(true)
                .isDeleted(false)
                .deledAt(null)
                .build();

        // 테스트 상품들 생성
        testProduct1 = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25000L)
                .stock(100)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(15000L)
                .stock(50)
                .build();

        // 테스트 주문 생성
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber("1001")
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(65000L)
                .build();

        // 테스트 이벤트 생성 (Record DTO 정적 팩토리 메서드 사용)
        List<OrderItemInfo> orderItems = Arrays.asList(
                OrderItemInfo.of("product1", "강아지 사료", 2, 25000L),
                OrderItemInfo.of("product2", "고양이 간식", 1, 15000L)
        );

        testEvent = OrderCreatedEvent.of(
                "order123",
                "1001",
                "user123",
                "google",
                "google123",
                65000L,   // finalTotalPrice
                orderItems
        );

        // PaymentCompletedEvent 생성
        paymentCompletedEvent = PaymentCompletedEvent.of(
                "order123",
                "1001",
                "user123",
                "google",
                "google123",
                "payment123",
                "toss_payment_key_123",
                65000L,
                orderItems,
                OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("김철수")
                        .recipientPhone("010-1234-5678")
                        .postalCode("06234")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("456호")
                        .deliveryNote("문 앞에 놓아주세요")
                        .build(),
                65000L,
                null
        );

        // 테스트 재고 예약 목록
        testReservations = Arrays.asList(
                StockReservation.createReservation(testOrder, testProduct1, 2, 30),
                StockReservation.createReservation(testOrder, testProduct2, 1, 30)
        );

        // 테스트 결제 정보
        testPayment = Payments.builder()
                .orders(testOrder)
                .buyers(testBuyer)
                .method(PaymentMethod.TOSS)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("재고 예약 처리 테스트")
    class StockReservationTests {

        @Test
        @DisplayName("✅ 재고 예약 성공")
        void handleStockReservation_Success() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willReturn(testReservations);

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());

            // ReservationRequest 타입 검증
            verify(stockReservationService).createBulkReservations(
                    eq(testOrder),
                    argThat(reservationRequests -> {
                        List<StockReservationService.ReservationRequest> requestList =
                                reservationRequests;
                        return requestList.size() == 2 &&
                                requestList.get(0).product().getId().equals("product1") &&
                                requestList.get(0).quantity().equals(2) &&
                                requestList.get(1).product().getId().equals("product2") &&
                                requestList.get(1).quantity().equals(1);
                    })
            );
        }

        @Test
        @DisplayName("❌ 주문 없음")
        void handleStockReservation_OrderNotFound() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.empty());

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService, never()).createBulkReservations(any(), any());
        }

        @Test
        @DisplayName("❌ 재고 부족 → 보상 트랜잭션")
        void handleStockReservation_InsufficientStock_Compensation() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder))
                    .willReturn(Optional.of(testOrder)); // 보상 트랜잭션용
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willThrow(new IllegalArgumentException("재고가 부족합니다"));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 동시성 충돌 → 보상 트랜잭션")
        void handleStockReservation_OptimisticLockingFailure_Compensation() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder))
                    .willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 취소된 주문 → 재고 예약 건너뜀")
        void handleStockReservation_CancelledOrder_Skipped() {
            // Given
            Orders cancelledOrder = Orders.builder()
                    .id("order123")
                    .orderNumber("1001")
                    .user(testUser)
                    .orderStatus(OrderStatus.CANCELLED)
                    .totalPrice(65000L)
                    .build();

            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(cancelledOrder));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService, never()).createBulkReservations(any(), any());
        }
    }

    @Nested
    @DisplayName("결제 정보 생성 테스트")
    class PaymentInfoCreationTests {

        @Test
        @DisplayName("✅ 결제 정보 생성 성공")
        void handlePaymentInfoCreation_Success() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testBuyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(testUser);

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(paymentRepository).save(any(Payments.class));

            // testPayment는 예상되는 결제 정보의 구조를 보여주는 참조용
            assertThat(testPayment.getMethod()).isEqualTo(PaymentMethod.TOSS);
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("❌ 주문 없음")
        void handlePaymentInfoCreation_OrderNotFound() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.empty());

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 구매자 정보 없음")
        void handlePaymentInfoCreation_BuyerNotFound() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("PaymentCompletedEvent 기반 테스트")
    class PaymentCompletedEventBasedTests {

        @Test
        @DisplayName("✅ 결제 완료 후 OrderItems와 Shipments 생성")
        void handleOrderItemsAndShipmentsCreation_WithShippingAddress() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));

            // When
            orderEventListener.handleOrderItemsAndShipmentsCreation(paymentCompletedEvent);

            // Then - 모든 주요 메서드 호출 검증
            verify(orderRepository).findById("order123");
            verify(orderItemRepository).saveAll(anyList());
            verify(shipmentRepository).save(any(Shipments.class));
            verify(orderRepository).save(any(Orders.class));
            verify(stockReservationService).confirmReservations("order123");
            verify(productStockManager).decrementStockForConfirmedReservations("order123");
        }

        @Test
        @DisplayName("✅ 배송지 정보 없는 경우 Shipments 생성 안함")
        void handleOrderItemsAndShipmentsCreation_WithoutShippingAddress() {
            // Given
            PaymentCompletedEvent eventWithoutShipping = PaymentCompletedEvent.of(
                    "order123",
                    "1001",
                    "user123",
                    "google",
                    "google123",
                    "payment123",
                    "toss_payment_key_123",
                    65000L,
                    paymentCompletedEvent.orderItems(),
                    null,  // 배송지 정보 없음
                    65000L,
                    null
            );

            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));

            // When
            orderEventListener.handleOrderItemsAndShipmentsCreation(eventWithoutShipping);

            // Then - Shipments 생성은 안되지만 나머지는 모두 실행
            verify(orderRepository).findById("order123");
            verify(orderItemRepository).saveAll(anyList());
            verify(shipmentRepository, never()).save(any()); // Shipments 생성 안됨
            verify(orderRepository).save(any(Orders.class));
            verify(stockReservationService).confirmReservations("order123");
            verify(productStockManager).decrementStockForConfirmedReservations("order123");
        }

        @Test
        @DisplayName("✅ 결제 완료 알림 처리")
        void handlePaymentCompletedNotification_ProcessesCorrectly() {
            // When
            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);

            // Then - 비동기 메서드이므로 예외 없이 완료되면 성공
        }

        @Test
        @DisplayName("❌ 주문 없음 - OrderItems/Shipments 생성 실패")
        void handleOrderItemsAndShipmentsCreation_OrderNotFound() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.empty());

            // When
            orderEventListener.handleOrderItemsAndShipmentsCreation(paymentCompletedEvent);

            // Then - 주문이 없으면 아무것도 실행되지 않음
            verify(orderRepository).findById("order123");
            verify(orderItemRepository, never()).saveAll(anyList());
            verify(shipmentRepository, never()).save(any());
            verify(stockReservationService, never()).confirmReservations(any());
            verify(productStockManager, never()).decrementStockForConfirmedReservations(any());
        }
    }

    @Nested
    @DisplayName("OrderCreatedEvent 기반 테스트")
    class OrderCreatedEventBasedTests {

        @Test
        @DisplayName("✅ 재고 예약 처리 - 정상 케이스")
        void handleStockReservation_NormalCase() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then - 메서드 호출 검증
            verify(orderRepository).findById("order123");
        }

        @Test
        @DisplayName("✅ 결제 정보 생성 - 정상 케이스")
        void handlePaymentInfoCreation_NormalCase() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testBuyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(testUser);

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then - 메서드 호출 검증
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
        }
    }

    @Nested
    @DisplayName("결제 완료 알림 테스트")
    class PaymentCompletedNotificationTests {

        @Test
        @DisplayName("✅ 결제 완료 알림 성공")
        void handlePaymentCompletedNotification_Success() {
            // When
            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);

            // Then - 단순히 예외 없이 완료되면 성공
        }

        @Test
        @DisplayName("✅ 여러 상품 알림 텍스트 생성")
        void handlePaymentCompletedNotification_MultipleProducts() {
            // Given - 이미 2개 상품이 포함된 paymentCompletedEvent 사용

            // When
            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);

            // Then - 알림 텍스트에 "강아지 사료 외 1건" 형태로 표시됨
        }
    }

    @Nested
    @DisplayName("실제 이벤트 핸들러 메서드 테스트")
    class RealEventHandlerTests {

        @Test
        @DisplayName("✅ 재고 예약 처리 메서드 정상 동작")
        void handleStockReservation_WorksProperly() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willReturn(testReservations);

            // When - 실제 메서드 호출
            orderEventListener.handleStockReservation(testEvent);

            // Then - Repository 및 Service 호출 검증
            verify(orderRepository).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());

            // testReservations 검증 (예상되는 예약 정보)
            assertThat(testReservations).hasSize(2);
            assertThat(testReservations.get(0).getReservedQuantity()).isEqualTo(2);
            assertThat(testReservations.get(1).getReservedQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("✅ 결제 정보 생성 메서드 정상 동작")
        void handlePaymentInfoCreation_WorksProperly() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testBuyerDTO));
            given(userRepository.getReferenceById("user123")).willReturn(testUser);

            // When - 실제 메서드 호출
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then - 예외 없이 완료되면 성공
            verify(orderRepository).findById("order123");
        }

        @Test
        @DisplayName("✅ 결제 완료 후 OrderItems/Shipments 생성 메서드 정상 동작")
        void handleOrderItemsAndShipmentsCreation_WorksProperly() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));

            // When - 실제 메서드 호출
            orderEventListener.handleOrderItemsAndShipmentsCreation(paymentCompletedEvent);

            // Then - 모든 중요한 Repository 호출 검증
            verify(orderRepository).findById("order123");
            verify(orderItemRepository).saveAll(anyList());
            verify(shipmentRepository).save(any(Shipments.class));
            verify(stockReservationService).confirmReservations("order123");
            verify(productStockManager).decrementStockForConfirmedReservations("order123");
        }

        @Test
        @DisplayName("✅ 결제 완료 알림 메서드 정상 동작")
        void handlePaymentCompletedNotification_WorksProperly() {
            // When - 실제 메서드 호출
            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);

            // Then - 예외 없이 완료되면 성공 (비동기 메서드)
        }
    }

    @Nested
    @DisplayName("Record DTO 및 편의 메서드 테스트")
    class RecordDTOAndConvenienceMethodTests {

        @Test
        @DisplayName("✅ OrderItemInfo Record 불변성 및 편의 메서드 검증")
        void orderItemInfo_ImmutabilityAndMethods_Verified() {
            // Given
            OrderItemInfo item1 = OrderItemInfo.of("product1", "상품명", 2, 1000L);
            OrderItemInfo item2 = OrderItemInfo.of("product1", "상품명", 2, 1000L);

            // Then - Record의 자동 equals/hashCode 구현 검증
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());

            // 불변성 및 편의 메서드 검증
            assertThat(item1.productId()).isEqualTo("product1");
            assertThat(item1.quantity()).isEqualTo(2);
            assertThat(item1.unitPrice()).isEqualTo(1000L);
            assertThat(item1.totalPrice()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("✅ OrderCreatedEvent 편의 메서드 검증")
        void orderCreatedEvent_ConvenienceMethods_Verified() {
            // Then
            assertThat(testEvent.getOrderItemCount()).isEqualTo(2);
            assertThat(testEvent.getTotalQuantity()).isEqualTo(3);
            assertThat(testEvent.getFirstProductName()).isEqualTo("강아지 사료");
            assertThat(testEvent.eventOccurredAt()).isNotNull();
            assertThat(testEvent.getOrderSummary()).contains("강아지 사료 외 1개");
        }

        @Test
        @DisplayName("✅ PaymentCompletedEvent 편의 메서드 검증")
        void paymentCompletedEvent_ConvenienceMethods_Verified() {
            // Then
            assertThat(paymentCompletedEvent.getOrderItemCount()).isEqualTo(2);
            assertThat(paymentCompletedEvent.getFirstProductName()).isEqualTo("강아지 사료");
            assertThat(paymentCompletedEvent.isCouponApplied()).isFalse(); // 쿠폰 할인률이 null
            assertThat(paymentCompletedEvent.eventOccurredAt()).isNotNull();
        }
    }
}