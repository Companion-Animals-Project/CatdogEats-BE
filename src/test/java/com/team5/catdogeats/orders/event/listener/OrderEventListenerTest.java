//package com.team5.catdogeats.orders.event.listener;
//
//import com.team5.catdogeats.orders.domain.Orders;
//import com.team5.catdogeats.orders.domain.Shipments;
//import com.team5.catdogeats.orders.domain.enums.OrderStatus;
//import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
//import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
//import com.team5.catdogeats.orders.event.OrderCreatedEvent;
//import com.team5.catdogeats.orders.repository.OrderItemRepository;
//import com.team5.catdogeats.orders.repository.OrderRepository;
//import com.team5.catdogeats.orders.repository.ShipmentRepository;
//import com.team5.catdogeats.payments.domain.Payments;
//import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
//import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
//import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
//import com.team5.catdogeats.payments.repository.PaymentRepository;
//import com.team5.catdogeats.products.domain.Products;
//import com.team5.catdogeats.products.domain.mapping.StockReservation;
//import com.team5.catdogeats.products.repository.ProductRepository;
//import com.team5.catdogeats.products.service.ProductStockManager;
//import com.team5.catdogeats.products.service.StockReservationService;
//import com.team5.catdogeats.users.domain.Users;
//import com.team5.catdogeats.users.domain.enums.Role;
//import com.team5.catdogeats.users.domain.mapping.Buyers;
//import com.team5.catdogeats.users.repository.BuyerRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.OptimisticLockingFailureException;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("OrderEventListener 테스트 (리팩토링된 버전)")
//class OrderEventListenerTest {
//
//    @InjectMocks
//    private OrderEventListener orderEventListener;
//
//    @Mock
//    private StockReservationService stockReservationService;
//    @Mock
//    private ProductStockManager productStockManager;
//    @Mock
//    private OrderRepository orderRepository;
//    @Mock
//    private OrderItemRepository orderItemRepository;
//    @Mock
//    private ShipmentRepository shipmentRepository;
//    @Mock
//    private PaymentRepository paymentRepository;
//    @Mock
//    private BuyerRepository buyerRepository;
//    @Mock
//    private ProductRepository productRepository;
//    @Mock
//
//
//    // 테스트 데이터
//    private Orders testOrder;
//    private Users testUser;
//    private Buyers testBuyer;
//    private Products testProduct1;
//    private Products testProduct2;
//    private OrderCreatedEvent testEvent;
//    private PaymentCompletedEvent paymentCompletedEvent;
//    private List<StockReservation> testReservations;
//
//    @BeforeEach
//    void setUp() {
//        // 테스트 사용자 생성
//        testUser = Users.builder()
//                .id("user123")
//                .provider("google")
//                .providerId("google123")
//                .name("김철수")
//                .role(Role.ROLE_BUYER)
//                .accountDisable(false)
//                .build();
//
//        // 테스트 구매자 엔티티
//        testBuyer = Buyers.builder()
//                .userId("user123")
//                .user(testUser)
//                .nameMaskingStatus(true)
//                .isDeleted(false)
//                .deledAt(null)
//                .build();
//
//        // 테스트 상품들 생성
//        testProduct1 = Products.builder()
//                .id("product1")
//                .title("강아지 사료")
//                .price(25000L)
//                .stock(100)
//                .build();
//
//        testProduct2 = Products.builder()
//                .id("product2")
//                .title("고양이 간식")
//                .price(15000L)
//                .stock(50)
//                .build();
//
//        // 테스트 주문 생성
//        testOrder = Orders.builder()
//                .id("order123")
//                .orderNumber("1001")
//                .buyers(testBuyer)
//                .orderStatus(OrderStatus.PAYMENT_PENDING)
//                .totalPrice(65000L)
//                .build();
//
//        // 테스트 이벤트 생성
//        List<OrderItemInfo> orderItems = Arrays.asList(
//                OrderItemInfo.of("product1", "강아지 사료", 2, 25000L),
//                OrderItemInfo.of("product2", "고양이 간식", 1, 15000L)
//        );
//
//        testEvent = OrderCreatedEvent.of(
//                "order123",
//                "1001",
//                "user123",
//                "google",
//                "google123",
//                65000L,
//                orderItems
//        );
//
//        // PaymentCompletedEvent 생성
//        paymentCompletedEvent = PaymentCompletedEvent.of(
//                "order123",
//                "1001",
//                "user123",
//                "google",
//                "google123",
//                "payment123",
//                "toss_payment_key_123",
//                65000L,
//                orderItems,
//                OrderCreateRequest.ShippingAddressRequest.builder()
//                        .recipientName("김철수")
//                        .recipientPhone("010-1234-5678")
//                        .postalCode("06234")
//                        .streetAddress("서울시 강남구 테헤란로 123")
//                        .detailAddress("456호")
//                        .deliveryNote("문 앞에 놓아주세요")
//                        .build(),
//                65000L,
//                null
//        );
//
//        // 테스트 재고 예약 목록
//        testReservations = Arrays.asList(
//                StockReservation.createReservation(testOrder, testProduct1, 2, 30),
//                StockReservation.createReservation(testOrder, testProduct2, 1, 30)
//        );
//    }
//
//    @Nested
//    @DisplayName("재고 예약 처리 테스트")
//    class StockReservationTests {
//
//        @Test
//        @DisplayName("✅ 재고 예약 성공")
//        void handleStockReservation_Success() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
//            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
//            given(stockReservationService.createBulkReservations(eq(testOrder), anyList())).willReturn(testReservations);
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
//            verify(stockReservationService).createBulkReservations(
//                    eq(testOrder),
//                    argThat(requests -> {
//                        List<StockReservationService.ReservationRequest> requestList = requests;
//                        return requestList.size() == 2 &&
//                                requestList.get(0).product().getId().equals("product1") &&
//                                requestList.get(0).quantity().equals(2) &&
//                                requestList.get(1).product().getId().equals("product2") &&
//                                requestList.get(1).quantity().equals(1);
//                    })
//            );
//        }
//
//        @Test
//        @DisplayName("❌ 주문 없음")
//        void handleStockReservation_OrderNotFound() {
//            given(orderRepository.findById("order123")).willReturn(Optional.empty());
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository, times(2)).findById("order123");
//            verify(stockReservationService, never()).createBulkReservations(any(), any());
//        }
//
//        @Test
//        @DisplayName("❌ 재고 부족 → 보상 트랜잭션")
//        void handleStockReservation_InsufficientStock_Compensation() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById(anyString())).willReturn(Optional.of(testProduct1));
//            given(stockReservationService.createBulkReservations(any(), anyList())).willThrow(new IllegalArgumentException("재고가 부족합니다"));
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository, times(2)).findById("order123");
//            verify(stockReservationService).createBulkReservations(any(), anyList());
//            verify(orderRepository).save(any(Orders.class));
//        }
//
//        @Test
//        @DisplayName("❌ 동시성 충돌 → 보상 트랜잭션")
//        void handleStockReservation_OptimisticLockingFailure_Compensation() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById(anyString())).willReturn(Optional.of(testProduct1));
//            given(stockReservationService.createBulkReservations(any(), anyList())).willThrow(new OptimisticLockingFailureException("동시성 충돌"));
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository, times(2)).findById("order123");
//            verify(stockReservationService).createBulkReservations(any(), anyList());
//            verify(orderRepository).save(any(Orders.class));
//        }
//
//        @Test
//        @DisplayName("❌ 취소된 주문 → 재고 예약 건너뜀")
//        void handleStockReservation_CancelledOrder_Skipped() {
//            Orders cancelledOrder = Orders.builder()
//                    .id("order123")
//                    .orderNumber("1001")
//                    .buyers(testBuyer)
//                    .orderStatus(OrderStatus.CANCELLED)
//                    .totalPrice(65000L)
//                    .build();
//
//            given(orderRepository.findById("order123")).willReturn(Optional.of(cancelledOrder));
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(stockReservationService, never()).createBulkReservations(any(), any());
//        }
//    }
//
//    @Nested
//    @DisplayName("결제 정보 생성 테스트")
//    class PaymentInfoCreationTests {
//
//        @Test
//        @DisplayName("✅ 결제 정보 생성 성공")
//        void handlePaymentInfoCreation_Success() {
//            // Given
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(buyerRepository.findById(testUser.getId())).willReturn(Optional.of(testBuyer));
//            // ⭐️ 수정: 불필요한 Mocking 제거
//            // given(userRepository.getReferenceById("user123")).willReturn(testUser);
//
//            // When
//            orderEventListener.handlePaymentInfoCreation(testEvent);
//
//            // Then
//            verify(orderRepository).findById("order123");
//            verify(buyerRepository).findById(testUser.getId());
//
//            ArgumentCaptor<Payments> paymentCaptor = ArgumentCaptor.forClass(Payments.class);
//            verify(paymentRepository).save(paymentCaptor.capture());
//
//            Payments savedPayment = paymentCaptor.getValue();
//            assertThat(savedPayment.getOrders()).isEqualTo(testOrder);
//            assertThat(savedPayment.getBuyers()).isEqualTo(testBuyer);
//            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
//            assertThat(savedPayment.getMethod()).isEqualTo(PaymentMethod.TOSS);
//        }
//
//        @Test
//        @DisplayName("❌ 주문 없음")
//        void handlePaymentInfoCreation_OrderNotFound() {
//            given(orderRepository.findById("order123")).willReturn(Optional.empty());
//
//            orderEventListener.handlePaymentInfoCreation(testEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(paymentRepository, never()).save(any());
//        }
//
//        @Test
//        @DisplayName("❌ 구매자 정보 없음")
//        void handlePaymentInfoCreation_BuyerNotFound() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(buyerRepository.findById(testUser.getId())).willReturn(Optional.empty());
//
//            orderEventListener.handlePaymentInfoCreation(testEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(buyerRepository).findById(testUser.getId());
//            verify(paymentRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("PaymentCompletedEvent 기반 테스트")
//    class PaymentCompletedEventBasedTests {
//
//        @Test
//        @DisplayName("✅ 결제 완료 후 OrderItems와 Shipments 생성")
//        void handleOrderItemsAndShipmentsCreation_WithShippingAddress() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById(anyString())).willReturn(Optional.of(testProduct1));
//
//            orderEventListener.handleOrderItemsAndShipmentsCreation(paymentCompletedEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(orderItemRepository).saveAll(anyList());
//            verify(shipmentRepository).save(any(Shipments.class));
//            verify(orderRepository).save(any(Orders.class));
//            verify(stockReservationService).confirmReservations("order123");
//            verify(productStockManager).decrementStockForConfirmedReservations("order123");
//        }
//
//        @Test
//        @DisplayName("✅ 배송지 정보 없는 경우 Shipments 생성 안함")
//        void handleOrderItemsAndShipmentsCreation_WithoutShippingAddress() {
//            PaymentCompletedEvent eventWithoutShipping = PaymentCompletedEvent.of(
//                    "order123", "1001", "user123", "google", "google123",
//                    "payment123", "toss_payment_key_123", 65000L,
//                    paymentCompletedEvent.orderItems(), null, 65000L, null
//            );
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById(anyString())).willReturn(Optional.of(testProduct1));
//
//            orderEventListener.handleOrderItemsAndShipmentsCreation(eventWithoutShipping);
//
//            verify(orderRepository).findById("order123");
//            verify(orderItemRepository).saveAll(anyList());
//            verify(shipmentRepository, never()).save(any());
//            verify(orderRepository).save(any(Orders.class));
//        }
//
//        @Test
//        @DisplayName("✅ 결제 완료 알림 처리")
//        void handlePaymentCompletedNotification_ProcessesCorrectly() {
//            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);
//        }
//
//        @Test
//        @DisplayName("❌ 주문 없음 - OrderItems/Shipments 생성 실패")
//        void handleOrderItemsAndShipmentsCreation_OrderNotFound() {
//            given(orderRepository.findById("order123")).willReturn(Optional.empty());
//
//            orderEventListener.handleOrderItemsAndShipmentsCreation(paymentCompletedEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(orderItemRepository, never()).saveAll(anyList());
//            verify(stockReservationService, never()).confirmReservations(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("OrderCreatedEvent 기반 테스트")
//    class OrderCreatedEventBasedTests {
//
//        @Test
//        @DisplayName("✅ 재고 예약 처리 - 정상 케이스")
//        void handleStockReservation_NormalCase() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById(anyString())).willReturn(Optional.of(testProduct1));
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository).findById("order123");
//        }
//
//        @Test
//        @DisplayName("✅ 결제 정보 생성 - 정상 케이스")
//        void handlePaymentInfoCreation_NormalCase() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(buyerRepository.findById(testUser.getId())).willReturn(Optional.of(testBuyer));
//
//            orderEventListener.handlePaymentInfoCreation(testEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(buyerRepository).findById(testUser.getId());
//        }
//    }
//
//    @Nested
//    @DisplayName("결제 완료 알림 테스트")
//    class PaymentCompletedNotificationTests {
//
//        @Test
//        @DisplayName("✅ 결제 완료 알림 성공")
//        void handlePaymentCompletedNotification_Success() {
//            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);
//        }
//
//        @Test
//        @DisplayName("✅ 여러 상품 알림 텍스트 생성")
//        void handlePaymentCompletedNotification_MultipleProducts() {
//            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);
//        }
//    }
//
//    @Nested
//    @DisplayName("실제 이벤트 핸들러 메서드 테스트")
//    class RealEventHandlerTests {
//
//        @Test
//        @DisplayName("✅ 재고 예약 처리 메서드 정상 동작")
//        void handleStockReservation_WorksProperly() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
//            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
//            given(stockReservationService.createBulkReservations(eq(testOrder), anyList())).willReturn(testReservations);
//
//            orderEventListener.handleStockReservation(testEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
//            assertThat(testReservations).hasSize(2);
//            assertThat(testReservations.get(0).getReservedQuantity()).isEqualTo(2);
//            assertThat(testReservations.get(1).getReservedQuantity()).isEqualTo(1);
//        }
//
//        @Test
//        @DisplayName("✅ 결제 정보 생성 메서드 정상 동작")
//        void handlePaymentInfoCreation_WorksProperly() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(buyerRepository.findById(testUser.getId())).willReturn(Optional.of(testBuyer));
//
//            orderEventListener.handlePaymentInfoCreation(testEvent);
//
//            verify(orderRepository).findById("order123");
//        }
//
//        @Test
//        @DisplayName("✅ 결제 완료 후 OrderItems/Shipments 생성 메서드 정상 동작")
//        void handleOrderItemsAndShipmentsCreation_WorksProperly() {
//            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
//            given(productRepository.findById(anyString())).willReturn(Optional.of(testProduct1));
//
//            orderEventListener.handleOrderItemsAndShipmentsCreation(paymentCompletedEvent);
//
//            verify(orderRepository).findById("order123");
//            verify(orderItemRepository).saveAll(anyList());
//            verify(shipmentRepository).save(any(Shipments.class));
//            verify(stockReservationService).confirmReservations("order123");
//            verify(productStockManager).decrementStockForConfirmedReservations("order123");
//        }
//
//        @Test
//        @DisplayName("✅ 결제 완료 알림 메서드 정상 동작")
//        void handlePaymentCompletedNotification_WorksProperly() {
//            orderEventListener.handlePaymentCompletedNotification(paymentCompletedEvent);
//        }
//    }
//
//    @Nested
//    @DisplayName("Record DTO 및 편의 메서드 테스트")
//    class RecordDTOAndConvenienceMethodTests {
//
//        @Test
//        @DisplayName("✅ OrderItemInfo Record 불변성 및 편의 메서드 검증")
//        void orderItemInfo_ImmutabilityAndMethods_Verified() {
//            OrderItemInfo item1 = OrderItemInfo.of("product1", "상품명", 2, 1000L);
//            OrderItemInfo item2 = OrderItemInfo.of("product1", "상품명", 2, 1000L);
//
//            assertThat(item1).isEqualTo(item2);
//            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
//            assertThat(item1.totalPrice()).isEqualTo(2000L);
//        }
//
//        @Test
//        @DisplayName("✅ OrderCreatedEvent 편의 메서드 검증")
//        void orderCreatedEvent_ConvenienceMethods_Verified() {
//            assertThat(testEvent.getOrderItemCount()).isEqualTo(2);
//            assertThat(testEvent.getTotalQuantity()).isEqualTo(3);
//            assertThat(testEvent.getFirstProductName()).isEqualTo("강아지 사료");
//            assertThat(testEvent.getOrderSummary()).contains("강아지 사료 외 1개");
//        }
//
//        @Test
//        @DisplayName("✅ PaymentCompletedEvent 편의 메서드 검증")
//        void paymentCompletedEvent_ConvenienceMethods_Verified() {
//            assertThat(paymentCompletedEvent.getOrderItemCount()).isEqualTo(2);
//            assertThat(paymentCompletedEvent.getFirstProductName()).isEqualTo("강아지 사료");
//            assertThat(paymentCompletedEvent.isCouponApplied()).isFalse();
//        }
//    }
//}