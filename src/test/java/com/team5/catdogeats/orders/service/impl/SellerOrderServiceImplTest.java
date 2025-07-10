package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.service.SellerOrderQueryService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerOrderService 읽기 작업 테스트")
class SellerOrderServiceImplTest {

    @InjectMocks
    private SellerOrderServiceImpl sellerOrderService;

    @Mock
    private SellerOrderQueryService queryService;

    // 테스트 데이터
    private UserPrincipal principal;
    private Users testUser;
    private Sellers testSeller;
    private Orders testOrder;
    private Shipments testShipment;
    private Products testProduct1;
    private Products testProduct2;
    private OrderItems testOrderItem1;
    private OrderItems testOrderItem2;
    private String testOrderNumber;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        // UserPrincipal 초기화
        principal = new UserPrincipal("google", "google123");
        testOrderNumber = "ORDER-2025-001";
        testPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

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

        // 테스트 상품들 생성
        testProduct1 = Products.builder()
                .id("product1")
                .title("프리미엄 강아지 사료")
                .price(25000L)
                .seller(testSeller)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(15000L)
                .seller(testSeller)
                .build();

        // 테스트 주문 상품들 생성
        testOrderItem1 = OrderItems.builder()
                .id("orderItem1")
                .products(testProduct1)
                .quantity(2)
                .price(25000L)
                .build();

        testOrderItem2 = OrderItems.builder()
                .id("orderItem2")
                .products(testProduct2)
                .quantity(1)
                .price(15000L)
                .build();

        // 테스트 주문 생성
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(testOrderNumber)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(65000L)
                .orderItems(Arrays.asList(testOrderItem1, testOrderItem2))
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
    @DisplayName("판매자용 주문 상세 조회 테스트")
    class GetSellerOrderDetailTests {

        @Test
        @DisplayName("✅ 정상적인 주문 상세 조회 성공")
        void getSellerOrderDetail_Success() {
            // given
            SellerOrderDetailResponse expectedResponse = SellerOrderDetailResponse.builder()
                    .orderNumber(testOrderNumber)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                    .recipientInfo(SellerOrderDetailResponse.RecipientInfo.builder()
                            .recipientName("김철수")
                            .recipientPhone("010-1234-5678")
                            .postalCode("06234")
                            .shippingAddress("서울시 강남구 테헤란로 123")
                            .detailAddress("456호")
                            .deliveryNote("문 앞에 놓아주세요")
                            .build())
                    .orderItems(Arrays.asList(
                            SellerOrderDetailResponse.SellerOrderItem.builder()
                                    .productId("product1")
                                    .productTitle("프리미엄 강아지 사료")
                                    .quantity(2)
                                    .unitPrice(25000L)
                                    .itemTotalPrice(50000L)
                                    .build(),
                            SellerOrderDetailResponse.SellerOrderItem.builder()
                                    .productId("product2")
                                    .productTitle("고양이 간식")
                                    .quantity(1)
                                    .unitPrice(15000L)
                                    .itemTotalPrice(15000L)
                                    .build()
                    ))
                    .totalAmount(65000L)
                    .orderedAt(ZonedDateTime.now())
                    .build();

            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
                    .willReturn(expectedResponse);

            // when
            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(response.orderItems()).hasSize(2);
            assertThat(response.totalAmount()).isEqualTo(65000L);

            // 수신자 정보 검증
            SellerOrderDetailResponse.RecipientInfo recipientInfo = response.recipientInfo();
            assertThat(recipientInfo.recipientName()).isEqualTo("김철수");
            assertThat(recipientInfo.recipientPhone()).isEqualTo("010-1234-5678");
            assertThat(recipientInfo.deliveryNote()).isEqualTo("문 앞에 놓아주세요");

            // 주문 상품 정보 검증
            List<SellerOrderDetailResponse.SellerOrderItem> orderItems = response.orderItems();
            SellerOrderDetailResponse.SellerOrderItem firstItem = orderItems.get(0);
            assertThat(firstItem.productId()).isEqualTo("product1");
            assertThat(firstItem.productTitle()).isEqualTo("프리미엄 강아지 사료");
            assertThat(firstItem.quantity()).isEqualTo(2);
            assertThat(firstItem.unitPrice()).isEqualTo(25000L);
            assertThat(firstItem.itemTotalPrice()).isEqualTo(50000L);

            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문으로 조회 시 예외 발생")
        void getSellerOrderDetail_OrderNotFound_ThrowsException() {
            // given
            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
                    .willThrow(new RuntimeException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            // when & then
            assertThatThrownBy(() -> sellerOrderService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("주문을 찾을 수 없거나 접근 권한이 없습니다");

            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
        }
    }

    @Nested
    @DisplayName("판매자용 주문 목록 조회 테스트")
    class GetSellerOrdersTests {

        @Test
        @DisplayName("✅ 정상적인 주문 목록 조회 성공")
        void getSellerOrders_Success() {
            // given
            SellerOrderListResponse expectedResponse = SellerOrderListResponse.builder()
                    .orders(Arrays.asList(
                            SellerOrderListResponse.SellerOrderSummary.builder()
                                    .orderNumber("ORDER-2025-001")
                                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                                    .recipientName("김철수")
                                    .maskedPhone("010-1234-****")
                                    .totalAmount(65000L)
                                    .orderItemCount(2)
                                    .orderedAt(ZonedDateTime.now())
                                    .build(),
                            SellerOrderListResponse.SellerOrderSummary.builder()
                                    .orderNumber("ORDER-2025-002")
                                    .orderStatus(OrderStatus.PREPARING)
                                    .recipientName("이영희")
                                    .maskedPhone("010-5678-****")
                                    .totalAmount(35000L)
                                    .orderItemCount(1)
                                    .orderedAt(ZonedDateTime.now().minusDays(1))
                                    .build()
                    ))
                    .currentPage(0)
                    .totalPages(1)
                    .totalElements(2L)
                    .hasNext(false)
                    .build();

            given(queryService.getSellerOrders(principal, testPageable))
                    .willReturn(expectedResponse);

            // when
            SellerOrderListResponse response = sellerOrderService.getSellerOrders(principal, testPageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orders()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(2L);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.hasNext()).isFalse();

            // 첫 번째 주문 검증
            SellerOrderListResponse.SellerOrderSummary firstOrder = response.orders().get(0);
            assertThat(firstOrder.orderNumber()).isEqualTo("ORDER-2025-001");
            assertThat(firstOrder.orderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(firstOrder.recipientName()).isEqualTo("김철수");
            assertThat(firstOrder.maskedPhone()).isEqualTo("010-1234-****");
            assertThat(firstOrder.totalAmount()).isEqualTo(65000L);
            assertThat(firstOrder.orderItemCount()).isEqualTo(2);

            verify(queryService).getSellerOrders(principal, testPageable);
        }

        @Test
        @DisplayName("✅ 빈 주문 목록 조회 성공")
        void getSellerOrders_EmptyList_Success() {
            // given
            SellerOrderListResponse expectedResponse = SellerOrderListResponse.builder()
                    .orders(List.of())
                    .currentPage(0)
                    .totalPages(0)
                    .totalElements(0L)
                    .hasNext(false)
                    .build();

            given(queryService.getSellerOrders(principal, testPageable))
                    .willReturn(expectedResponse);

            // when
            SellerOrderListResponse response = sellerOrderService.getSellerOrders(principal, testPageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orders()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0L);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(0);
            assertThat(response.hasNext()).isFalse();

            verify(queryService).getSellerOrders(principal, testPageable);
        }

        @Test
        @DisplayName("❌ 판매자 권한 없는 사용자로 조회 시 예외 발생")
        void getSellerOrders_UnauthorizedUser_ThrowsException() {
            // given
            given(queryService.getSellerOrders(principal, testPageable))
                    .willThrow(new IllegalArgumentException("판매자 권한이 없습니다"));

            // when & then
            assertThatThrownBy(() -> sellerOrderService.getSellerOrders(principal, testPageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("판매자 권한이 없습니다");

            verify(queryService).getSellerOrders(principal, testPageable);
        }
    }

    @Nested
    @DisplayName("서비스 의존성 및 위임 테스트")
    class ServiceDelegationTests {

        @Test
        @DisplayName("✅ 주문 상세 조회가 QueryService에 올바르게 위임됨")
        void getSellerOrderDetail_ProperlyDelegated() {
            // given
            SellerOrderDetailResponse mockResponse = SellerOrderDetailResponse.builder()
                    .orderNumber(testOrderNumber)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                    .build();

            given(queryService.getSellerOrderDetail(principal, testOrderNumber))
                    .willReturn(mockResponse);

            // when
            SellerOrderDetailResponse result = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            assertThat(result).isEqualTo(mockResponse);
            verify(queryService).getSellerOrderDetail(principal, testOrderNumber);
        }

        @Test
        @DisplayName("✅ 주문 목록 조회가 QueryService에 올바르게 위임됨")
        void getSellerOrders_ProperlyDelegated() {
            // given
            SellerOrderListResponse mockResponse = SellerOrderListResponse.builder()
                    .orders(List.of())
                    .totalElements(0L)
                    .build();

            given(queryService.getSellerOrders(principal, testPageable))
                    .willReturn(mockResponse);

            // when
            SellerOrderListResponse result = sellerOrderService.getSellerOrders(principal, testPageable);

            // then
            assertThat(result).isEqualTo(mockResponse);
            verify(queryService).getSellerOrders(principal, testPageable);
        }
    }
}