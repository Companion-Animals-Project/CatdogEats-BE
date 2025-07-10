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

    @BeforeEach
    void setUp() {
        // UserPrincipal 초기화
        principal = new UserPrincipal("google", "google123");
        testOrderNumber = "ORDER-2025-001";
        testDateTime = ZonedDateTime.now();

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

        // 다른 판매자 생성
        otherSeller = Sellers.builder()
                .userId("otherSeller123")
                .vendorName("다른 판매자")
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

        otherProduct = Products.builder()
                .id("otherProduct")
                .title("다른 판매자 상품")
                .price(30000L)
                .seller(otherSeller)
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

        otherOrderItem = OrderItems.builder()
                .id("otherOrderItem")
                .products(otherProduct)
                .quantity(1)
                .price(30000L)
                .build();

        // 테스트 주문 생성 (여러 판매자의 상품 포함)
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(testOrderNumber)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(95000L) // 모든 상품 합계
                .orderItems(Arrays.asList(testOrderItem1, testOrderItem2, otherOrderItem))
                .createdAt(testDateTime)
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
                .createdAt(testDateTime)
                .build();
    }

    @Nested
    @DisplayName("판매자용 주문 상세 조회 테스트")
    class GetSellerOrderDetailTests {

        @Test
        @DisplayName("✅ 정상적인 주문 상세 조회 성공 - 해당 판매자 상품만 필터링")
        void getSellerOrderDetail_Success_OnlySellerProducts() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(response.getOrderDate()).isEqualTo(testDateTime);

            // 해당 판매자의 상품만 조회되어야 함 (다른 판매자 상품 제외)
            List<SellerOrderDetailResponse.SellerOrderDetailItem> orderItems = response.getOrderItems();
            assertThat(orderItems).hasSize(2); // testSeller의 상품만 2개

            List<String> productIds = orderItems.stream()
                    .map(SellerOrderDetailResponse.SellerOrderDetailItem::getProductId)
                    .toList();
            assertThat(productIds).containsExactlyInAnyOrder("product1", "product2");
            assertThat(productIds).doesNotContain("otherProduct"); // 다른 판매자 상품 제외

            // 총 금액도 해당 판매자 상품만 반영되어야 함
            SellerOrderDetailResponse.OrderSummary orderSummary = response.getOrderSummary();
            assertThat(orderSummary.getTotalAmount()).isEqualTo(65000L); // 25000*2 + 15000*1

            // 배송지 정보 검증
            SellerOrderDetailResponse.ShippingAddress shippingAddress = response.getShippingAddress();
            assertThat(shippingAddress.getRecipientName()).isEqualTo("김철수");
            assertThat(shippingAddress.getRecipientPhone()).isEqualTo("010-1234-5678");
            assertThat(shippingAddress.getShippingAddress()).isEqualTo("서울시 강남구 테헤란로 123");
            assertThat(shippingAddress.getDeliveryNote()).isEqualTo("문 앞에 놓아주세요");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellerRepository).findByUserId("user123");
            verify(shipmentRepository).findByOrdersOrderNumber(testOrderNumber);
        }

        @Test
        @DisplayName("✅ 배송 요청사항이 null인 경우 기본값 설정")
        void getSellerOrderDetail_NullDeliveryNote_DefaultValue() {
            // given
            testShipment.setDeliveryNote(null);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            SellerOrderDetailResponse.ShippingAddress shippingAddress = response.getShippingAddress();
            assertThat(shippingAddress.getDeliveryNote()).isEqualTo("배송 요청사항 없음");
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
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            SellerOrderDetailResponse.ShipmentInfo shipmentInfo = response.getShipmentInfo();
            assertThat(shipmentInfo.getCourier()).isEqualTo("CJ대한통운");
            assertThat(shipmentInfo.getTrackingNumber()).isEqualTo("123456789012");
            assertThat(shipmentInfo.getShippedAt()).isEqualTo(testDateTime.minusDays(1));
            assertThat(shipmentInfo.isShipped()).isTrue();
        }

        @Test
        @DisplayName("❌ 존재하지 않는 사용자로 조회 시 예외 발생")
        void getSellerOrderDetail_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");

            verify(sellerRepository).findByUserId("user123"); // userId로는 조회하지 않아야 함
        }

        @Test
        @DisplayName("❌ 판매자가 아닌 사용자로 조회 시 예외 발생")
        void getSellerOrderDetail_NotSeller_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("판매자 정보를 찾을 수 없습니다");

            verify(shipmentRepository).findByOrdersOrderNumber(testOrderNumber); // 주문 조회하지 않아야 함
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문으로 조회 시 예외 발생")
        void getSellerOrderDetail_OrderNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("❌ 해당 판매자의 상품이 포함되지 않은 주문 조회 시 예외 발생")
        void getSellerOrderDetail_NoSellerProducts_ThrowsException() {
            // given - 다른 판매자의 상품만 있는 주문
            Orders otherSellerOrder = Orders.builder()
                    .id("otherOrder")
                    .orderNumber(testOrderNumber)
                    .orderItems(Arrays.asList(otherOrderItem)) // 다른 판매자 상품만
                    .build();

            Shipments otherShipment = Shipments.builder()
                    .id("otherShipment")
                    .orders(otherSellerOrder)
                    .build();

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(otherShipment));

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 주문에 대한 접근 권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("판매자용 주문 목록 조회 테스트")
    class GetSellerOrdersTests {

        private Pageable testPageable;
        private Shipments testShipment1;
        private Shipments testShipment2;
        private Orders testOrder1;
        private Orders testOrder2;

        @BeforeEach
        void setUpOrderList() {
            testPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

            // 첫 번째 주문
            testOrder1 = Orders.builder()
                    .id("order1")
                    .orderNumber("ORDER-2025-001")
                    .user(testUser)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                    .totalPrice(50000L)
                    .orderItems(Arrays.asList(testOrderItem1))
                    .createdAt(testDateTime)
                    .isHidden(false)
                    .build();

            testShipment1 = Shipments.builder()
                    .id("shipment1")
                    .orders(testOrder1)
                    .recipientName("김철수")
                    .recipientPhone("010-1234-5678")
                    .postalCode("06234")
                    .shippingAddress("서울시 강남구 테헤란로 123")
                    .deliveryNote("문 앞에 놓아주세요")
                    .createdAt(testDateTime)
                    .build();

            // 두 번째 주문
            testOrder2 = Orders.builder()
                    .id("order2")
                    .orderNumber("ORDER-2025-002")
                    .user(testUser)
                    .orderStatus(OrderStatus.PREPARING)
                    .totalPrice(30000L)
                    .orderItems(Arrays.asList(testOrderItem2))
                    .createdAt(testDateTime.minusDays(1))
                    .isHidden(false)
                    .build();

            testShipment2 = Shipments.builder()
                    .id("shipment2")
                    .orders(testOrder2)
                    .recipientName("이영희")
                    .recipientPhone("010-5678-9012")
                    .postalCode("12345")
                    .shippingAddress("부산시 해운대구 센텀로 456")
                    .deliveryNote(null)
                    .createdAt(testDateTime.minusDays(1))
                    .build();
        }

        @Test
        @DisplayName("✅ 정상적인 주문 목록 조회 성공")
        void getSellerOrders_Success() {
            // given
            List<Shipments> shipmentList = Arrays.asList(testShipment1, testShipment2);
            Page<Shipments> shipmentPage = new PageImpl<>(shipmentList, testPageable, 2L);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("seller123", testPageable))
                    .willReturn(shipmentPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOrders()).hasSize(2);
            assertThat(response.getTotalElements()).isEqualTo(2L);
            assertThat(response.getCurrentPage()).isEqualTo(0);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.isHasNext()).isFalse();

            // 첫 번째 주문 검증
            SellerOrderListResponse.SellerOrderSummary firstOrder = response.getOrders().get(0);
            assertThat(firstOrder.getOrderNumber()).isEqualTo("ORDER-2025-001");
            assertThat(firstOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(firstOrder.getRecipientName()).isEqualTo("김철수");
            assertThat(firstOrder.getMaskedPhone()).isEqualTo("010****5678"); // 마스킹 처리
            assertThat(firstOrder.getOrderDate()).isEqualTo(testDateTime);

            // 주문 요약 정보 검증
            SellerOrderListResponse.OrderSummaryInfo orderSummary1 = firstOrder.getOrderSummary();
            assertThat(orderSummary1.getItemCount()).isEqualTo(1);
            assertThat(orderSummary1.getTotalAmount()).isEqualTo(50000L);

            // 두 번째 주문 검증
            SellerOrderListResponse.SellerOrderSummary secondOrder = response.getOrders().get(1);
            assertThat(secondOrder.getOrderNumber()).isEqualTo("ORDER-2025-002");
            assertThat(secondOrder.getOrderStatus()).isEqualTo(OrderStatus.PREPARING);
            assertThat(secondOrder.getRecipientName()).isEqualTo("이영희");
            assertThat(secondOrder.getMaskedPhone()).isEqualTo("010****9012");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellerRepository).findByUserId("user123");
            verify(shipmentRepository).findSellerOrdersWithPaging("seller123", testPageable);
        }

        @Test
        @DisplayName("✅ 빈 주문 목록 조회 성공")
        void getSellerOrders_EmptyList_Success() {
            // given
            Page<Shipments> emptyPage = new PageImpl<>(List.of(), testPageable, 0L);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("seller123", testPageable))
                    .willReturn(emptyPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOrders()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0L);
            assertThat(response.getCurrentPage()).isEqualTo(0);
            assertThat(response.getTotalPages()).isEqualTo(0);
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("✅ 운송장 정보가 있는 주문 목록 조회")
        void getSellerOrders_WithTrackingInfo_Success() {
            // given
            testShipment1.setCourier("CJ대한통운");
            testShipment1.setTrackingNumber("123456789012");
            testShipment1.setShippedAt(testDateTime.minusHours(2));

            List<Shipments> shipmentList = Arrays.asList(testShipment1);
            Page<Shipments> shipmentPage = new PageImpl<>(shipmentList, testPageable, 1L);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("seller123", testPageable))
                    .willReturn(shipmentPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, testPageable);

            // then
            SellerOrderListResponse.SellerOrderSummary order = response.getOrders().get(0);
            SellerOrderListResponse.ShipmentBasicInfo shipmentInfo = order.getShipmentInfo();

            assertThat(shipmentInfo.getCourier()).isEqualTo("CJ대한통운");
            assertThat(shipmentInfo.getTrackingNumber()).isEqualTo("123456789012");
            assertThat(shipmentInfo.isShipped()).isTrue();
            assertThat(shipmentInfo.getShippedAt()).isEqualTo(testDateTime.minusHours(2));
        }

        @Test
        @DisplayName("❌ 잘못된 페이지 크기로 조회 시 예외 발생")
        void getSellerOrders_InvalidPageSize_ThrowsException() {
            // given
            Pageable invalidPageable = PageRequest.of(0, 101); // 100 초과

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrders(principal, invalidPageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("페이지 크기는 100을 초과할 수 없습니다");

            verify(shipmentRepository).findSellerOrdersWithPaging("seller123", invalidPageable); // 호출되지 않아야 함
        }

        @Test
        @DisplayName("❌ 음수 페이지 번호로 조회 시 예외 발생")
        void getSellerOrders_NegativePageNumber_ThrowsException() {
            // given
            Pageable invalidPageable = PageRequest.of(-1, 20);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrders(principal, invalidPageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("페이지 번호는 0 이상이어야 합니다");
        }

        @Test
        @DisplayName("❌ 존재하지 않는 사용자로 목록 조회 시 예외 발생")
        void getSellerOrders_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrders(principal, testPageable))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");

            verify(sellerRepository).findByUserId("user123"); // 호출되지 않아야 함
            verify(shipmentRepository).findSellerOrdersWithPaging("seller123", testPageable); // 호출되지 않아야 함
        }

        @Test
        @DisplayName("❌ 판매자가 아닌 사용자로 목록 조회 시 예외 발생")
        void getSellerOrders_NotSeller_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrders(principal, testPageable))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("판매자 정보를 찾을 수 없습니다");

            verify(shipmentRepository).findSellerOrdersWithPaging("seller123", testPageable); // 호출되지 않아야 함
        }
    }

    @Nested
    @DisplayName("데이터 변환 및 유틸리티 테스트")
    class DataTransformationTests {

        @Test
        @DisplayName("✅ 전화번호 마스킹 처리 정확성 검증")
        void phoneNumberMasking_CorrectFormat() {
            // given
            List<Shipments> shipmentList = Arrays.asList(testShipment);
            Page<Shipments> shipmentPage = new PageImpl<>(shipmentList, PageRequest.of(0, 20), 1L);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("seller123", PageRequest.of(0, 20)))
                    .willReturn(shipmentPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, PageRequest.of(0, 20));

            // then
            String maskedPhone = response.getOrders().get(0).getMaskedPhone();
            assertThat(maskedPhone).matches("^010\\*{4}\\d{4}$"); // 010****숫자4자리 패턴
            assertThat(maskedPhone).isEqualTo("010****5678");
        }

        @Test
        @DisplayName("✅ 짧은 전화번호의 마스킹 처리")
        void phoneNumberMasking_ShortNumber() {
            // given
            testShipment.setRecipientPhone("123");
            List<Shipments> shipmentList = Arrays.asList(testShipment);
            Page<Shipments> shipmentPage = new PageImpl<>(shipmentList, PageRequest.of(0, 20), 1L);

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findSellerOrdersWithPaging("seller123", PageRequest.of(0, 20)))
                    .willReturn(shipmentPage);

            // when
            SellerOrderListResponse response = sellerOrderQueryService.getSellerOrders(principal, PageRequest.of(0, 20));

            // then
            String maskedPhone = response.getOrders().get(0).getMaskedPhone();
            assertThat(maskedPhone).isEqualTo("****");
        }

        @Test
        @DisplayName("✅ 판매자 상품 필터링 정확성 검증")
        void sellerProductFiltering_Accuracy() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            // 총 3개 상품 중 해당 판매자 상품 2개만 필터링되어야 함
            assertThat(response.getOrderItems()).hasSize(2);

            List<String> productIds = response.getOrderItems().stream()
                    .map(SellerOrderDetailResponse.SellerOrderDetailItem::getProductId)
                    .toList();

            // 해당 판매자 상품만 포함
            assertThat(productIds).containsExactlyInAnyOrder("product1", "product2");
            // 다른 판매자 상품 제외
            assertThat(productIds).doesNotContain("otherProduct");

            // 금액도 해당 판매자 상품만 계산되어야 함
            Long totalAmount = response.getOrderSummary().getTotalAmount();
            assertThat(totalAmount).isEqualTo(65000L); // 25000*2 + 15000*1
            assertThat(totalAmount).isNotEqualTo(95000L); // 전체 주문 금액이 아님
        }

        @Test
        @DisplayName("✅ 주문 상품별 총 가격 계산 정확성")
        void orderItemTotalPriceCalculation_Accuracy() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellerRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrdersOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderQueryService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            List<SellerOrderDetailResponse.SellerOrderDetailItem> orderItems = response.getOrderItems();

            // 첫 번째 상품 (product1: 25000원 * 2개)
            SellerOrderDetailResponse.SellerOrderDetailItem item1 = orderItems.stream()
                    .filter(item -> "product1".equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(item1.getUnitPrice()).isEqualTo(25000L);
            assertThat(item1.getQuantity()).isEqualTo(2);
            assertThat(item1.getTotalPrice()).isEqualTo(50000L);

            // 두 번째 상품 (product2: 15000원 * 1개)
            SellerOrderDetailResponse.SellerOrderDetailItem item2 = orderItems.stream()
                    .filter(item -> "product2".equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(item2.getUnitPrice()).isEqualTo(15000L);
            assertThat(item2.getQuantity()).isEqualTo(1);
            assertThat(item2.getTotalPrice()).isEqualTo(15000L);
        }
    }
}