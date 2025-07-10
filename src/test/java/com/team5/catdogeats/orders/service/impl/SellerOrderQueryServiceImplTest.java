package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerOrderQueryService 읽기 비즈니스 로직 테스트")
class SellerOrderQueryServiceImplTest {

    @InjectMocks
    private SellerOrderQueryServiceImpl sellerOrderQueryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellersRepository sellerRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    // 테스트 데이터
    private UserPrincipal principal;
    private Users testUser;
    private Sellers testSeller;
    private Sellers otherSeller;
    private Orders testOrder;
    private Shipments testShipment;
    private Products testProduct1;
    private Products testProduct2;
    private Products otherProduct;
    private OrderItems testOrderItem1;
    private OrderItems testOrderItem2;
    private OrderItems otherOrderItem;
    private String testOrderNumber;
    private ZonedDateTime testDateTime;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testDateTime = ZonedDateTime.now();
        testOrderNumber = "ORDER-2024-1234567890";
        testPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // JWT 인증 정보
        principal = UserPrincipal.builder()
                .provider("google")
                .providerId("google123")
                .name("테스트 판매자")
                .email("seller@test.com")
                .role(Rol2.SELLER)
                .build();

        // 테스트 사용자
        testUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .name("테스트 판매자")
                .email("seller@test.com")
                .role(Role.SELLER)
                .isActive(true)
                .createdAt(testDateTime)
                .build();

        // 테스트 판매자
        testSeller = Sellers.builder()
                .id("seller123")
                .userId("user123")
                .storeName("테스트 스토어")
                .businessNumber("123-45-67890")
                .phoneNumber("02-1234-5678")
                .storeDescription("테스트용 스토어")
                .isActive(true)
                .createdAt(testDateTime)
                .build();

        // 다른 판매자
        otherSeller = Sellers.builder()
                .id("otherSeller123")
                .userId("otherUser123")
                .storeName("다른 스토어")
                .businessNumber("987-65-43210")
                .phoneNumber("02-9876-5432")
                .isActive(true)
                .createdAt(testDateTime)
                .build();

        // 테스트 상품들
        testProduct1 = Products.builder()
                .id("product1")
                .name("테스트 상품1")
                .price(25000L)
                .seller(testSeller)
                .isActive(true)
                .createdAt(testDateTime)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .name("테스트 상품2")
                .price(15000L)
                .seller(testSeller)
                .isActive(true)
                .createdAt(testDateTime)
                .build();

        otherProduct = Products.builder()
                .id("otherProduct")
                .name("다른 판매자 상품")
                .price(30000L)
                .seller(otherSeller)
                .isActive(true)
                .createdAt(testDateTime)
                .build();

        // 테스트 주문 상품들
        testOrderItem1 = OrderItems.builder()
                .id("item1")
                .quantity(2)
                .unitPrice(25000L)
                .totalPrice(50000L)
                .products(testProduct1)
                .createdAt(testDateTime)
                .build();

        testOrderItem2 = OrderItems.builder()
                .id("item2")
                .quantity(1)
                .unitPrice(15000L)
                .totalPrice(15000L)
                .products(testProduct2)
                .createdAt(testDateTime)
                .build();

        otherOrderItem = OrderItems.builder()
                .id("otherItem")
                .quantity(1)
                .unitPrice(30000L)
                .totalPrice(30000L)
                .products(otherProduct)
                .createdAt(testDateTime)
                .build();

        // 테스트 주문
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(testOrderNumber)
                .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
                .user(testUser)
                .orderItems(Arrays.asList(testOrderItem1, testOrderItem2))
                .createdAt(testDateTime)
                .build();

        // 테스트 배송 정보
        testShipment = Shipments.builder()
                .id("shipment123")
                .orders(testOrder)
                .user(testUser)
                .seller(testSeller)
                .recipientName("김철수")
                .recipientPhone("010-1234-5678")
                .postalCode("12345")
                .streetAddress("서울시 강남구 테헤란로 123")
                .detailAddress("456호")
                .deliveryRequest("문 앞에 놓아주세요")
                .createdAt(testDateTime)
                .build();
    }

    @Nested
    @DisplayName("판매자 주문 상세 조회 테스트")
    class GetSellerOrderDetailTests {

        @Test
        @DisplayName("✅ 정상적인 주문 상세 조회")
        void getSellerOrderDetail_Success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            // 기본 주문 정보 검증
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.READY_FOR_SHIPMENT);
            assertThat(response.orderDate()).isEqualTo(testDateTime);

            // 주문 상품 정보 검증
            List<SellerOrderDetailResponse.SellerOrderDetailItem> orderItems = response.orderItems();
            assertThat(orderItems).hasSize(2);

            SellerOrderDetailResponse.SellerOrderDetailItem firstItem = orderItems.get(0);
            assertThat(firstItem.orderItemId()).isEqualTo("item1");
            assertThat(firstItem.productName()).isEqualTo("테스트 상품1");
            assertThat(firstItem.quantity()).isEqualTo(2);
            assertThat(firstItem.unitPrice()).isEqualTo(25000L);
            assertThat(firstItem.totalPrice()).isEqualTo(50000L);

            // 주문 요약 정보 검증
            SellerOrderDetailResponse.OrderSummary orderSummary = response.orderSummary();
            assertThat(orderSummary.totalAmount()).isEqualTo(65000L); // 25000*2 + 15000*1

            // 배송지 정보 검증
            SellerOrderDetailResponse.ShippingAddress shippingAddress = response.shippingAddress();
            assertThat(shippingAddress.recipientName()).isEqualTo("김철수");
            assertThat(shippingAddress.recipientPhone()).isEqualTo("010-1234-5678");
            assertThat(shippingAddress.fullAddress()).isEqualTo("서울시 강남구 테헤란로 123 456호");
            assertThat(shippingAddress.deliveryRequest()).isEqualTo("문 앞에 놓아주세요");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellerRepository).findByUserId("user123");
            verify(shipmentRepository).findByOrderNumber(testOrderNumber);
        }

        @Test
        @DisplayName("✅ 배송 요청사항이 null인 경우 기본값 설정")
        void getSellerOrderDetail_NullDeliveryNote_DefaultValue() {
            // given
            testShipment.setDeliveryRequest(null);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            SellerOrderDetailResponse.ShippingAddress shippingAddress = response.shippingAddress();
            assertThat(shippingAddress.deliveryRequest()).isEqualTo("배송 요청사항 없음");
        }

        @Test
        @DisplayName("✅ 운송장 정보가 등록된 경우")
        void getSellerOrderDetail_WithTrackingInfo_Success() {
            // given
            testShipment.setCourier("CJ대한통운");
            testShipment.setTrackingNumber("123456789012");
            testShipment.setShippedAt(testDateTime.minusDays(1));

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            SellerOrderDetailResponse.ShipmentInfo shipmentInfo = response.shipmentInfo();
            assertThat(shipmentInfo.courier()).isEqualTo("CJ대한통운");
            assertThat(shipmentInfo.trackingNumber()).isEqualTo("123456789012");
            assertThat(shipmentInfo.shippedAt()).isEqualTo(testDateTime.minusDays(1));
            assertThat(shipmentInfo.isShipped()).isTrue();
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문")
        void getSellerOrderDetail_OrderNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber("NON-EXISTENT-ORDER"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrderDetail(principal, "NON-EXISTENT-ORDER")
            ).isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("❌ 권한 없는 판매자의 주문 조회")
        void getSellerOrderDetail_UnauthorizedSeller_ThrowsException() {
            // given
            // 다른 판매자의 상품으로 변경
            testProduct1.setSeller(otherSeller);
            testProduct2.setSeller(otherSeller);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when & then
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 주문에 대한 접근 권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("판매자 주문 목록 조회 테스트")
    class GetSellerOrdersTests {

        @Test
        @DisplayName("✅ 정상적인 주문 목록 조회")
        void getSellerOrders_Success() {
            // given
            List<Shipments> shipmentsList = Arrays.asList(testShipment);
            Page<Shipments> shipmentsPage = new PageImpl<>(shipmentsList, testPageable, 1);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("user123", testPageable))
                    .willReturn(shipmentsPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            // 페이징 정보 검증
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.totalElements()).isEqualTo(1L);
            assertThat(response.pageSize()).isEqualTo(10);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.hasPrevious()).isFalse();

            // 주문 목록 검증
            List<SellerOrderListResponse.SellerOrderSummary> orders = response.orders();
            assertThat(orders).hasSize(1);

            SellerOrderListResponse.SellerOrderSummary orderSummary = orders.get(0);
            assertThat(orderSummary.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(orderSummary.orderStatus()).isEqualTo(OrderStatus.READY_FOR_SHIPMENT);
            assertThat(orderSummary.orderDate()).isEqualTo(testDateTime);
            assertThat(orderSummary.buyerName()).isEqualTo("테스트 판매자");

            // 주문 상품 정보 검증
            List<SellerOrderListResponse.SellerOrderItem> orderItems = orderSummary.orderItems();
            assertThat(orderItems).hasSize(2);

            // 주문 요약 정보 검증
            SellerOrderListResponse.OrderSummaryInfo summaryInfo = orderSummary.orderSummary();
            assertThat(summaryInfo.itemCount()).isEqualTo(2);
            assertThat(summaryInfo.totalAmount()).isEqualTo(65000L);

            // 배송 기본 정보 검증
            SellerOrderListResponse.ShipmentBasicInfo shipmentInfo = orderSummary.shipmentInfo();
            assertThat(shipmentInfo.isShipped()).isFalse();

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellerRepository).findByUserId("user123");
            verify(shipmentRepository).findSellerOrdersWithPaging("user123", testPageable);
        }

        @Test
        @DisplayName("✅ 빈 주문 목록 조회")
        void getSellerOrders_EmptyResult_Success() {
            // given
            Page<Shipments> emptyPage = new PageImpl<>(List.of(), testPageable, 0);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("user123", testPageable))
                    .willReturn(emptyPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            assertThat(response.orders()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0L);
            assertThat(response.totalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("✅ 여러 페이지 주문 목록 조회")
        void getSellerOrders_MultiplePages_Success() {
            // given
            List<Shipments> shipmentsList = Arrays.asList(testShipment);
            Page<Shipments> shipmentsPage = new PageImpl<>(shipmentsList, testPageable, 25); // 총 25개 주문, 페이지당 10개

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("user123", testPageable))
                    .willReturn(shipmentsPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.totalElements()).isEqualTo(25L);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("❌ 존재하지 않는 사용자")
        void getSellerOrders_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrders(principal, testPageable)
            ).isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("❌ 존재하지 않는 판매자")
        void getSellerOrders_SellerNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrders(principal, testPageable)
            ).isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("판매자 정보를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("페이징 검증 테스트")
    class PagingValidationTests {

        @Test
        @DisplayName("❌ 페이지 크기가 100을 초과하는 경우")
        void getSellerOrders_PageSizeExceeds100_ThrowsException() {
            // given
            Pageable invalidPageable = PageRequest.of(0, 101);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));

            // when & then
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrders(principal, invalidPageable)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("페이지 크기는 100을 초과할 수 없습니다");
        }

        @Test
        @DisplayName("❌ 페이지 번호가 음수인 경우")
        void getSellerOrders_NegativePageNumber_ThrowsException() {
            // given
            Pageable invalidPageable = PageRequest.of(-1, 10);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));

            // when & then
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrders(principal, invalidPageable)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("페이지 번호는 0 이상이어야 합니다");
        }
    }

    @Nested
    @DisplayName("데이터 변환 테스트")
    class DataConversionTests {

        @Test
        @DisplayName("✅ 운송장 정보가 있는 주문 목록")
        void getSellerOrders_WithTrackingInfo_Success() {
            // given
            testShipment.setCourier("CJ대한통운");
            testShipment.setTrackingNumber("123456789012");
            testShipment.setShippedAt(testDateTime.minusDays(1));

            List<Shipments> shipmentsList = Arrays.asList(testShipment);
            Page<Shipments> shipmentsPage = new PageImpl<>(shipmentsList, testPageable, 1);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("user123", testPageable))
                    .willReturn(shipmentsPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            SellerOrderListResponse.SellerOrderSummary orderSummary = response.orders().get(0);
            SellerOrderListResponse.ShipmentBasicInfo shipmentInfo = orderSummary.shipmentInfo();

            assertThat(shipmentInfo.courier()).isEqualTo("CJ대한통운");
            assertThat(shipmentInfo.trackingNumber()).isEqualTo("123456789012");
            assertThat(shipmentInfo.isShipped()).isTrue();
            assertThat(shipmentInfo.shippedAt()).isEqualTo(testDateTime.minusDays(1));
        }

        @Test
        @DisplayName("✅ 마스킹된 구매자 이름 생성")
        void getSellerOrders_MaskedBuyerName_Success() {
            // given
            List<Shipments> shipmentsList = Arrays.asList(testShipment);
            Page<Shipments> shipmentsPage = new PageImpl<>(shipmentsList, testPageable, 1);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("user123", testPageable))
                    .willReturn(shipmentsPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            SellerOrderListResponse.SellerOrderSummary orderSummary = response.orders().get(0);
            assertThat(orderSummary.buyerName()).isEqualTo("테스트 판매자");
            assertThat(orderSummary.maskedBuyerName()).contains("*");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTests {

        @Test
        @DisplayName("✅ 혼합 주문에서 판매자 상품만 필터링")
        void getSellerOrderDetail_MixedOrder_FilterSellerProducts() {
            // given
            // 혼합 주문 생성 (테스트 판매자 상품 + 다른 판매자 상품)
            testOrder.setOrderItems(Arrays.asList(testOrderItem1, testOrderItem2, otherOrderItem));

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            // 테스트 판매자의 상품만 포함되어야 함 (2개)
            assertThat(response.orderItems()).hasSize(2);
            assertThat(response.orderItems())
                    .extracting(SellerOrderDetailResponse.SellerOrderDetailItem::productName)
                    .containsExactly("테스트 상품1", "테스트 상품2");

            // 다른 판매자 상품은 제외되어야 함
            assertThat(response.orderItems())
                    .extracting(SellerOrderDetailResponse.SellerOrderDetailItem::productName)
                    .doesNotContain("다른 판매자 상품");

            // 주문 요약은 테스트 판매자 상품의 합계만 포함
            assertThat(response.orderSummary().totalAmount()).isEqualTo(65000L); // 50000 + 15000
        }

        @Test
        @DisplayName("✅ 배송지 정보 포맷팅")
        void getSellerOrderDetail_AddressFormatting_Success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            SellerOrderDetailResponse.ShippingAddress shippingAddress = response.shippingAddress();
            assertThat(shippingAddress.zipCode()).isEqualTo("12345");
            assertThat(shippingAddress.address()).isEqualTo("서울시 강남구 테헤란로 123");
            assertThat(shippingAddress.addressDetail()).isEqualTo("456호");
            assertThat(shippingAddress.fullAddress()).isEqualTo("서울시 강남구 테헤란로 123 456호");
        }
    }
}