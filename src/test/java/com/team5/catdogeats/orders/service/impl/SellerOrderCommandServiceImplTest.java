package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.ShipmentSyncService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerOrderCommandService 쓰기 작업 테스트")
class SellerOrderCommandServiceImplTest {

    @InjectMocks
    private SellerOrderCommandServiceImpl sellerOrderCommandService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellersRepository sellerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentSyncService shipmentSyncService;

    // 테스트 데이터
    private UserPrincipal principal;
    private Users testUser;
    private Sellers testSeller;
    private Orders testOrder;
    private Shipments testShipment;
    private Products testProduct;
    private OrderItems testOrderItem;
    private String testOrderNumber;

    @BeforeEach
    void setUp() {
        // UserPrincipal 초기화
        principal = new UserPrincipal("google", "google123");
        testOrderNumber = "ORDER-2025-001";

        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .name("김철수")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build();

        // 테스트 판매자 생성
        testSeller = Sellers.builder()
                .userId("seller123")
                .user(testUser)
                .vendorName("멍냥이네 수제간식")
                .isDeleted(false)
                .build();

        // 테스트 상품 생성
        testProduct = Products.builder()
                .id("product1")
                .title("프리미엄 강아지 사료")
                .price(25000L)
                .seller(testSeller)
                .build();

        // 테스트 주문 상품 생성
        testOrderItem = OrderItems.builder()
                .id("orderItem1")
                .products(testProduct)
                .quantity(2)
                .price(25000L)
                .build();

        // 테스트 주문 생성
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(testOrderNumber)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(50000L)
                .orderItems(Arrays.asList(testOrderItem))
                .isHidden(false)
                .build();

        // 테스트 배송 정보 생성
        testShipment = Shipments.builder()
                .id("shipment123")
                .orders(testOrder)
                .recipientName("김철수")
                .recipientPhone("010-1234-5678")
                .postalCode("06234")
                .shippingAddress("서울시 강남구 테헤란로 123")
                .detailAddress("456호")
                .deliveryNote("문 앞에 놓아주세요")
                .build();
    }

    @Nested
    @DisplayName("주문 상태 변경 테스트")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("✅ 결제완료 → 상품준비중 상태 변경 성공")
        void updateOrderStatus_PaymentCompletedToPreparing_Success() {
            // given
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.PREPARING,
                    "상품 준비를 시작합니다",
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(testOrder);
            given(shipmentRepository.save(any(Shipments.class)))
                    .willReturn(testShipment);

            // when
            OrderStatusUpdateResponse response = sellerOrderCommandService.updateOrderStatus(principal, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.previousStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(response.currentStatus()).isEqualTo(OrderStatus.PREPARING);
            assertThat(response.reason()).isEqualTo("상품 준비를 시작합니다");
            assertThat(response.isDelayed()).isFalse();
            assertThat(response.message()).contains("상품 준비 중 상태로 변경되었습니다");

            // 저장 검증
            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            verify(orderRepository).save(orderCaptor.capture());
            Orders savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PREPARING);

            verify(shipmentRepository).save(any(Shipments.class));
        }

        @Test
        @DisplayName("✅ 상품준비중 → 배송준비완료 상태 변경 성공")
        void updateOrderStatus_PreparingToReadyForShipment_Success() {
            // given
            testOrder.setOrderStatus(OrderStatus.PREPARING);
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.READY_FOR_SHIPMENT,
                    "배송 준비가 완료되었습니다",
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(testOrder);
            given(shipmentRepository.save(any(Shipments.class)))
                    .willReturn(testShipment);

            // when
            OrderStatusUpdateResponse response = sellerOrderCommandService.updateOrderStatus(principal, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.previousStatus()).isEqualTo(OrderStatus.PREPARING);
            assertThat(response.currentStatus()).isEqualTo(OrderStatus.READY_FOR_SHIPMENT);
            assertThat(response.message()).contains("배송 준비 완료 상태로 변경되었습니다");

            verify(orderRepository).save(any(Orders.class));
            verify(shipmentRepository).save(any(Shipments.class));
        }

        @Test
        @DisplayName("✅ 지연 출고 상태 변경 성공")
        void updateOrderStatus_WithDelay_Success() {
            // given
            testOrder.setOrderStatus(OrderStatus.PREPARING);
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.READY_FOR_SHIPMENT,
                    "재고 부족으로 인한 지연",
                    true,
                    "2025-07-15"
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(testOrder);
            given(shipmentRepository.save(any(Shipments.class)))
                    .willReturn(testShipment);

            // when
            OrderStatusUpdateResponse response = sellerOrderCommandService.updateOrderStatus(principal, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isDelayed()).isTrue();
            assertThat(response.delayReason()).isEqualTo("재고 부족으로 인한 지연");
            assertThat(response.expectedDeliveryDate()).isNotNull();

            verify(orderRepository).save(any(Orders.class));
            verify(shipmentRepository).save(any(Shipments.class));
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문으로 상태 변경 시 예외 발생")
        void updateOrderStatus_OrderNotFound_ThrowsException() {
            // given
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    "INVALID-ORDER",
                    OrderStatus.PREPARING,
                    null,
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber("INVALID-ORDER"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.updateOrderStatus(principal, request))
                    .isInstanceOf(NoSuchElementException.class);

            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository, never()).save(any(Shipments.class));
        }

        @Test
        @DisplayName("❌ 잘못된 상태 전환 시 예외 발생")
        void updateOrderStatus_InvalidStatusTransition_ThrowsException() {
            // given - PAYMENT_COMPLETED에서 IN_DELIVERY로 바로 변경 시도 (단계 건너뛰기)
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.IN_DELIVERY,
                    null,
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.updateOrderStatus(principal, request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(orderRepository, never()).save(any(Orders.class));
        }
    }

    @Nested
    @DisplayName("운송장 번호 등록 테스트")
    class RegisterTrackingNumberTests {

        @Test
        @DisplayName("✅ 운송장 번호 등록 성공 (즉시 배송 시작)")
        void registerTrackingNumber_WithImmediateShipment_Success() {
            // given
            testOrder.setOrderStatus(OrderStatus.READY_FOR_SHIPMENT);
            TrackingNumberRegisterRequest request = new TrackingNumberRegisterRequest(
                    testOrderNumber,
                    CourierCompany.CJ_LOGISTICS,
                    "123456789012",
                    "조심히 배송해주세요",
                    true
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(shipmentRepository.findByCourierAndTrackingNumber(any(String.class), any(String.class)))
                    .willReturn(Optional.empty());
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(testOrder);
            given(shipmentRepository.save(any(Shipments.class)))
                    .willReturn(testShipment);

            // when
            TrackingNumberRegisterResponse response = sellerOrderCommandService.registerTrackingNumber(principal, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.trackingNumber()).isEqualTo("123456789012");
            assertThat(response.courierCompany()).isEqualTo(CourierCompany.CJ_LOGISTICS);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
            assertThat(response.shippedAt()).isNotNull();
            assertThat(response.message()).contains("운송장 번호가 등록되고 배송이 시작되었습니다");

            // 배송 정보 검증
            TrackingNumberRegisterResponse.ShipmentInfo shipmentInfo = response.shipmentInfo();
            assertThat(shipmentInfo.courier()).isEqualTo(CourierCompany.CJ_LOGISTICS.getDisplayName());
            assertThat(shipmentInfo.trackingNumber()).isEqualTo("123456789012");
            assertThat(shipmentInfo.shipmentMemo()).isEqualTo("조심히 배송해주세요");

            // 저장 검증
            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            verify(orderRepository).save(orderCaptor.capture());
            Orders savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.IN_DELIVERY);

            ArgumentCaptor<Shipments> shipmentCaptor = ArgumentCaptor.forClass(Shipments.class);
            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipments savedShipment = shipmentCaptor.getValue();
            assertThat(savedShipment.getCourier()).isEqualTo(CourierCompany.CJ_LOGISTICS.getDisplayName());
            assertThat(savedShipment.getTrackingNumber()).isEqualTo("123456789012");
            assertThat(savedShipment.getShipmentMemo()).isEqualTo("조심히 배송해주세요");
            assertThat(savedShipment.getShippedAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ 운송장 번호만 등록 (배송 시작하지 않음)")
        void registerTrackingNumber_WithoutImmediateShipment_Success() {
            // given
            testOrder.setOrderStatus(OrderStatus.READY_FOR_SHIPMENT);
            TrackingNumberRegisterRequest request = new TrackingNumberRegisterRequest(
                    testOrderNumber,
                    CourierCompany.HANJIN_EXPRESS,
                    "987654321098",
                    null,
                    false
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(shipmentRepository.findByCourierAndTrackingNumber(any(String.class), any(String.class)))
                    .willReturn(Optional.empty());
            given(shipmentRepository.save(any(Shipments.class)))
                    .willReturn(testShipment);

            // when
            TrackingNumberRegisterResponse response = sellerOrderCommandService.registerTrackingNumber(principal, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.READY_FOR_SHIPMENT); // 상태 변경 없음
            assertThat(response.message()).contains("운송장 번호가 등록되었습니다");

            // 주문 상태는 변경되지 않아야 함
            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository).save(any(Shipments.class));
        }

        @Test
        @DisplayName("❌ 잘못된 주문 상태에서 운송장 등록 시 예외 발생")
        void registerTrackingNumber_InvalidOrderStatus_ThrowsException() {
            // given - PAYMENT_COMPLETED 상태에서 운송장 등록 시도
            testOrder.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
            TrackingNumberRegisterRequest request = new TrackingNumberRegisterRequest(
                    testOrderNumber,
                    CourierCompany.CJ_LOGISTICS,
                    "123456789012",
                    null,
                    true
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.registerTrackingNumber(principal, request))
                    .isInstanceOf(IllegalStateException.class);

            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository, never()).save(any(Shipments.class));
        }

        @Test
        @DisplayName("❌ 중복된 운송장 번호 등록 시 예외 발생")
        void registerTrackingNumber_DuplicateTrackingNumber_ThrowsException() {
            // given
            testOrder.setOrderStatus(OrderStatus.READY_FOR_SHIPMENT);
            TrackingNumberRegisterRequest request = new TrackingNumberRegisterRequest(
                    testOrderNumber,
                    CourierCompany.CJ_LOGISTICS,
                    "123456789012",
                    null,
                    true
            );

            // 이미 존재하는 운송장 번호
            Shipments existingShipment = Shipments.builder()
                    .id("existing123")
                    .courier(CourierCompany.CJ_LOGISTICS.getDisplayName())
                    .trackingNumber("123456789012")
                    .build();

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(shipmentRepository.findByCourierAndTrackingNumber(
                    CourierCompany.CJ_LOGISTICS.getDisplayName(), "123456789012"))
                    .willReturn(Optional.of(existingShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.registerTrackingNumber(principal, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 등록된 운송장 번호입니다");

            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository, never()).save(any(Shipments.class));
        }
    }

    @Nested
    @DisplayName("배송 상태 동기화 테스트")
    class SyncAllShipmentStatusTests {

        @Test
        @DisplayName("✅ 배송 상태 동기화 성공")
        void syncAllShipmentStatus_Success() {
            // given
            List<ShipmentSyncResponse.UpdatedOrderInfo> updatedOrders = Arrays.asList(
                    ShipmentSyncResponse.UpdatedOrderInfo.of(
                            "ORDER-2025-001",
                            "123456789012",
                            "CJ대한통운",
                            ZonedDateTime.now()
                    ),
                    ShipmentSyncResponse.UpdatedOrderInfo.of(
                            "ORDER-2025-002",
                            "987654321098",
                            "한진택배",
                            ZonedDateTime.now()
                    )
            );

            ShipmentSyncResponse expectedResponse = ShipmentSyncResponse.builder()
                    .totalCheckedOrders(10)
                    .updatedOrders(2)
                    .failedOrders(0)
                    .updatedOrderList(updatedOrders)
                    .failedOrderList(List.of())
                    .syncedAt(ZonedDateTime.now())
                    .message("2개 주문이 배송 완료로 업데이트되었습니다.")
                    .build();

            given(shipmentSyncService.syncAllShipmentStatus(principal))
                    .willReturn(expectedResponse);

            // when
            ShipmentSyncResponse response = sellerOrderCommandService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(10);
            assertThat(response.updatedOrders()).isEqualTo(2);
            assertThat(response.failedOrders()).isEqualTo(0);
            assertThat(response.updatedOrderList()).hasSize(2);
            assertThat(response.failedOrderList()).isEmpty();
            assertThat(response.message()).contains("2개 주문이 배송 완료로 업데이트되었습니다");

            // 업데이트된 주문 정보 검증
            ShipmentSyncResponse.UpdatedOrderInfo firstUpdated = response.updatedOrderList().get(0);
            assertThat(firstUpdated.orderNumber()).isEqualTo("ORDER-2025-001");
            assertThat(firstUpdated.trackingNumber()).isEqualTo("123456789012");
            assertThat(firstUpdated.courier()).isEqualTo("CJ대한통운");
            assertThat(firstUpdated.deliveredAt()).isNotNull();

            verify(shipmentSyncService).syncAllShipmentStatus(principal);
        }

        @Test
        @DisplayName("✅ 업데이트할 주문이 없는 경우")
        void syncAllShipmentStatus_NoUpdates_Success() {
            // given
            ShipmentSyncResponse expectedResponse = ShipmentSyncResponse.builder()
                    .totalCheckedOrders(5)
                    .updatedOrders(0)
                    .failedOrders(0)
                    .updatedOrderList(List.of())
                    .failedOrderList(List.of())
                    .syncedAt(ZonedDateTime.now())
                    .message("모든 주문이 이미 최신 상태입니다.")
                    .build();

            given(shipmentSyncService.syncAllShipmentStatus(principal))
                    .willReturn(expectedResponse);

            // when
            ShipmentSyncResponse response = sellerOrderCommandService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(5);
            assertThat(response.updatedOrders()).isEqualTo(0);
            assertThat(response.updatedOrderList()).isEmpty();
            assertThat(response.message()).contains("모든 주문이 이미 최신 상태입니다");

            verify(shipmentSyncService).syncAllShipmentStatus(principal);
        }

        @Test
        @DisplayName("✅ 일부 주문 동기화 실패한 경우")
        void syncAllShipmentStatus_WithFailures_Success() {
            // given
            List<ShipmentSyncResponse.UpdatedOrderInfo> updatedOrders = Arrays.asList(
                    ShipmentSyncResponse.UpdatedOrderInfo.of(
                            "ORDER-2025-001",
                            "123456789012",
                            "CJ대한통운",
                            ZonedDateTime.now()
                    )
            );

            List<ShipmentSyncResponse.FailedOrderInfo> failedOrders = Arrays.asList(
                    ShipmentSyncResponse.FailedOrderInfo.of(
                            "ORDER-2025-002",
                            "987654321098",
                            "한진택배",
                            "물류 서버 응답 없음"
                    )
            );

            ShipmentSyncResponse expectedResponse = ShipmentSyncResponse.builder()
                    .totalCheckedOrders(8)
                    .updatedOrders(1)
                    .failedOrders(1)
                    .updatedOrderList(updatedOrders)
                    .failedOrderList(failedOrders)
                    .syncedAt(ZonedDateTime.now())
                    .message("1개 주문이 업데이트되었습니다. 1개 주문에서 오류가 발생했습니다.")
                    .build();

            given(shipmentSyncService.syncAllShipmentStatus(principal))
                    .willReturn(expectedResponse);

            // when
            ShipmentSyncResponse response = sellerOrderCommandService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(8);
            assertThat(response.updatedOrders()).isEqualTo(1);
            assertThat(response.failedOrders()).isEqualTo(1);
            assertThat(response.updatedOrderList()).hasSize(1);
            assertThat(response.failedOrderList()).hasSize(1);

            // 실패한 주문 정보 검증
            ShipmentSyncResponse.FailedOrderInfo failedOrder = response.failedOrderList().get(0);
            assertThat(failedOrder.orderNumber()).isEqualTo("ORDER-2025-002");
            assertThat(failedOrder.errorReason()).isEqualTo("물류 서버 응답 없음");

            verify(shipmentSyncService).syncAllShipmentStatus(principal);
        }

        @Test
        @DisplayName("❌ 배송 상태 동기화 서비스 예외 발생")
        void syncAllShipmentStatus_ServiceException_ThrowsException() {
            // given
            given(shipmentSyncService.syncAllShipmentStatus(principal))
                    .willThrow(new RuntimeException("물류 서버 연결 실패"));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.syncAllShipmentStatus(principal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("물류 서버 연결 실패");

            verify(shipmentSyncService).syncAllShipmentStatus(principal);
        }
    }

    @Nested
    @DisplayName("주문 삭제(숨김 처리) 테스트")
    class DeleteOrderTests {

        @Test
        @DisplayName("✅ 배송 완료된 주문 삭제 성공")
        void deleteOrder_DeliveredOrder_Success() {
            // given
            testOrder.setOrderStatus(OrderStatus.DELIVERED);
            testOrder.setIsHidden(false);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(testOrder);

            // when
            boolean result = sellerOrderCommandService.deleteOrder(principal, testOrderNumber);

            // then
            assertThat(result).isTrue();

            // 숨김 처리 검증
            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            verify(orderRepository).save(orderCaptor.capture());
            Orders savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getIsHidden()).isTrue();
            assertThat(savedOrder.getHiddenAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ 취소된 주문 삭제 성공")
        void deleteOrder_CancelledOrder_Success() {
            // given
            testOrder.setOrderStatus(OrderStatus.CANCELLED);
            testOrder.setIsHidden(false);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(testOrder);

            // when
            boolean result = sellerOrderCommandService.deleteOrder(principal, testOrderNumber);

            // then
            assertThat(result).isTrue();
            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 배송 중인 주문 삭제 시 예외 발생")
        void deleteOrder_InDeliveryOrder_ThrowsException() {
            // given
            testOrder.setOrderStatus(OrderStatus.IN_DELIVERY);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.deleteOrder(principal, testOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(orderRepository, never()).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 이미 숨김 처리된 주문 삭제 시 예외 발생")
        void deleteOrder_AlreadyHiddenOrder_ThrowsException() {
            // given
            testOrder.setOrderStatus(OrderStatus.DELIVERED);
            testOrder.setIsHidden(true);
            testOrder.setHiddenAt(ZonedDateTime.now());

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.deleteOrder(principal, testOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 숨김 처리된 주문입니다");

            verify(orderRepository, never()).save(any(Orders.class));
        }
    }

    @Nested
    @DisplayName("공통 인증 및 권한 검증 테스트")
    class AuthenticationAndAuthorizationTests {

        @Test
        @DisplayName("❌ 존재하지 않는 사용자로 요청 시 예외 발생")
        void commandOperations_UserNotFound_ThrowsException() {
            // given
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.PREPARING,
                    null,
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.updateOrderStatus(principal, request))
                    .isInstanceOf(NoSuchElementException.class);

            verify(sellerRepository, never()).findByUserId(any(String.class));
            verify(orderRepository, never()).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 판매자가 아닌 사용자로 요청 시 예외 발생")
        void commandOperations_NotSeller_ThrowsException() {
            // given
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.PREPARING,
                    null,
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.updateOrderStatus(principal, request))
                    .isInstanceOf(NoSuchElementException.class);

            verify(orderRepository, never()).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 다른 판매자의 주문에 접근 시 예외 발생")
        void commandOperations_UnauthorizedAccess_ThrowsException() {
            // given
            // 다른 판매자의 상품으로 주문 구성
            Sellers otherSeller = Sellers.builder()
                    .userId("otherSeller123")
                    .vendorName("다른 판매자")
                    .build();

            Products otherProduct = Products.builder()
                    .id("otherProduct")
                    .seller(otherSeller)
                    .build();

            OrderItems otherOrderItem = OrderItems.builder()
                    .id("otherOrderItem")
                    .products(otherProduct)
                    .build();

            Orders otherOrder = Orders.builder()
                    .id("otherOrder")
                    .orderNumber(testOrderNumber)
                    .orderItems(Arrays.asList(otherOrderItem))
                    .build();

            Shipments otherShipment = Shipments.builder()
                    .id("otherShipment")
                    .orders(otherOrder)
                    .build();

            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(
                    testOrderNumber,
                    OrderStatus.PREPARING,
                    null,
                    false,
                    null
            );

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(otherShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderCommandService.updateOrderStatus(principal, request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(orderRepository, never()).save(any(Orders.class));
        }
    }
}