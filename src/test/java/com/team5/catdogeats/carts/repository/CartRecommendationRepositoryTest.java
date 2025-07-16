package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CartRecommendationRepository 테스트 (하이브리드)")
class CartRecommendationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRecommendationRepository cartRecommendationRepository;

    private Buyers testBuyer;
    private Users testUser;
    private Users testSeller;
    private Buyers buyer;
    private Sellers seller;
    private Products dogProduct1;
    private Products dogProduct2;
    private Products dogProduct3;
    private Products dogProduct4;
    private Products dogProduct5;
    private Products catProduct1;
    private Products catProduct2;
    private Products catProduct3;
    private Orders completedOrder1;
    private Orders completedOrder2;
    private Orders pendingOrder;
    private Orders cancelledOrder;

    @BeforeEach
    void setUp() {
        // 테스트용 구매자 생성
        testUser = Users.builder()
                .provider("google")
                .providerId("test-buyer-id")
                .userNameAttribute("sub")
                .name("테스트 구매자")
                .role(Role.ROLE_BUYER)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // 테스트용 판매자 생성
        testSeller = Users.builder()
                .provider("google")
                .providerId("test-seller-id")
                .userNameAttribute("sub")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .build();
        testSeller = entityManager.persistAndFlush(testSeller);

        // 구매자 정보 생성
        buyer = Buyers.builder()
                .userId(testUser.getId())
                .user(testUser)
                .nameMaskingStatus(false)
                .build();
        entityManager.persistAndFlush(buyer);

        // 판매자 정보 생성
        seller = Sellers.builder()
                .userId(testSeller.getId())
                .user(testSeller)
                .vendorName("테스트 상점")
                .build();
        entityManager.persistAndFlush(seller);

        // DOG 카테고리 상품들 생성 (더 체계적으로)
        dogProduct1 = createProduct(1001L, "강아지 간식 1", PetCategory.DOG, 10000L);
        dogProduct2 = createProduct(1002L, "강아지 간식 2", PetCategory.DOG, 15000L);
        dogProduct3 = createProduct(1003L, "강아지 간식 3", PetCategory.DOG, 12000L);
        dogProduct4 = createProduct(1004L, "강아지 간식 4", PetCategory.DOG, 8000L);
        dogProduct5 = createProduct(1005L, "강아지 간식 5", PetCategory.DOG, 20000L);

        // CAT 카테고리 상품들 생성
        catProduct1 = createProduct(2001L, "고양이 간식 1", PetCategory.CAT, 12000L);
        catProduct2 = createProduct(2002L, "고양이 간식 2", PetCategory.CAT, 18000L);
        catProduct3 = createProduct(2003L, "고양이 간식 3", PetCategory.CAT, 16000L);

        // 주문들 생성
        completedOrder1 = createOrder(20241201001L, OrderStatus.DELIVERED, 25000L);
        completedOrder2 = createOrder(20241201002L, OrderStatus.PAYMENT_COMPLETED, 30000L);
        pendingOrder = createOrder(20241201003L, OrderStatus.PAYMENT_PENDING, 15000L);
        cancelledOrder = createOrder(20241201004L, OrderStatus.CANCELLED, 20000L);

        // 주문 아이템들 생성 (인기도 차별화)
        // dogProduct1: 5번 주문 (가장 인기)
        createOrderItem(completedOrder1, dogProduct1, 2);
        createOrderItem(completedOrder2, dogProduct1, 3);

        // dogProduct2: 3번 주문
        createOrderItem(completedOrder1, dogProduct2, 1);
        createOrderItem(completedOrder2, dogProduct2, 2);

        // dogProduct3: 2번 주문
        createOrderItem(completedOrder2, dogProduct3, 2);

        // dogProduct4: 1번 주문
        createOrderItem(completedOrder1, dogProduct4, 1);

        // dogProduct5: 주문 없음

        // CAT 상품들
        createOrderItem(completedOrder1, catProduct1, 1);
        createOrderItem(completedOrder2, catProduct2, 1);

        // 제외되어야 하는 주문들
        createOrderItem(pendingOrder, dogProduct1, 1);
        createOrderItem(cancelledOrder, catProduct3, 1);

        entityManager.flush();
        entityManager.clear();
    }

    private Products createProduct(Long productNumber, String title, PetCategory petCategory, Long price) {
        Products product = Products.builder()
                .productNumber(productNumber)
                .seller(seller)
                .title(title)
                .subTitle(title + " 서브타이틀")  // 추가 필요
                .productInfo(title + " 상품 정보")  // 추가 필요
                .contents(title + " 설명")
                .petCategory(petCategory)
                .productCategory(ProductCategory.HANDMADE)
                .discounted(false)
                .price(price)
                .leadTime((short) 3)  // 추가 필요
                .stock(100)  // 추가 필요
                .leadTime((short) 3)
                .stock(100)
                .build();

        return entityManager.persistAndFlush(product);
    }

    private Orders createOrder(Long orderNumber, OrderStatus status, Long totalPrice) {
        Orders order = Orders.builder()
                .orderNumber(String.valueOf(orderNumber)) // Long을 String으로 변환
                .buyers(buyer)
                .subtotalPrice(totalPrice)
                .totalDeliveryFee(1000L)
                .totalDiscountAmount(100L)
                .discountedTotalPrice(totalPrice)
                .orderStatus(status)
                .build();
        return entityManager.persistAndFlush(order);
    }

    private void createOrderItem(Orders order, Products product, int quantity) {
        OrderItems orderItem = OrderItems.builder()
                .orders(order)
                .products(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();
        entityManager.persistAndFlush(orderItem);
    }

    // ✅ 새로운 최적화된 메서드 테스트 (핵심 테스트)
    @Nested
    @DisplayName("최적화된 메서드 테스트 (DB 레벨 LIMIT)")
    class OptimizedMethodsTest {

        @Test
        @DisplayName("카테고리별 인기 상품 조회 - LIMIT 적용")
        void findTopPopularProductsByCategory_WithLimit_Success() {
            // given
            int limit = 3;

            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsByCategory(
                    PetCategory.DOG.name(), limit);

            // then
            assertThat(result).hasSize(3); // 정확히 3개만 반환

            // 인기도 순서 확인
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId()); // 5번 주문
            assertThat(result.get(1).getId()).isEqualTo(dogProduct2.getId()); // 3번 주문
            assertThat(result.get(2).getId()).isEqualTo(dogProduct3.getId()); // 2번 주문

            // 모두 DOG 카테고리인지 확인
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.DOG);
        }

        @Test
        @DisplayName("카테고리별 인기 상품 조회 - 제외 상품 있을 때")
        void findTopPopularProductsByCategoryExcluding_WithLimit_Success() {
            // given
            List<String> excludeIds = Arrays.asList(dogProduct1.getId());
            int limit = 2;

            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), excludeIds, limit);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId()) // 제외됨
                    .contains(dogProduct2.getId(), dogProduct3.getId()); // 포함됨
        }

        @Test
        @DisplayName("전체 인기 상품 조회 - LIMIT 적용")
        void findTopPopularProductsAll_WithLimit_Success() {
            // given
            int limit = 4;

            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsAll(limit);

            // then
            assertThat(result).hasSize(4);

            // 전체 중 가장 인기 있는 상품이 첫 번째여야 함
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId());
        }

        @Test
        @DisplayName("전체 인기 상품 조회 - 제외 상품 있을 때")
        void findTopPopularProductsExcluding_WithLimit_Success() {
            // given
            List<String> excludeIds = Arrays.asList(dogProduct1.getId(), dogProduct2.getId());
            int limit = 3;

            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsExcluding(excludeIds, limit);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId(), dogProduct2.getId());
        }

        @Test
        @DisplayName("LIMIT 경계값 테스트")
        void limitBoundaryTest() {
            // LIMIT 0
            List<Products> result0 = cartRecommendationRepository.findTopPopularProductsAll(0);
            assertThat(result0).isEmpty();

            // LIMIT 1
            List<Products> result1 = cartRecommendationRepository.findTopPopularProductsAll(1);
            assertThat(result1).hasSize(1);
            assertThat(result1.get(0).getId()).isEqualTo(dogProduct1.getId());

            // LIMIT 큰 수
            List<Products> result100 = cartRecommendationRepository.findTopPopularProductsAll(100);
            assertThat(result100.size()).isLessThanOrEqualTo(8); // 총 상품 수
        }
    }

    // ✅ 기존 vs 새로운 방식 비교 테스트 → 새로운 방식끼리 비교
    @Nested
    @DisplayName("새로운 방식 다양한 시나리오 테스트")
    class OptimizedMethodsVariousScenarios {

        @Test
        @DisplayName("결과 일치성 검증 - 전체 상품 (두 번 호출 비교)")
        void resultConsistency_AllProducts() {
            // given
            int limit = 4;

            // when - 첫 번째 호출
            List<Products> firstResult = cartRecommendationRepository.findTopPopularProductsAll(limit);

            // when - 두 번째 호출 (동일한 결과인지 확인)
            List<Products> secondResult = cartRecommendationRepository.findTopPopularProductsAll(limit);

            // then
            assertThat(firstResult).hasSize(limit);
            assertThat(secondResult).hasSize(limit);
            assertThat(firstResult).extracting(Products::getId)
                    .isEqualTo(secondResult.stream().map(Products::getId).collect(Collectors.toList()));
        }

        @Test
        @DisplayName("결과 일치성 검증 - 카테고리별 (서로 다른 LIMIT)")
        void resultConsistency_ByCategory() {
            // given
            int smallLimit = 2;
            int largeLimit = 4;

            // when - 작은 LIMIT으로 조회
            List<Products> smallResult = cartRecommendationRepository.findTopPopularProductsByCategory(
                    PetCategory.DOG.name(), smallLimit);

            // when - 큰 LIMIT으로 조회
            List<Products> largeResult = cartRecommendationRepository.findTopPopularProductsByCategory(
                    PetCategory.DOG.name(), largeLimit);

            // then - 작은 결과가 큰 결과의 부분집합이어야 함
            assertThat(smallResult).hasSize(smallLimit);
            assertThat(largeResult.size()).isGreaterThanOrEqualTo(smallLimit);

            // 첫 번째 2개 요소는 동일해야 함
            for (int i = 0; i < smallLimit; i++) {
                assertThat(smallResult.get(i).getId()).isEqualTo(largeResult.get(i).getId());
            }
        }
    }

    // ✅ 기존 메서드 동작 확인 - 새로운 메서드로 교체
    @Nested
    @DisplayName("새로운 메서드 기본 동작 확인")
    class NewMethodsBasicTest {

        @Test
        @DisplayName("전체 인기 상품 조회 기본 동작")
        void findTopPopularProductsAll_BasicOperation() {
            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsAll(10);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId()); // 가장 인기 상품
        }

        @Test
        @DisplayName("카테고리별 인기 상품 조회 기본 동작")
        void findTopPopularProductsByCategory_BasicOperation() {
            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsByCategory(
                    PetCategory.DOG.name(), 10);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.DOG);
        }

        @Test
        @DisplayName("제외 상품이 있는 전체 조회 기본 동작")
        void findTopPopularProductsExcluding_BasicOperation() {
            // given
            List<String> excludeIds = Arrays.asList(dogProduct1.getId());

            // when
            List<Products> result = cartRecommendationRepository.findTopPopularProductsExcluding(excludeIds, 10);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId());
        }
    }

    // ✅ 실제 서비스 사용 시나리오
    @Nested
    @DisplayName("실제 서비스 시나리오")
    class ServiceScenarios {

        @Test
        @DisplayName("장바구니 추천 시나리오 - 4개 추천")
        void cartRecommendation_DefaultScenario() {
            // given
            List<String> cartItems = Arrays.asList(dogProduct1.getId(), catProduct1.getId());

            // when
            List<Products> recommendations = cartRecommendationRepository.findTopPopularProductsExcluding(cartItems, 4);

            // then
            assertThat(recommendations).hasSize(4);
            assertThat(recommendations).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId(), catProduct1.getId());
        }

        @Test
        @DisplayName("카테고리 특화 추천 시나리오")
        void categorySpecificRecommendation() {
            // given - 강아지 전용 장바구니
            List<String> dogCartItems = Arrays.asList(dogProduct1.getId());

            // when
            List<Products> dogRecommendations = cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), dogCartItems, 4);

            // then
            assertThat(dogRecommendations).hasSize(4);
            assertThat(dogRecommendations).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.DOG);
            assertThat(dogRecommendations).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId());
        }
    }
}