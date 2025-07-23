//package com.team5.catdogeats.orders.service.impl;
//
//import com.team5.catdogeats.auth.dto.UserPrincipal;
//import com.team5.catdogeats.orders.domain.Orders;
//import com.team5.catdogeats.orders.domain.Shipments;
//import com.team5.catdogeats.orders.domain.enums.OrderStatus;
//import com.team5.catdogeats.orders.domain.mapping.OrderItems;
//import com.team5.catdogeats.orders.dto.response.BuyerOrderListResponse;
//import com.team5.catdogeats.orders.dto.response.BuyerShipmentDetailResponse;
//import com.team5.catdogeats.orders.dto.response.TrackingResponse;
//import com.team5.catdogeats.orders.repository.OrderRepository;
//import com.team5.catdogeats.orders.repository.ShipmentRepository;
//import com.team5.catdogeats.orders.service.LogisticsTrackingService;
//import com.team5.catdogeats.orders.service.ShipmentSyncService;
//import com.team5.catdogeats.products.domain.Products;
//import com.team5.catdogeats.users.domain.Users;
//import com.team5.catdogeats.users.domain.enums.Role;
//import com.team5.catdogeats.users.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.time.ZonedDateTime;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("구매자 주문/배송 조회 서비스 테스트")
//class BuyerOrderQueryServiceImplTest {
//
//    @Mock
//    private OrderRepository orderRepository;
//
//    @Mock
//    private ShipmentRepository shipmentRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private LogisticsTrackingService logisticsTrackingService;
//
//    @Mock
//    private ShipmentSyncService shipmentSyncService;
//
//    @InjectMocks
//    private BuyerOrderQueryServiceImpl buyerOrderQueryService;
//
//    private UserPrincipal testUserPrincipal;
//    private Users testUser;
//    private Orders testOrder;
//    private Shipments testShipment;
//    private OrderItems testOrderItem;
//    private Pageable testPageable;
//    private TrackingResponse mockTrackingResponse;
//
//    private final String testOrderNumber = "ORD-20241225-001";
//
//    @BeforeEach
//    void setUp() {
//        // 테스트용 UserPrincipal
//        testUserPrincipal = new UserPrincipal("google", "google123");
//
//        // 테스트용 사용자
//        testUser = Users.builder()
//                .id("user123")
//                .provider("google")
//                .providerId("google123")
//                .name("김구매자")
//                .userNameAttribute("sub")
//                .role(Role.ROLE_BUYER)
//                .accountDisable(false)
//                .build();
//
//        // 테스트용 상품
//        Products testProduct = Products.builder()
//                .id("product123")
//                .title("테스트 상품")
//                .price(25000L)
//                .build();
//
//        // 테스트용 주문 상품
//        testOrderItem = OrderItems.builder()
//                .id("orderItem123")
//                .quantity(2)
//                .price(25000L)
//                .products(testProduct)
//                .build();
//
//        // 테스트용 주문
//        testOrder = Orders.builder()
//                .id("order123")
//                .orderNumber(testOrderNumber)
//                .orderStatus(OrderStatus.IN_DELIVERY)
//                .totalPrice(50000L)
//                .user(testUser)
//                .orderItems(Collections.singletonList(testOrderItem))
//                .build();
//
//        // 테스트용 배송 정보
//        testShipment = Shipments.builder()
//                .id("shipment123")
//                .orders(testOrder)
//                .user(testUser)
//                .recipientName("김수령자")
//                .recipientPhone("010-1234-5678")
//                .postalCode("12345")
//                .streetAddress("서울시 강남구 테헤란로 123")
//                .detailAddress("456호")
//                .deliveryRequest("문 앞에 놓아주세요")
//                .trackingNumber("123456789012")
//                .courier("CJ대한통운")
//                .build();
//
//        // 테스트용 페이징 정보
//        testPageable = PageRequest.of(0, 20);
//
//        // 테스트용 물류 서버 응답
//        mockTrackingResponse = new TrackingResponse(
//                "123456789012",
//                "CJ",
//                "IN_DELIVERY",
//                ZonedDateTime.now().minusDays(1),
//                null,
//                Arrays.asList(
//                        new TrackingResponse.TrackingLogResponse(
//                                "1",
//                                "PICKED_UP",
//                                "상품이 접수되었습니다",
//                                ZonedDateTime.now().minusDays(1)
//                        ),
//                        new TrackingResponse.TrackingLogResponse(
//                                "2",
//                                "IN_DELIVERY",
//                                "배송 중입니다",
//                                ZonedDateTime.now()
//                        )
//                )
//        );
//    }
//
//    @Nested
//    @DisplayName("구매자 주문 목록 조회 테스트")
//    class GetBuyerOrderListTests {
//
//        @Test
//        @DisplayName("✅ 정상적인 주문 목록 조회 성공")
//        void getBuyerOrderList_Success() {
//            // given
//            List<Orders> orderList = Collections.singletonList(testOrder);
//            Page<Orders> orderPage = new PageImpl<>(orderList, testPageable, 1);
//
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findBuyerOrdersWithDetails(testUser, testPageable))
//                    .willReturn(orderPage);
//
//            // when
//            BuyerOrderListResponse response = buyerOrderQueryService.getBuyerOrderList(testUserPrincipal, testPageable);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orders()).hasSize(1);
//            assertThat(response.currentPage()).isEqualTo(0);
//            assertThat(response.totalPages()).isEqualTo(1);
//            assertThat(response.totalElements()).isEqualTo(1L);
//            assertThat(response.pageSize()).isEqualTo(20);
//            assertThat(response.hasNext()).isFalse();
//            assertThat(response.hasPrevious()).isFalse();
//
//            // 주문 요약 정보 검증
//            BuyerOrderListResponse.BuyerOrderSummary orderSummary = response.orders().get(0);
//            assertThat(orderSummary.orderNumber()).isEqualTo(testOrderNumber);
//            assertThat(orderSummary.deliveryStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
//            assertThat(orderSummary.totalPrice()).isEqualTo(50000L);
//            assertThat(orderSummary.orderItemsInfo()).contains("테스트 상품");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository).findBuyerOrdersWithDetails(testUser, testPageable);
//        }
//
//        @Test
//        @DisplayName("✅ 빈 주문 목록 조회 성공")
//        void getBuyerOrderList_EmptyList_Success() {
//            // given
//            Page<Orders> emptyOrderPage = new PageImpl<>(List.of(), testPageable, 0);
//
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findBuyerOrdersWithDetails(testUser, testPageable))
//                    .willReturn(emptyOrderPage);
//
//            // when
//            BuyerOrderListResponse response = buyerOrderQueryService.getBuyerOrderList(testUserPrincipal, testPageable);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orders()).isEmpty();
//            assertThat(response.totalElements()).isEqualTo(0L);
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository).findBuyerOrdersWithDetails(testUser, testPageable);
//        }
//
//        @Test
//        @DisplayName("❌ 존재하지 않는 사용자로 주문 목록 조회 시도")
//        void getBuyerOrderList_UserNotFound_ThrowsException() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() ->
//                    buyerOrderQueryService.getBuyerOrderList(testUserPrincipal, testPageable)
//            ).isInstanceOf(RuntimeException.class) // 실제 구현에서는 RuntimeException으로 감싸서 던짐
//                    .hasMessageContaining("주문 목록 조회 중 오류가 발생했습니다");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository, never()).findBuyerOrdersWithDetails(any(), any());
//        }
//
//        @Test
//        @DisplayName("❌ 잘못된 페이징 정보로 조회 시도")
//        void getBuyerOrderList_InvalidPageable_ThrowsException() {
//            // when & then - PageRequest 생성 시점에서 IllegalArgumentException 발생
//            assertThatThrownBy(() -> {
//                Pageable invalidPageable = PageRequest.of(-1, 0); // 잘못된 페이징 정보
//                buyerOrderQueryService.getBuyerOrderList(testUserPrincipal, invalidPageable);
//            }).isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("Page index must not be less than zero");
//
//            // 서비스 메서드가 호출되기 전에 예외가 발생하므로 repository 호출 검증 안 함
//        }
//
//        @Test
//        @DisplayName("❌ 페이지 크기가 100을 초과하는 경우")
//        void getBuyerOrderList_PageSizeExceeded_ThrowsException() {
//            // given
//            Pageable oversizedPageable = PageRequest.of(0, 101); // 크기 초과
//
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//
//            // when & then
//            assertThatThrownBy(() ->
//                    buyerOrderQueryService.getBuyerOrderList(testUserPrincipal, oversizedPageable)
//            ).isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("페이지 크기는 100을 초과할 수 없습니다");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//        }
//
//        @Test
//        @DisplayName("✅ 여러 주문이 있는 목록 조회 성공")
//        void getBuyerOrderList_MultipleOrders_Success() {
//            // given
//            Orders secondOrder = Orders.builder()
//                    .id("order456")
//                    .orderNumber("ORD-20241225-002")
//                    .orderStatus(OrderStatus.DELIVERED)
//                    .totalPrice(75000L)
//                    .user(testUser)
//                    .orderItems(Collections.singletonList(testOrderItem))
//                    .build();
//
//            List<Orders> orderList = Arrays.asList(testOrder, secondOrder);
//            Page<Orders> orderPage = new PageImpl<>(orderList, testPageable, 2);
//
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findBuyerOrdersWithDetails(testUser, testPageable))
//                    .willReturn(orderPage);
//
//            // when
//            BuyerOrderListResponse response = buyerOrderQueryService.getBuyerOrderList(testUserPrincipal, testPageable);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orders()).hasSize(2);
//            assertThat(response.totalElements()).isEqualTo(2L);
//
//            // 첫 번째 주문 검증
//            BuyerOrderListResponse.BuyerOrderSummary firstOrder = response.orders().get(0);
//            assertThat(firstOrder.orderNumber()).isEqualTo(testOrderNumber);
//            assertThat(firstOrder.deliveryStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
//
//            // 두 번째 주문 검증
//            BuyerOrderListResponse.BuyerOrderSummary secondOrderSummary = response.orders().get(1);
//            assertThat(secondOrderSummary.orderNumber()).isEqualTo("ORD-20241225-002");
//            assertThat(secondOrderSummary.deliveryStatus()).isEqualTo(OrderStatus.DELIVERED);
//        }
//    }
//
//    @Nested
//    @DisplayName("구매자 배송 정보 상세 조회 테스트")
//    class GetBuyerShipmentDetailTests {
//
//        @Test
//        @DisplayName("✅ 정상적인 배송 정보 상세 조회 성공")
//        void getBuyerShipmentDetail_Success() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.of(testOrder));
//            given(shipmentRepository.findByOrders(testOrder))
//                    .willReturn(Optional.of(testShipment));
//            given(shipmentSyncService.syncSingleOrderDeliveryStatus(testOrderNumber))
//                    .willReturn(false); // 동기화 불필요
//            given(logisticsTrackingService.getTrackingInfo("123456789012"))
//                    .willReturn(Optional.of(mockTrackingResponse));
//
//            // when
//            BuyerShipmentDetailResponse response = buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
//            assertThat(response.deliveryStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
//            assertThat(response.arrivalDate()).isNull(); // 배송 중이므로 도착일 없음
//
//            // 운송장 정보 검증
//            assertThat(response.trackingInfo().trackingNumber()).isEqualTo("123456789012");
//            assertThat(response.trackingInfo().carrierName()).isEqualTo("CJ대한통운");
//
//            // 수취인 정보 검증
//            assertThat(response.recipientInfo().recipientName()).isEqualTo("김수령자");
//            assertThat(response.recipientInfo().recipientPhone()).isEqualTo("010-1234-5678");
//            assertThat(response.recipientInfo().fullAddress()).contains("서울시 강남구 테헤란로 123");
//            assertThat(response.recipientInfo().deliveryRequest()).isEqualTo("문 앞에 놓아주세요");
//
//            // 추적 로그 검증
//            assertThat(response.trackingLogs()).hasSize(2);
//            assertThat(response.trackingLogs().get(0).status()).isEqualTo("PICKED_UP");
//            assertThat(response.trackingLogs().get(1).status()).isEqualTo("IN_DELIVERY");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository).findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber);
//            verify(shipmentRepository).findByOrders(testOrder);
//            verify(shipmentSyncService).syncSingleOrderDeliveryStatus(testOrderNumber);
//            verify(logisticsTrackingService).getTrackingInfo("123456789012");
//        }
//
//        @Test
//        @DisplayName("✅ 배송 완료된 주문의 상세 조회 성공")
//        void getBuyerShipmentDetail_DeliveredOrder_Success() {
//            // given
//            testOrder.setOrderStatus(OrderStatus.DELIVERED);
//            ZonedDateTime deliveredAt = ZonedDateTime.now().minusDays(1);
//            testShipment.setDeliveredAt(deliveredAt);
//
//            TrackingResponse deliveredTrackingResponse = new TrackingResponse(
//                    "123456789012",
//                    "CJ",
//                    "DELIVERED",
//                    ZonedDateTime.now().minusDays(2),
//                    deliveredAt,
//                    List.of(
//                            new TrackingResponse.TrackingLogResponse(
//                                    "3",
//                                    "DELIVERED",
//                                    "배송이 완료되었습니다",
//                                    deliveredAt
//                            )
//                    )
//            );
//
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.of(testOrder));
//            given(shipmentRepository.findByOrders(testOrder))
//                    .willReturn(Optional.of(testShipment));
//            given(shipmentSyncService.syncSingleOrderDeliveryStatus(testOrderNumber))
//                    .willReturn(false);
//            given(logisticsTrackingService.getTrackingInfo("123456789012"))
//                    .willReturn(Optional.of(deliveredTrackingResponse));
//
//            // when
//            BuyerShipmentDetailResponse response = buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
//            assertThat(response.deliveryStatus()).isEqualTo(OrderStatus.DELIVERED); // 실제 구현에서는 DELIVERED 상태를 반환
//            assertThat(response.arrivalDate()).isEqualTo(deliveredAt); // 도착일 표시
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository).findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber);
//        }
//
//        @Test
//        @DisplayName("❌ 존재하지 않는 사용자로 배송 상세 조회 시도")
//        void getBuyerShipmentDetail_UserNotFound_ThrowsException() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() ->
//                    buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber)
//            ).isInstanceOf(NoSuchElementException.class)
//                    .hasMessageContaining("사용자를 찾을 수 없습니다");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository, never()).findOrderDetailByUserAndOrderNumber(any(), any());
//        }
//
//        @Test
//        @DisplayName("❌ 존재하지 않는 주문으로 배송 상세 조회 시도")
//        void getBuyerShipmentDetail_OrderNotFound_ThrowsException() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() ->
//                    buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber)
//            ).isInstanceOf(NoSuchElementException.class)
//                    .hasMessageContaining("주문을 찾을 수 없거나 접근 권한이 없습니다");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository).findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber);
//            verify(shipmentRepository, never()).findByOrders(any());
//        }
//
//        @Test
//        @DisplayName("❌ 배송 정보가 없는 주문으로 상세 조회 시도")
//        void getBuyerShipmentDetail_ShipmentNotFound_ThrowsException() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.of(testOrder));
//            given(shipmentRepository.findByOrders(testOrder))
//                    .willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() ->
//                    buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber)
//            ).isInstanceOf(IllegalStateException.class)
//                    .hasMessageContaining("배송 정보가 없는 주문입니다");
//
//            verify(userRepository).findByProviderAndProviderId("google", "google123");
//            verify(orderRepository).findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber);
//            verify(shipmentRepository).findByOrders(testOrder);
//            verify(shipmentSyncService, never()).syncSingleOrderDeliveryStatus(any());
//        }
//
//        @Test
//        @DisplayName("✅ 배송 상태 동기화가 수행되는 경우")
//        void getBuyerShipmentDetail_WithSync_Success() {
//            // given
//            Orders updatedOrder = Orders.builder()
//                    .id("order123")
//                    .orderNumber(testOrderNumber)
//                    .orderStatus(OrderStatus.DELIVERED) // 동기화 후 상태 변경
//                    .totalPrice(50000L)
//                    .user(testUser)
//                    .orderItems(Collections.singletonList(testOrderItem))
//                    .build();
//
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.of(testOrder))
//                    .willReturn(Optional.of(updatedOrder)); // 두 번째 호출 시 업데이트된 주문 반환
//            given(shipmentRepository.findByOrders(any(Orders.class)))
//                    .willReturn(Optional.of(testShipment));
//            given(shipmentSyncService.syncSingleOrderDeliveryStatus(testOrderNumber))
//                    .willReturn(true); // 동기화 수행됨
//            given(logisticsTrackingService.getTrackingInfo("123456789012"))
//                    .willReturn(Optional.of(mockTrackingResponse));
//
//            // when
//            BuyerShipmentDetailResponse response = buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
//
//            verify(shipmentSyncService).syncSingleOrderDeliveryStatus(testOrderNumber);
//            verify(orderRepository, times(2)).findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber);
//        }
//
//        @Test
//        @DisplayName("✅ 동기화 실패 시에도 기존 데이터로 진행")
//        void getBuyerShipmentDetail_SyncFailure_ContinuesWithExistingData() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.of(testOrder));
//            given(shipmentRepository.findByOrders(testOrder))
//                    .willReturn(Optional.of(testShipment));
//            given(shipmentSyncService.syncSingleOrderDeliveryStatus(testOrderNumber))
//                    .willThrow(new RuntimeException("동기화 서버 오류"));
//            given(logisticsTrackingService.getTrackingInfo("123456789012"))
//                    .willReturn(Optional.of(mockTrackingResponse));
//
//            // when & then
//            assertThatCode(() ->
//                    buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber)
//            ).doesNotThrowAnyException();
//
//            verify(shipmentSyncService).syncSingleOrderDeliveryStatus(testOrderNumber);
//            verify(logisticsTrackingService).getTrackingInfo("123456789012");
//        }
//
//        @Test
//        @DisplayName("✅ 물류 서버 조회 실패 시 빈 추적 로그로 진행")
//        void getBuyerShipmentDetail_LogisticsFailure_ContinuesWithEmptyLogs() {
//            // given
//            given(userRepository.findByProviderAndProviderId("google", "google123"))
//                    .willReturn(Optional.of(testUser));
//            given(orderRepository.findOrderDetailByUserAndOrderNumber(testUser, testOrderNumber))
//                    .willReturn(Optional.of(testOrder));
//            given(shipmentRepository.findByOrders(testOrder))
//                    .willReturn(Optional.of(testShipment));
//            given(shipmentSyncService.syncSingleOrderDeliveryStatus(testOrderNumber))
//                    .willReturn(false);
//            given(logisticsTrackingService.getTrackingInfo("123456789012"))
//                    .willReturn(Optional.empty()); // 물류 서버 조회 실패
//
//            // when
//            BuyerShipmentDetailResponse response = buyerOrderQueryService.getBuyerShipmentDetail(testUserPrincipal, testOrderNumber);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
//            assertThat(response.trackingLogs()).isEmpty(); // 빈 추적 로그
//
//            verify(logisticsTrackingService).getTrackingInfo("123456789012");
//        }
//    }
//}