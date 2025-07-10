package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.LogisticsTrackingService;
import com.team5.catdogeats.orders.dto.response.TrackingResponse;
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
@DisplayName("ShipmentSyncService 배송 상태 동기화 테스트")
class ShipmentSyncServiceImplTest {

    @InjectMocks
    private ShipmentSyncServiceImpl shipmentSyncService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private LogisticsTrackingService logisticsTrackingService;

    // 테스트 데이터
    private UserPrincipal principal;
    private Users testUser;
    private Sellers testSeller;
    private Orders testOrder1;
    private Orders testOrder2;
    private Orders testOrder3;
    private Shipments testShipment1;
    private Shipments testShipment2;
    private Shipments testShipment3;
    private ZonedDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = ZonedDateTime.now();

        // JWT 인증 정보
        principal = new UserPrincipal("google", "google123");

        // 테스트 사용자
        testUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .name("테스트 판매자")
                .userNameAttribute("sub")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build();

        // 테스트 판매자
        testSeller = Sellers.builder()
                .userId("user123")
                .user(testUser)
                .vendorName("테스트 스토어")
                .businessNumber("123-45-67890")
                .build();

        // 테스트 주문들
        testOrder1 = Orders.builder()
                .id("order1")
                .orderNumber("ORDER-2025-001")
                .orderStatus(OrderStatus.IN_DELIVERY)
                .user(testUser)
                .build();

        testOrder2 = Orders.builder()
                .id("order2")
                .orderNumber("ORDER-2025-002")
                .orderStatus(OrderStatus.IN_DELIVERY)
                .user(testUser)
                .build();

        testOrder3 = Orders.builder()
                .id("order3")
                .orderNumber("ORDER-2025-003")
                .orderStatus(OrderStatus.IN_DELIVERY)
                .user(testUser)
                .build();

        // 테스트 배송 정보들
        testShipment1 = Shipments.builder()
                .id("shipment1")
                .orders(testOrder1)
                .user(testUser)
                .seller(testSeller)
                .courier("CJ대한통운")
                .trackingNumber("123456789012")
                .shippedAt(testDateTime.minusDays(2))
                .build();

        testShipment2 = Shipments.builder()
                .id("shipment2")
                .orders(testOrder2)
                .user(testUser)
                .seller(testSeller)
                .courier("한진택배")
                .trackingNumber("987654321098")
                .shippedAt(testDateTime.minusDays(1))
                .build();

        testShipment3 = Shipments.builder()
                .id("shipment3")
                .orders(testOrder3)
                .user(testUser)
                .seller(testSeller)
                .courier("롯데택배")
                .trackingNumber("555666777888")
                .shippedAt(testDateTime.minusDays(3))
                .build();
    }

    @Nested
    @DisplayName("전체 배송 상태 동기화 테스트")
    class SyncAllShipmentStatusTests {

        @Test
        @DisplayName("✅ 배송 완료 주문들이 정상 업데이트됨")
        void syncAllShipmentStatus_DeliveredOrders_UpdatedSuccessfully() {
            // given
            List<Shipments> inDeliveryShipments = Arrays.asList(testShipment1, testShipment2, testShipment3);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(inDeliveryShipments);

            // 배송 완료된 주문들에 대한 물류 서버 응답
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "DELIVERED",
                            testDateTime.minusDays(2), testDateTime, List.of())));
            given(logisticsTrackingService.getTrackingInfo("987654321098"))
                    .willReturn(Optional.of(new TrackingResponse("987654321098", "04", "DELIVERED",
                            testDateTime.minusDays(1), testDateTime, List.of())));
            given(logisticsTrackingService.getTrackingInfo("555666777888"))
                    .willReturn(Optional.of(new TrackingResponse("555666777888", "08", "DELIVERED",
                            testDateTime.minusDays(3), testDateTime, List.of())));

            given(orderRepository.save(any(Orders.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(shipmentRepository.save(any(Shipments.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(3);
            assertThat(response.updatedOrders()).isEqualTo(3);
            assertThat(response.failedOrders()).isEqualTo(0);
            assertThat(response.updatedOrderList()).hasSize(3);
            assertThat(response.failedOrderList()).isEmpty();
            assertThat(response.message()).contains("3개 주문이 배송 완료로 업데이트되었습니다");

            // 저장 호출 검증
            verify(orderRepository, times(3)).save(any(Orders.class));
            verify(shipmentRepository, times(3)).save(any(Shipments.class));
        }

        @Test
        @DisplayName("✅ 일부 주문만 배송 완료된 경우")
        void syncAllShipmentStatus_PartialDelivery_MixedResults() {
            // given
            List<Shipments> inDeliveryShipments = Arrays.asList(testShipment1, testShipment2, testShipment3);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(inDeliveryShipments);

            // 혼합된 배송 상태 응답
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "DELIVERED",
                            testDateTime.minusDays(2), testDateTime, List.of())));
            given(logisticsTrackingService.getTrackingInfo("987654321098"))
                    .willReturn(Optional.of(new TrackingResponse("987654321098", "04", "IN_TRANSIT",
                            testDateTime.minusDays(1), null, List.of())));
            given(logisticsTrackingService.getTrackingInfo("555666777888"))
                    .willReturn(Optional.of(new TrackingResponse("555666777888", "08", "DELIVERED",
                            testDateTime.minusDays(3), testDateTime, List.of())));

            given(orderRepository.save(any(Orders.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(shipmentRepository.save(any(Shipments.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(3);
            assertThat(response.updatedOrders()).isEqualTo(2);
            assertThat(response.failedOrders()).isEqualTo(0);
            assertThat(response.updatedOrderList()).hasSize(2);
            assertThat(response.failedOrderList()).isEmpty();
            assertThat(response.message()).contains("2개 주문이 배송 완료로 업데이트되었습니다");

            // 업데이트된 주문들만 검증
            List<String> updatedOrderNumbers = response.updatedOrderList().stream()
                    .map(ShipmentSyncResponse.UpdatedOrderInfo::orderNumber)
                    .toList();
            assertThat(updatedOrderNumbers).containsExactlyInAnyOrder("ORDER-2025-001", "ORDER-2025-003");

            // 저장 호출 검증 (배송 완료된 주문 2개만)
            verify(orderRepository, times(2)).save(any(Orders.class));
            verify(shipmentRepository, times(2)).save(any(Shipments.class));
        }

        @Test
        @DisplayName("✅ 모든 주문이 이미 최신 상태 (업데이트 없음)")
        void syncAllShipmentStatus_NoUpdatesNeeded_Success() {
            // given
            List<Shipments> inDeliveryShipments = Arrays.asList(testShipment1, testShipment2);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(inDeliveryShipments);

            // 모든 주문이 아직 배송 중 상태
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "IN_TRANSIT",
                            testDateTime.minusDays(2), null, List.of())));
            given(logisticsTrackingService.getTrackingInfo("987654321098"))
                    .willReturn(Optional.of(new TrackingResponse("987654321098", "04", "IN_TRANSIT",
                            testDateTime.minusDays(1), null, List.of())));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(2);
            assertThat(response.updatedOrders()).isEqualTo(0);
            assertThat(response.failedOrders()).isEqualTo(0);
            assertThat(response.updatedOrderList()).isEmpty();
            assertThat(response.failedOrderList()).isEmpty();
            assertThat(response.message()).isEqualTo("모든 주문이 이미 최신 상태입니다.");

            // 저장 호출이 없어야 함
            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository, never()).save(any(Shipments.class));
        }

        @Test
        @DisplayName("✅ 배송 중인 주문이 없는 경우")
        void syncAllShipmentStatus_NoInDeliveryOrders_EmptyResult() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(List.of());

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(0);
            assertThat(response.updatedOrders()).isEqualTo(0);
            assertThat(response.failedOrders()).isEqualTo(0);
            assertThat(response.updatedOrderList()).isEmpty();
            assertThat(response.failedOrderList()).isEmpty();

            verify(logisticsTrackingService, never()).getTrackingInfo(any(String.class));
        }
    }

    @Nested
    @DisplayName("물류 서버 연동 오류 처리 테스트")
    class LogisticsServerErrorTests {

        @Test
        @DisplayName("✅ 물류 서버 응답 없음 처리")
        void syncAllShipmentStatus_LogisticsServerNoResponse_FailedOrder() {
            // given
            List<Shipments> inDeliveryShipments = Arrays.asList(testShipment1);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(inDeliveryShipments);

            // 물류 서버에서 응답 없음
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.empty());

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(1);
            assertThat(response.updatedOrders()).isEqualTo(0);
            assertThat(response.failedOrders()).isEqualTo(1);
            assertThat(response.updatedOrderList()).isEmpty();
            assertThat(response.failedOrderList()).hasSize(1);

            ShipmentSyncResponse.FailedOrderInfo failedOrder = response.failedOrderList().get(0);
            assertThat(failedOrder.orderNumber()).isEqualTo("ORDER-2025-001");
            assertThat(failedOrder.trackingNumber()).isEqualTo("123456789012");
            assertThat(failedOrder.errorReason()).contains("물류 서버에서 배송 정보를 조회할 수 없습니다");

            // 저장 호출이 없어야 함
            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository, never()).save(any(Shipments.class));
        }

        @Test
        @DisplayName("✅ 물류 서버 예외 발생 처리")
        void syncAllShipmentStatus_LogisticsServerException_FailedOrder() {
            // given
            List<Shipments> inDeliveryShipments = Arrays.asList(testShipment1);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(inDeliveryShipments);

            // 물류 서버에서 예외 발생
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willThrow(new RuntimeException("물류 서버 연결 실패"));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCheckedOrders()).isEqualTo(1);
            assertThat(response.updatedOrders()).isEqualTo(0);
            assertThat(response.failedOrders()).isEqualTo(1);
            assertThat(response.failedOrderList()).hasSize(1);

            ShipmentSyncResponse.FailedOrderInfo failedOrder = response.failedOrderList().get(0);
            assertThat(failedOrder.errorReason()).contains("물류 서버 연결 실패");
        }
    }

    @Nested
    @DisplayName("권한 검증 테스트")
    class AuthorizationTests {

        @Test
        @DisplayName("❌ 존재하지 않는 사용자로 동기화 시 예외 발생")
        void syncAllShipmentStatus_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> shipmentSyncService.syncAllShipmentStatus(principal))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");

            verify(sellersRepository, never()).findByUserId(any(String.class));
            verify(shipmentRepository, never()).findByOrderStatusAndTrackingNumberIsNotNull(any(OrderStatus.class));
        }

        @Test
        @DisplayName("❌ 판매자가 아닌 사용자로 동기화 시 예외 발생")
        void syncAllShipmentStatus_NotSeller_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> shipmentSyncService.syncAllShipmentStatus(principal))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("판매자 정보를 찾을 수 없습니다");

            verify(shipmentRepository, never()).findByOrderStatusAndTrackingNumberIsNotNull(any(OrderStatus.class));
        }
    }

    @Nested
    @DisplayName("배송 상태 처리 로직 테스트")
    class ShipmentProcessingTests {

        @Test
        @DisplayName("✅ 주문 상태와 배송 정보가 올바르게 업데이트됨")
        void processShipmentSync_StatusAndTimestampsUpdated_Success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(Arrays.asList(testShipment1));

            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "DELIVERED",
                            testDateTime.minusDays(2), testDateTime, List.of())));

            given(orderRepository.save(any(Orders.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(shipmentRepository.save(any(Shipments.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            ArgumentCaptor<Shipments> shipmentCaptor = ArgumentCaptor.forClass(Shipments.class);

            verify(orderRepository).save(orderCaptor.capture());
            verify(shipmentRepository).save(shipmentCaptor.capture());

            Orders savedOrder = orderCaptor.getValue();
            Shipments savedShipment = shipmentCaptor.getValue();

            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(savedShipment.getDeliveredAt()).isNotNull();
            assertThat(savedShipment.getTrackingUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ OUT_FOR_DELIVERY 상태는 업데이트하지 않음")
        void processShipmentSync_OutForDeliveryStatus_NoUpdate() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(Arrays.asList(testShipment1));

            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "OUT_FOR_DELIVERY",
                            testDateTime.minusDays(2), null, List.of())));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response.updatedOrders()).isEqualTo(0);
            assertThat(response.failedOrders()).isEqualTo(0);

            verify(orderRepository, never()).save(any(Orders.class));
            verify(shipmentRepository, never()).save(any(Shipments.class));
        }
    }

    @Nested
    @DisplayName("응답 메시지 생성 테스트")
    class ResponseMessageTests {

        @Test
        @DisplayName("✅ 성공과 실패가 모두 있는 경우 메시지 생성")
        void buildResponseMessage_SuccessAndFailure_CorrectMessage() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(Arrays.asList(testShipment1, testShipment2));
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "DELIVERED",
                            testDateTime.minusDays(2), testDateTime, List.of())));
            given(logisticsTrackingService.getTrackingInfo("987654321098"))
                    .willReturn(Optional.empty());

            given(orderRepository.save(any(Orders.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(shipmentRepository.save(any(Shipments.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response.message()).isEqualTo("1개 주문이 업데이트되었습니다. 1개 주문에서 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("✅ 업데이트가 없는 경우 메시지 생성")
        void buildResponseMessage_NoUpdates_CorrectMessage() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY))
                    .willReturn(Arrays.asList(testShipment1));
            given(logisticsTrackingService.getTrackingInfo("123456789012"))
                    .willReturn(Optional.of(new TrackingResponse("123456789012", "01", "IN_TRANSIT",
                            testDateTime.minusDays(2), null, List.of())));

            // when
            ShipmentSyncResponse response = shipmentSyncService.syncAllShipmentStatus(principal);

            // then
            assertThat(response.message()).isEqualTo("모든 주문이 이미 최신 상태입니다.");
        }
    }
}