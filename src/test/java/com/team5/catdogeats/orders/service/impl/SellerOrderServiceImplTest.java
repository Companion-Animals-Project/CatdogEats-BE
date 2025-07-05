package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerOrderService 통합 테스트")
class SellerOrderServiceImplTest {

    @InjectMocks
    private SellerOrderServiceImpl sellerOrderService;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellersRepository sellersRepository;

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
    @DisplayName("판매자용 주문 상세 조회 성공 테스트")
    class GetSellerOrderDetailSuccessTests {

        @Test
        @DisplayName("✅ 정상적인 주문 상세 조회 성공")
        void getSellerOrderDetail_Success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderNumber()).isEqualTo(testOrderNumber);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
            assertThat(response.orderDate()).isNotNull(); // BaseEntity에서 자동 설정되므로 null이 아닌지만 확인
            assertThat(response.recipientInfo()).isNotNull();
            assertThat(response.recipientInfo().recipientName()).isEqualTo("김철수");
            assertThat(response.recipientInfo().recipientPhone()).isEqualTo("010-1234-5678");
            assertThat(response.recipientInfo().postalCode()).isEqualTo("06234");
            assertThat(response.recipientInfo().streetAddress()).isEqualTo("서울시 강남구 테헤란로 123");
            assertThat(response.recipientInfo().detailAddress()).isEqualTo("456호");
            assertThat(response.recipientInfo().deliveryNote()).isEqualTo("문 앞에 놓아주세요");

            assertThat(response.orderItems()).hasSize(2);
            assertThat(response.totalAmount()).isEqualTo(65000L);

            // 첫 번째 상품 확인
            SellerOrderDetailResponse.SellerOrderItem firstItem = response.orderItems().get(0);
            assertThat(firstItem.productId()).isEqualTo("product1");
            assertThat(firstItem.productName()).isEqualTo("프리미엄 강아지 사료");
            assertThat(firstItem.quantity()).isEqualTo(2);
            assertThat(firstItem.unitPrice()).isEqualTo(25000L);
            assertThat(firstItem.itemTotalPrice()).isEqualTo(50000L);

            // 두 번째 상품 확인
            SellerOrderDetailResponse.SellerOrderItem secondItem = response.orderItems().get(1);
            assertThat(secondItem.productId()).isEqualTo("product2");
            assertThat(secondItem.productName()).isEqualTo("고양이 간식");
            assertThat(secondItem.quantity()).isEqualTo(1);
            assertThat(secondItem.unitPrice()).isEqualTo(15000L);
            assertThat(secondItem.itemTotalPrice()).isEqualTo(15000L);

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellersRepository).findByUserId("user123");
            verify(shipmentRepository).findByOrderNumber(testOrderNumber);
        }

        @Test
        @DisplayName("✅ 배송 요청사항이 null인 경우 기본값 설정")
        void getSellerOrderDetail_WithNullDeliveryNote_Success() {
            // given
            testShipment = Shipments.builder()
                    .id("shipment123")
                    .orders(testOrder)
                    .recipientName("김철수")
                    .recipientPhone("010-1234-5678")
                    .postalCode("06234")
                    .shippingAddress("서울시 강남구 테헤란로 123")
                    .detailAddress("456호")
                    .deliveryNote(null) // null로 설정
                    .build();

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(testShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            assertThat(response.recipientInfo().deliveryNote()).isEqualTo("배송 요청사항 없음");
        }
    }

    @Nested
    @DisplayName("판매자용 주문 상세 조회 실패 테스트")
    class GetSellerOrderDetailFailureTests {

        @Test
        @DisplayName("❌ 존재하지 않는 사용자로 조회 시 NoSuchElementException 발생")
        void getSellerOrderDetail_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
        }

        @Test
        @DisplayName("❌ 판매자 권한이 없는 사용자로 조회 시 IllegalArgumentException 발생")
        void getSellerOrderDetail_NotSeller_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("판매자 권한이 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellersRepository).findByUserId("user123");
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문 번호로 조회 시 NoSuchElementException 발생")
        void getSellerOrderDetail_OrderNotFound_ThrowsException() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerOrderService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("주문을 찾을 수 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellersRepository).findByUserId("user123");
            verify(shipmentRepository).findByOrderNumber(testOrderNumber);
        }

        @Test
        @DisplayName("❌ 해당 주문에 판매자의 상품이 없는 경우 NoSuchElementException 발생")
        void getSellerOrderDetail_NoSellerProductsInOrder_ThrowsException() {
            // given - 다른 판매자의 상품으로 설정
            Sellers otherSeller = Sellers.builder()
                    .userId("otherSeller123")
                    .vendorName("다른 판매자")
                    .build();

            Products otherProduct = Products.builder()
                    .id("otherProduct")
                    .title("다른 판매자 상품")
                    .price(10000L)
                    .seller(otherSeller)
                    .build();

            OrderItems otherOrderItem = OrderItems.builder()
                    .id("otherOrderItem")
                    .products(otherProduct)
                    .quantity(1)
                    .price(10000L)
                    .build();

            Orders orderWithOtherSellerProduct = Orders.builder()
                    .id("order123")
                    .orderNumber(testOrderNumber)
                    .user(testUser)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                    .totalPrice(10000L)
                    .orderItems(Collections.singletonList(otherOrderItem))
                    .build();

            Shipments shipmentWithOtherProduct = Shipments.builder()
                    .id("shipment123")
                    .orders(orderWithOtherSellerProduct)
                    .recipientName("김철수")
                    .recipientPhone("010-1234-5678")
                    .postalCode("06234")
                    .shippingAddress("서울시 강남구 테헤란로 123")
                    .detailAddress("456호")
                    .deliveryNote("문 앞에 놓아주세요")
                    .build();

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(shipmentWithOtherProduct));

            // when & then
            assertThatThrownBy(() -> sellerOrderService.getSellerOrderDetail(principal, testOrderNumber))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("접근 권한이 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "google123");
            verify(sellersRepository).findByUserId("user123");
            verify(shipmentRepository).findByOrderNumber(testOrderNumber);
        }
    }

    @Nested
    @DisplayName("판매자 권한 및 필터링 테스트")
    class SellerAuthorizationTests {

        @Test
        @DisplayName("✅ 여러 판매자 상품이 있는 주문에서 해당 판매자 상품만 필터링")
        void getSellerOrderDetail_FilterOnlySellerProducts_Success() {
            // given - 다른 판매자 상품도 포함된 주문
            Sellers otherSeller = Sellers.builder()
                    .userId("otherSeller123")
                    .vendorName("다른 판매자")
                    .build();

            Products otherProduct = Products.builder()
                    .id("otherProduct")
                    .title("다른 판매자 상품")
                    .price(30000L)
                    .seller(otherSeller)
                    .build();

            OrderItems otherOrderItem = OrderItems.builder()
                    .id("otherOrderItem")
                    .products(otherProduct)
                    .quantity(1)
                    .price(30000L)
                    .build();

            // 현재 판매자 상품과 다른 판매자 상품이 모두 포함된 주문
            List<OrderItems> mixedOrderItems = Arrays.asList(testOrderItem1, testOrderItem2, otherOrderItem);

            Orders mixedOrder = Orders.builder()
                    .id("order123")
                    .orderNumber(testOrderNumber)
                    .user(testUser)
                    .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                    .totalPrice(95000L)
                    .orderItems(mixedOrderItems)
                    .build();

            Shipments mixedShipment = Shipments.builder()
                    .id("shipment123")
                    .orders(mixedOrder)
                    .recipientName("김철수")
                    .recipientPhone("010-1234-5678")
                    .postalCode("06234")
                    .shippingAddress("서울시 강남구 테헤란로 123")
                    .detailAddress("456호")
                    .deliveryNote("문 앞에 놓아주세요")
                    .build();

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testUser));
            given(sellersRepository.findByUserId("user123"))
                    .willReturn(Optional.of(testSeller));
            given(shipmentRepository.findByOrderNumber(testOrderNumber))
                    .willReturn(Optional.of(mixedShipment));

            // when
            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(principal, testOrderNumber);

            // then
            assertThat(response).isNotNull();
            assertThat(response.orderItems()).hasSize(2); // 다른 판매자 상품은 제외되고 현재 판매자 상품만 2개
            assertThat(response.totalAmount()).isEqualTo(65000L); // 현재 판매자 상품들의 총액만 계산

            // 현재 판매자의 상품들만 포함되었는지 확인
            List<String> productIds = response.orderItems().stream()
                    .map(SellerOrderDetailResponse.SellerOrderItem::productId)
                    .toList();
            assertThat(productIds).containsExactlyInAnyOrder("product1", "product2");
            assertThat(productIds).doesNotContain("otherProduct");
        }
    }
}