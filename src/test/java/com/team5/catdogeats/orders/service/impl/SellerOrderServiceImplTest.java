//package com.team5.catdogeats.orders.service.impl;
//
//import com.team5.catdogeats.auth.dto.UserPrincipal;
//import com.team5.catdogeats.orders.domain.Orders;
//import com.team5.catdogeats.orders.domain.Shipments;
//import com.team5.catdogeats.orders.domain.enums.OrderStatus;
//import com.team5.catdogeats.orders.domain.mapping.OrderItems;
//import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
//import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
//import com.team5.catdogeats.orders.service.SellerOrderQueryService;
//import com.team5.catdogeats.products.domain.Products;
//import com.team5.catdogeats.users.domain.Users;
//import com.team5.catdogeats.users.domain.enums.Role;
//import com.team5.catdogeats.users.domain.mapping.Sellers;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//
//import java.time.ZonedDateTime;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("SellerOrderService 읽기 작업 테스트")
//class SellerOrderServiceImplTest {
//
//    @InjectMocks
//    private SellerOrderServiceImpl sellerOrderService;
//
//    @Mock
//    private SellerOrderQueryService queryService;
//
//    // 테스트 데이터
//    private UserPrincipal principal;
//    private Users testUser;
//    private Sellers testSeller;
//    private Orders testOrder;
//    private Shipments testShipment;
//    private Products testProduct1;
//    private Products testProduct2;
//    private OrderItems testOrderItem1;
//    private OrderItems testOrderItem2;
//    private String testOrderNumber;
//    private Pageable testPageable;
//
//    @BeforeEach
//    void setUp() {
//        ZonedDateTime testDateTime = ZonedDateTime.now();
//        testOrderNumber = "ORDER-2024-1234567890";
//        testPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
//
//        // JWT 인증 정보
//        principal = new UserPrincipal("google", "google123");
//
//        // 테스트 사용자
//        testUser = Users.builder()
//                .id("user123")
//                .provider("google")
//                .providerId("google123")
//                .name("테스트 판매자")
//                .userNameAttribute("sub")
//                .role(Role.ROLE_SELLER)
//                .accountDisable(false)
//                .build();
//
//        // 테스트 판매자
//        testSeller = Sellers.builder()
//                .userId("user123")
//                .user(testUser)
//                .vendorName("테스트 스토어")
//                .businessNumber("123-45-67890")
//                .build();
//
//        // 테스트 상품들
//        testProduct1 = Products.builder()
//                .id("product1")
//                .title("테스트 상품1")
//                .price(25000L)
//                .seller(testSeller)
//                .build();
//
//        testProduct2 = Products.builder()
//                .id("product2")
//                .title("테스트 상품2")
//                .price(15000L)
//                .seller(testSeller)
//                .build();
//
//        // 테스트 주문 상품들
//        testOrderItem1 = OrderItems.builder()
//                .id("item1")
//                .quantity(2)
//                .price(25000L)
//                .products(testProduct1)
//                .build();
//
//        testOrderItem2 = OrderItems.builder()
//                .id("item2")
//                .quantity(1)
//                .price(15000L)
//                .products(testProduct2)
//                .build();
//
//        // 테스트 주문
//        testOrder = Orders.builder()
//                .id("order123")
//                .orderNumber(testOrderNumber)
//                .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                .user(testUser)
//                .orderItems(Arrays.asList(testOrderItem1, testOrderItem2))
//                .build();
//
//        // 테스트 배송 정보
//        testShipment = Shipments.builder()
//                .id("shipment123")
//                .orders(testOrder)
//                .user(testUser)
//                .seller(testSeller)
//                .recipientName("김철수")
//                .recipientPhone("010-1234-5678")
//                .postalCode("12345")
//                .streetAddress("서울시 강남구 테헤란로 123")
//                .detailAddress("456호")
//                .deliveryRequest("문 앞에 놓아주세요")
//                .build();
//    }
//
//    @Nested
//    @DisplayName("주문 상세 조회 테스트")
//    class GetSellerOrderDetailTests {
//
//        @Test
//        @DisplayName("✅ 정상적인 주문 상세 조회")
//        void getSellerOrderDetail_Success() {
//            // given
//            SellerOrderDetailResponse expectedResponse = SellerOrderDetailResponse.builder()
//                    .orderNumber(testOrderNumber)
//                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                    .orderDate(testOrder.getCreatedAt())
//                    .shippingAddress(SellerOrderDetailResponse.ShippingAddress.builder()
//                            .recipientName("김철수")
//                            .recipientPhone("010-1234-5678")
//                            .zipCode("12345")
//                            .address("서울시 강남구 테헤란로 123")
//                            .addressDetail("456호")
//                            .fullAddress("서울시 강남구 테헤란로 123 456호")
//                            .deliveryRequest("문 앞에 놓아주세요")
//                            .build())
//                    .orderItems(List.of(
//                            SellerOrderDetailResponse.SellerOrderDetailItem.builder()
//                                    .orderItemId("item1")
//                                    .productId("product1")
//                                    .productName("테스트 상품1")
//                                    .quantity(2)
//                                    .unitPrice(25000L)
//                                    .totalPrice(50000L)
//                                    .build(),
//                            SellerOrderDetailResponse.SellerOrderDetailItem.builder()
//                                    .orderItemId("item2")
//                                    .productId("product2")
//                                    .productName("테스트 상품2")
//                                    .quantity(1)
//                                    .unitPrice(15000L)
//                                    .totalPrice(15000L)
//                                    .build()
//                    ))
//                    .orderSummary(SellerOrderDetailResponse.OrderSummary.builder()
//                            .itemCount(2)
//                            .totalProductPrice(65000L)
//                            .deliveryFee(0L)
//                            .totalAmount(65000L)
//                            .build())
//                    .build();
//
//            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
//                    .willReturn(expectedResponse);
//
//            // when
//            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);
//
//            // then
//            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
//            assertThat(response.orderStatus()).isEqualTo(OrderStatus.READY_FOR_SHIPMENT);
//            assertThat(response.orderItems()).hasSize(2);
//
//            // totalAmount() 메서드 테스트
//            assertThat(response.getTotalAmount()).isEqualTo(65000L);
//
//            // 받는 사람 정보 검증
//            SellerOrderDetailResponse.ShippingAddress recipientInfo = response.shippingAddress();
//            assertThat(recipientInfo.recipientName()).isEqualTo("김철수");
//            assertThat(recipientInfo.recipientPhone()).isEqualTo("010-1234-5678");
//
//            // 주문 상품 정보 검증
//            List<SellerOrderDetailResponse.SellerOrderDetailItem> orderItems = response.orderItems();
//            SellerOrderDetailResponse.SellerOrderDetailItem firstItem = orderItems.get(0);
//            assertThat(firstItem.orderItemId()).isEqualTo("item1");
//            assertThat(firstItem.productName()).isEqualTo("테스트 상품1");
//            assertThat(firstItem.quantity()).isEqualTo(2);
//            assertThat(firstItem.unitPrice()).isEqualTo(25000L);
//            assertThat(firstItem.totalPrice()).isEqualTo(50000L);
//
//            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
//        }
//
//        @Test
//        @DisplayName("✅ 서비스 계층에서 쿼리 서비스 위임 확인")
//        void getSellerOrderDetail_DelegationToQueryService() {
//            // given
//            SellerOrderDetailResponse mockResponse = SellerOrderDetailResponse.builder()
//                    .orderNumber(testOrderNumber)
//                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                    .orderDate(testOrder.getCreatedAt())
//                    .build();
//
//            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
//                    .willReturn(mockResponse);
//
//            // when
//            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);
//
//            // then
//            assertThat(response).isSameAs(mockResponse);
//            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
//        }
//
//        @Test
//        @DisplayName("❌ 쿼리 서비스에서 예외 발생 시 그대로 전파")
//        void getSellerOrderDetail_QueryServiceException_Propagated() {
//            // given
//            RuntimeException expectedException = new RuntimeException("쿼리 서비스 에러");
//            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
//                    .willThrow(expectedException);
//
//            // when & then
//            assertThatThrownBy(() ->
//                    sellerOrderService.getSellerOrderDetail(principal, testOrderNumber)
//            ).isSameAs(expectedException);
//
//            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
//        }
//    }
//
//    @Nested
//    @DisplayName("주문 목록 조회 테스트")
//    class GetSellerOrdersTests {
//
//        @Test
//        @DisplayName("✅ 정상적인 주문 목록 조회")
//        void getSellerOrders_Success() {
//            // given
//            SellerOrderListResponse expectedResponse = SellerOrderListResponse.builder()
//                    .orders(List.of(
//                            SellerOrderListResponse.SellerOrderSummary.builder()
//                                    .orderNumber(testOrderNumber)
//                                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                                    .orderDate(testOrder.getCreatedAt())
//                                    .buyerName("테스트 판매자")
//                                    .maskedBuyerName("테***자")
//                                    .orderItems(List.of(
//                                            SellerOrderListResponse.SellerOrderItem.builder()
//                                                    .orderItemId("item1")
//                                                    .productId("product1")
//                                                    .productName("테스트 상품1")
//                                                    .quantity(2)
//                                                    .unitPrice(25000L)
//                                                    .totalPrice(50000L)
//                                                    .build()
//                                    ))
//                                    .orderSummary(SellerOrderListResponse.OrderSummaryInfo.builder()
//                                            .itemCount(2)
//                                            .totalAmount(65000L)
//                                            .build())
//                                    .shipmentInfo(SellerOrderListResponse.ShipmentBasicInfo.builder()
//                                            .isShipped(false)
//                                            .build())
//                                    .build()
//                    ))
//                    .currentPage(0)
//                    .totalPages(1)
//                    .totalElements(1L)
//                    .pageSize(10)
//                    .hasNext(false)
//                    .hasPrevious(false)
//                    .build();
//
//            given(queryService.getSellerOrders(principal, testPageable))
//                    .willReturn(expectedResponse);
//
//            // when
//            SellerOrderListResponse response = sellerOrderService.getSellerOrders(principal, testPageable);
//
//            // then
//            assertThat(response.orders()).hasSize(1);
//            assertThat(response.currentPage()).isEqualTo(0);
//            assertThat(response.totalElements()).isEqualTo(1L);
//            assertThat(response.hasNext()).isFalse();
//            assertThat(response.hasPrevious()).isFalse();
//
//            verify(queryService).getSellerOrders(principal, testPageable);
//        }
//
//        @Test
//        @DisplayName("✅ 빈 주문 목록 조회")
//        void getSellerOrders_EmptyResult_Success() {
//            // given
//            SellerOrderListResponse emptyResponse = SellerOrderListResponse.builder()
//                    .orders(List.of())
//                    .currentPage(0)
//                    .totalPages(0)
//                    .totalElements(0L)
//                    .pageSize(10)
//                    .hasNext(false)
//                    .hasPrevious(false)
//                    .build();
//
//            given(queryService.getSellerOrders(principal, testPageable))
//                    .willReturn(emptyResponse);
//
//            // when
//            SellerOrderListResponse response = sellerOrderService.getSellerOrders(principal, testPageable);
//
//            // then
//            assertThat(response.orders()).isEmpty();
//            assertThat(response.totalElements()).isEqualTo(0L);
//
//            verify(queryService).getSellerOrders(principal, testPageable);
//        }
//
//        @Test
//        @DisplayName("✅ 서비스 계층에서 쿼리 서비스 위임 확인")
//        void getSellerOrders_DelegationToQueryService() {
//            // given
//            SellerOrderListResponse mockResponse = SellerOrderListResponse.builder()
//                    .orders(List.of())
//                    .currentPage(0)
//                    .totalPages(0)
//                    .totalElements(0L)
//                    .pageSize(10)
//                    .hasNext(false)
//                    .hasPrevious(false)
//                    .build();
//
//            given(queryService.getSellerOrders(principal, testPageable))
//                    .willReturn(mockResponse);
//
//            // when
//            SellerOrderListResponse response = sellerOrderService.getSellerOrders(principal, testPageable);
//
//            // then
//            assertThat(response).isSameAs(mockResponse);
//            verify(queryService).getSellerOrders(principal, testPageable);
//        }
//
//        @Test
//        @DisplayName("❌ 쿼리 서비스에서 예외 발생 시 그대로 전파")
//        void getSellerOrders_QueryServiceException_Propagated() {
//            // given
//            RuntimeException expectedException = new RuntimeException("쿼리 서비스 에러");
//            given(queryService.getSellerOrders(principal, testPageable))
//                    .willThrow(expectedException);
//
//            // when & then
//            assertThatThrownBy(() ->
//                    sellerOrderService.getSellerOrders(principal, testPageable)
//            ).isSameAs(expectedException);
//
//            verify(queryService).getSellerOrders(principal, testPageable);
//        }
//    }
//
//    @Nested
//    @DisplayName("CQRS 패턴 검증 테스트")
//    class CQRSPatternTests {
//
//        @Test
//        @DisplayName("✅ 읽기 전용 서비스임을 확인 - 모든 작업이 쿼리 서비스로 위임")
//        void verifyReadOnlyService_AllOperationsDelegatedToQueryService() {
//            // given
//            SellerOrderDetailResponse detailResponse = SellerOrderDetailResponse.builder()
//                    .orderNumber(testOrderNumber)
//                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                    .orderDate(testOrder.getCreatedAt())
//                    .build();
//
//            SellerOrderListResponse listResponse = SellerOrderListResponse.builder()
//                    .orders(List.of())
//                    .currentPage(0)
//                    .totalPages(0)
//                    .totalElements(0L)
//                    .pageSize(10)
//                    .hasNext(false)
//                    .hasPrevious(false)
//                    .build();
//
//            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
//                    .willReturn(detailResponse);
//            given(queryService.getSellerOrders(principal, testPageable))
//                    .willReturn(listResponse);
//
//            // when
//            SellerOrderDetailResponse detailResult = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);
//            SellerOrderListResponse listResult = sellerOrderService.getSellerOrders(principal, testPageable);
//
//            // then
//            assertThat(detailResult).isSameAs(detailResponse);
//            assertThat(listResult).isSameAs(listResponse);
//
//            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
//            verify(queryService).getSellerOrders(principal, testPageable);
//        }
//
//        @Test
//        @DisplayName("✅ 서비스 계층의 역할 - 단순 위임만 수행")
//        void verifyServiceLayerRole_SimpleDelegationOnly() {
//            // given
//            SellerOrderDetailResponse mockResponse = SellerOrderDetailResponse.builder()
//                    .orderNumber(testOrderNumber)
//                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                    .orderDate(testOrder.getCreatedAt())
//                    .build();
//
//            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
//                    .willReturn(mockResponse);
//
//            // when
//            long startTime = System.nanoTime();
//            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);
//            long endTime = System.nanoTime();
//
//            // then
//            // 응답 검증
//            assertThat(response).isSameAs(mockResponse);
//
//            // 성능 검증 - 단순 위임이므로 매우 빨라야 함 (1ms 미만)
//            long executionTimeNanos = endTime - startTime;
//            long executionTimeMillis = executionTimeNanos / 1_000_000;
//            assertThat(executionTimeMillis).isLessThan(1L);
//
//            // 위임 검증
//            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
//        }
//    }
//
//    @Nested
//    @DisplayName("성능 테스트")
//    class PerformanceTests {
//
//        @Test
//        @DisplayName("✅ 대량 요청 처리 성능")
//        void handleMultipleRequests_Performance() {
//            // given
//            SellerOrderDetailResponse mockResponse = SellerOrderDetailResponse.builder()
//                    .orderNumber(testOrderNumber)
//                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
//                    .orderDate(testOrder.getCreatedAt())
//                    .build();
//
//            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
//                    .willReturn(mockResponse);
//
//            // when
//            long startTime = System.currentTimeMillis();
//
//            for (int i = 0; i < 100; i++) {
//                SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);
//                assertThat(response).isNotNull();
//            }
//
//            long endTime = System.currentTimeMillis();
//            long executionTime = endTime - startTime;
//
//            // then
//            // 100번 요청이 1초 이내에 완료되어야 함
//            assertThat(executionTime).isLessThan(1000L);
//
//            verify(queryService, org.mockito.Mockito.times(100))
//                    .getSellerOrderDetail(principal, testOrderNumber);
//        }
//    }
//}