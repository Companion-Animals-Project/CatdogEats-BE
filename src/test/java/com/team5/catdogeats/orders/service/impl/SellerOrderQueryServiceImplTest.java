package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
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

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SellerOrderQueryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private SellerOrderQueryServiceImpl sellerOrderQueryService;

    // ===== 공용 테스트 데이터 =====
    private ZonedDateTime now;
    private UserPrincipal principal;
    private Users sellerUser;
    private Users otherUser;
    private Sellers seller;
    private Sellers otherSeller;
    private Products product1;
    private Products product2;
    private Products otherProduct;
    private Orders order;
    private Shipments shipment;
    private Pageable pageable;

    private static final String ORDER_NUMBER = "ORDER-2025-000001";

    @BeforeEach
    void setUp() throws Exception {
        now = ZonedDateTime.now();
        pageable = PageRequest.of(0, 10);

        /* ===== Principal ===== */
        principal = new UserPrincipal("google", "google123");

        /* ===== Users ===== */
        sellerUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .userNameAttribute("sub")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .build();

        otherUser = Users.builder()
                .id("otherUser123")
                .provider("google")
                .providerId("google456")
                .userNameAttribute("sub")
                .name("다른 판매자")
                .role(Role.ROLE_SELLER)
                .build();

        /* ===== Sellers ===== */
        seller = Sellers.builder()
                .userId(sellerUser.getId())
                .user(sellerUser)
                .vendorName("테스트 스토어")
                .businessNumber("123-45-67890")
                .build();

        otherSeller = Sellers.builder()
                .userId(otherUser.getId())
                .user(otherUser)
                .vendorName("다른 스토어")
                .businessNumber("321-54-98765")
                .build();

        /* ===== Products ===== */
        product1 = Products.builder()
                .id("prod1")
                .productNumber(1L)
                .seller(seller)
                .title("테스트 상품1")
                .subTitle("부제목1")
                .productInfo("상품 정보1")
                .contents("내용1")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .price(25_000L)
                .leadTime((short) 1)
                .stock(100)
                .build();

        product2 = Products.builder()
                .id("prod2")
                .productNumber(2L)
                .seller(seller)
                .title("테스트 상품2")
                .subTitle("부제목2")
                .productInfo("상품 정보2")
                .contents("내용2")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .price(15_000L)
                .leadTime((short) 1)
                .stock(100)
                .build();

        otherProduct = Products.builder()
                .id("prod3")
                .productNumber(3L)
                .seller(otherSeller)
                .title("다른 판매자 상품")
                .subTitle("부제목3")
                .productInfo("상품 정보3")
                .contents("내용3")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .price(30_000L)
                .leadTime((short) 1)
                .stock(100)
                .build();

        /* ===== Orders & OrderItems ===== */
        List<OrderItems> items = new ArrayList<>();

        order = Orders.builder()
                .id("order1")
                .orderNumber(ORDER_NUMBER)
                .user(sellerUser)
                .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
                .totalPrice(65_000L)
                .orderItems(items)
                .build();

        OrderItems item1 = OrderItems.builder()
                .id("item1")
                .orders(order)
                .products(product1)
                .quantity(2)
                .price(25_000L)
                .build();

        OrderItems item2 = OrderItems.builder()
                .id("item2")
                .orders(order)
                .products(product2)
                .quantity(1)
                .price(15_000L)
                .build();

        OrderItems item3 = OrderItems.builder()
                .id("item3")
                .orders(order)
                .products(otherProduct)
                .quantity(1)
                .price(30_000L)
                .build();

        items.addAll(List.of(item1, item2, item3));

        /* ===== Shipments ===== */
        shipment = Shipments.builder()
                .id("ship1")
                .orders(order)
                .user(sellerUser)
                .seller(seller)
                .courier("CJ대한통운")
                .trackingNumber("123456789012")
                .shippedAt(now.minusDays(1))
                .deliveredAt(null)
                .recipientName("김철수")
                .recipientPhone("010-1234-5678")
                .postalCode("06250")
                .streetAddress("서울시 강남구 테헤란로 123")
                .detailAddress("456호")
                .deliveryRequest("문 앞에 놓아주세요")
                .build();

        /* ===== createdAt 필드 수동 세팅 ===== */
        setCreatedAt(order, now);
        setCreatedAt(shipment, now);
    }

    // ===== Reflection Utils =====
    private static void setCreatedAt(Object entity, ZonedDateTime dateTime) throws Exception {
        Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(entity, dateTime);
    }

    // ===== 테스트 케이스 =====
    @Nested
    @DisplayName("판매자 주문 상세 조회")
    class GetSellerOrderDetail {

        @Test
        @DisplayName("✅ 정상 조회")
        void success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(sellerUser));
            given(sellersRepository.findByUserId(sellerUser.getId()))
                    .willReturn(Optional.of(seller));
            given(shipmentRepository.findByOrderNumber(ORDER_NUMBER))
                    .willReturn(Optional.of(shipment));

            // when
            SellerOrderDetailResponse response =
                    sellerOrderQueryService.getSellerOrderDetail(principal, ORDER_NUMBER);

            // then
            assertThat(response.orderNumber()).isEqualTo(ORDER_NUMBER);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.READY_FOR_SHIPMENT);
            assertThat(response.orderItems()).hasSize(2);        // otherSeller 상품은 제외
            assertThat(response.orderSummary().totalAmount()).isEqualTo(65_000L);

            SellerOrderDetailResponse.ShippingAddress address = response.shippingAddress();
            assertThat(address.recipientName()).isEqualTo("김철수");
            assertThat(address.recipientPhone()).isEqualTo("010-1234-5678");
            assertThat(address.fullAddress()).isEqualTo("서울시 강남구 테헤란로 123 456호");
            assertThat(address.deliveryRequest()).isEqualTo("문 앞에 놓아주세요");
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문")
        void orderNotFound() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(sellerUser));
            given(sellersRepository.findByUserId(sellerUser.getId()))
                    .willReturn(Optional.of(seller));
            given(shipmentRepository.findByOrderNumber(ORDER_NUMBER))
                    .willReturn(Optional.empty());

            // expect
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrderDetail(principal, ORDER_NUMBER))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("❌ 판매자 권한 없음")
        void unauthorizedSeller() {
            // given : 주문에 seller의 상품이 없음
            Orders otherOrder = Orders.builder()
                    .id("order2")
                    .orderNumber("ORDER-OTHER")
                    .user(otherUser)
                    .orderStatus(OrderStatus.READY_FOR_SHIPMENT)
                    .totalPrice(30_000L)
                    .orderItems(List.of(
                            OrderItems.builder()
                                    .id("item-only-other")
                                    .orders(null)
                                    .products(otherProduct)
                                    .quantity(1)
                                    .price(30_000L)
                                    .build()
                    ))
                    .build();

            Shipments otherShipment = Shipments.builder()
                    .id("ship-other")
                    .orders(otherOrder)
                    .user(otherUser)
                    .seller(otherSeller)
                    .courier("CJ대한통운")
                    .trackingNumber("999999999999")
                    .build();

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(sellerUser));
            given(sellersRepository.findByUserId(sellerUser.getId()))
                    .willReturn(Optional.of(seller));
            given(shipmentRepository.findByOrderNumber("ORDER-OTHER"))
                    .willReturn(Optional.of(otherShipment));

            // expect
            assertThatThrownBy(() ->
                    sellerOrderQueryService.getSellerOrderDetail(principal, "ORDER-OTHER"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("판매자 주문 목록 조회")
    class GetSellerOrders {

        @Test
        @DisplayName("✅ 정상 조회")
        void success() {
            // given
            Page<Shipments> page = new PageImpl<>(List.of(shipment));

            given(userRepository.findByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(sellerUser));
            given(sellersRepository.findByUserId(sellerUser.getId()))
                    .willReturn(Optional.of(seller));
            given(shipmentRepository.findSellerOrdersWithPaging(eq(seller.getUserId()), eq(pageable)))
                    .willReturn(page);

            // when
            SellerOrderListResponse response =
                    sellerOrderQueryService.getSellerOrders(principal, pageable);

            // then
            assertThat(response.orders()).hasSize(1);

            SellerOrderListResponse.SellerOrderSummary summary = response.orders().get(0);
            assertThat(summary.orderNumber()).isEqualTo(ORDER_NUMBER);
            assertThat(summary.orderItems()).hasSize(2);
            assertThat(summary.orderSummary().totalAmount()).isEqualTo(65_000L);
        }
    }
}
