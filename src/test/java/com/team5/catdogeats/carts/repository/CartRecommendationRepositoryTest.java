package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CartRecommendationRepository 테스트")
class  CartRecommendationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRecommendationRepository cartRecommendationRepository;

    private Users testBuyer;
    private Users testSeller;
    private Buyers buyer;
    private Sellers seller;
    private Products dogProduct1;
    private Products dogProduct2;
    private Products dogProduct3;
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
        testBuyer = Users.builder()
                .provider("google")
                .providerId("test-buyer-id")
                .userNameAttribute("sub")
                .name("테스트 구매자")
                .role(Role.ROLE_BUYER)
                .build();
        testBuyer = entityManager.persistAndFlush(testBuyer);

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
                .userId(testBuyer.getId())
                .user(testBuyer)
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

        // DOG 카테고리 상품들 생성
        dogProduct1 = Products.builder()
                .productNumber(1001L)
                .seller(seller)
                .title("강아지 간식 1")
                .contents("강아지용 수제 간식")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.HANDMADE)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(false)
                .price(10000L)
                .leadTime((short) 3)
                .stock(100)
                .build();
        entityManager.persistAndFlush(dogProduct1);

        dogProduct2 = Products.builder()
                .productNumber(1002L)
                .seller(seller)
                .title("강아지 간식 2")
                .contents("강아지용 프리미엄 간식")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(true)
                .discountRate(20.0)
                .price(15000L)
                .leadTime((short) 5)
                .stock(50)
                .build();
        entityManager.persistAndFlush(dogProduct2);

        dogProduct3 = Products.builder()
                .productNumber(1003L)
                .seller(seller)
                .title("강아지 간식 3")
                .contents("강아지용 인기 간식")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.HANDMADE)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(false)
                .price(12000L)
                .leadTime((short) 4)
                .stock(80)
                .build();
        entityManager.persistAndFlush(dogProduct3);

        // CAT 카테고리 상품들 생성
        catProduct1 = Products.builder()
                .productNumber(2001L)
                .seller(seller)
                .title("고양이 간식 1")
                .contents("고양이용 수제 간식")
                .petCategory(PetCategory.CAT)
                .productCategory(ProductCategory.HANDMADE)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(false)
                .price(12000L)
                .leadTime((short) 4)
                .stock(80)
                .build();
        entityManager.persistAndFlush(catProduct1);

        catProduct2 = Products.builder()
                .productNumber(2002L)
                .seller(seller)
                .title("고양이 간식 2")
                .contents("고양이용 프리미엄 간식")
                .petCategory(PetCategory.CAT)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(false)
                .price(18000L)
                .leadTime((short) 6)
                .stock(30)
                .build();
        entityManager.persistAndFlush(catProduct2);

        catProduct3 = Products.builder()
                .productNumber(2003L)
                .seller(seller)
                .title("고양이 간식 3")
                .contents("고양이용 베스트 간식")
                .petCategory(PetCategory.CAT)
                .productCategory(ProductCategory.HANDMADE)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(true)
                .discountRate(10.0)
                .price(16000L)
                .leadTime((short) 5)
                .stock(60)
                .build();
        entityManager.persistAndFlush(catProduct3);

        // 주문들 생성
        completedOrder1 = Orders.builder()
                .orderNumber(20241201001L)
                .user(testBuyer)
                .totalPrice(25000L)
                .orderStatus(OrderStatus.DELIVERED)
                .build();
        entityManager.persistAndFlush(completedOrder1);

        completedOrder2 = Orders.builder()
                .orderNumber(20241201002L)
                .user(testBuyer)
                .totalPrice(30000L)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .build();
        entityManager.persistAndFlush(completedOrder2);

        pendingOrder = Orders.builder()
                .orderNumber(20241201003L)
                .user(testBuyer)
                .totalPrice(15000L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .build();
        entityManager.persistAndFlush(pendingOrder);

        cancelledOrder = Orders.builder()
                .orderNumber(20241201004L)
                .user(testBuyer)
                .totalPrice(20000L)
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        entityManager.persistAndFlush(cancelledOrder);

        // 주문 아이템들 생성 (완료된 주문들)
        // completedOrder1: dogProduct1 * 2, catProduct1 * 1
        OrderItems orderItem1 = OrderItems.builder()
                .orders(completedOrder1)
                .products(dogProduct1)
                .quantity(2)
                .price(10000L)
                .build();
        entityManager.persistAndFlush(orderItem1);

        OrderItems orderItem2 = OrderItems.builder()
                .orders(completedOrder1)
                .products(catProduct1)
                .quantity(1)
                .price(12000L)
                .build();
        entityManager.persistAndFlush(orderItem2);

        // completedOrder2: dogProduct2 * 1, dogProduct3 * 1, catProduct2 * 1
        OrderItems orderItem3 = OrderItems.builder()
                .orders(completedOrder2)
                .products(dogProduct2)
                .quantity(1)
                .price(15000L)
                .build();
        entityManager.persistAndFlush(orderItem3);

        OrderItems orderItem4 = OrderItems.builder()
                .orders(completedOrder2)
                .products(dogProduct3)
                .quantity(1)
                .price(12000L)
                .build();
        entityManager.persistAndFlush(orderItem4);

        OrderItems orderItem5 = OrderItems.builder()
                .orders(completedOrder2)
                .products(catProduct2)
                .quantity(1)
                .price(18000L)
                .build();
        entityManager.persistAndFlush(orderItem5);

        // pendingOrder: dogProduct1 * 1 (결제 대기 상태라 추천에서 제외되어야 함)
        OrderItems orderItem6 = OrderItems.builder()
                .orders(pendingOrder)
                .products(dogProduct1)
                .quantity(1)
                .price(10000L)
                .build();
        entityManager.persistAndFlush(orderItem6);

        // cancelledOrder: catProduct3 * 1 (취소된 주문이라 추천에서 제외되어야 함)
        OrderItems orderItem7 = OrderItems.builder()
                .orders(cancelledOrder)
                .products(catProduct3)
                .quantity(1)
                .price(16000L)
                .build();
        entityManager.persistAndFlush(orderItem7);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("특정 카테고리 인기 상품 조회 (제외 상품 있을 때)")
    class FindPopularProductsByCategoryExcluding {

        @Test
        @DisplayName("성공 - DOG 카테고리, 특정 상품 제외")
        void findPopularProductsByCategoryExcluding_DogCategoryWithExclusions_Success() {
            // given
            List<String> excludeProductIds = Arrays.asList(dogProduct1.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.DOG, excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId()) // 제외된 상품
                    .contains(dogProduct2.getId(), dogProduct3.getId()); // 포함된 상품들
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.DOG);
        }

        @Test
        @DisplayName("성공 - CAT 카테고리, 특정 상품 제외")
        void findPopularProductsByCategoryExcluding_CatCategoryWithExclusions_Success() {
            // given
            List<String> excludeProductIds = Arrays.asList(catProduct2.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.CAT, excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(catProduct2.getId()) // 제외된 상품
                    .contains(catProduct1.getId()); // 포함된 상품
            // catProduct3는 취소된 주문에만 있으므로 결과에 없을 수 있음
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.CAT);
        }

        @Test
        @DisplayName("성공 - 여러 상품 제외")
        void findPopularProductsByCategoryExcluding_MultipleExclusions_Success() {
            // given
            List<String> excludeProductIds = Arrays.asList(dogProduct1.getId(), dogProduct2.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.DOG, excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId(), dogProduct2.getId()) // 제외된 상품들
                    .contains(dogProduct3.getId()); // 포함된 상품
        }

        @Test
        @DisplayName("성공 - 존재하지 않는 상품 ID 제외")
        void findPopularProductsByCategoryExcluding_NonExistentExclusions_Success() {
            // given
            List<String> excludeProductIds = Arrays.asList("nonexistent-product-id");

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.DOG, excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.DOG);
        }

        @Test
        @DisplayName("인기도 순 정렬 확인 - DOG 카테고리")
        void findPopularProductsByCategoryExcluding_OrderByPopularity_Success() {
            // given - dogProduct1이 2번, dogProduct2와 dogProduct3가 각각 1번 주문됨
            List<String> excludeProductIds = Collections.emptyList();

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.DOG, excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            // dogProduct1이 첫 번째로 와야 함 (2번 주문됨)
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId());

            // 나머지 상품들도 포함되어야 함
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct2.getId(), dogProduct3.getId());
        }
    }

    @Nested
    @DisplayName("특정 카테고리 인기 상품 조회 (제외 상품 없을 때)")
    class FindPopularProductsByCategory {

        @Test
        @DisplayName("성공 - DOG 카테고리 전체 조회")
        void findPopularProductsByCategory_DogCategory_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.DOG);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.DOG);
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct1.getId(), dogProduct2.getId(), dogProduct3.getId());
        }

        @Test
        @DisplayName("성공 - CAT 카테고리 전체 조회")
        void findPopularProductsByCategory_CatCategory_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.CAT);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getPetCategory)
                    .allMatch(category -> category == PetCategory.CAT);
            assertThat(result).extracting(Products::getId)
                    .contains(catProduct1.getId(), catProduct2.getId());
        }

        @Test
        @DisplayName("인기도 순 정렬 확인 - DOG 카테고리")
        void findPopularProductsByCategory_OrderByPopularity_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.DOG);

            // then
            assertThat(result).isNotEmpty();
            // dogProduct1이 첫 번째로 와야 함 (2번 주문됨)
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId());
        }

        @Test
        @DisplayName("주문 상태 필터링 확인 - 완료된 주문만 포함")
        void findPopularProductsByCategory_OnlyCompletedOrders_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.CAT);

            // then
            assertThat(result).isNotEmpty();
            // catProduct3는 취소된 주문에만 있으므로 포함되지 않아야 함
            assertThat(result).extracting(Products::getId)
                    .contains(catProduct1.getId(), catProduct2.getId())
                    .doesNotContain(catProduct3.getId());
        }
    }

    @Nested
    @DisplayName("전체 인기 상품 조회 (제외 상품 있을 때)")
    class FindPopularProductsExcluding {

        @Test
        @DisplayName("성공 - 특정 상품들 제외")
        void findPopularProductsExcluding_WithExclusions_Success() {
            // given
            List<String> excludeProductIds = Arrays.asList(dogProduct1.getId(), catProduct1.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsExcluding(excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId(), catProduct1.getId()) // 제외된 상품들
                    .contains(dogProduct2.getId(), dogProduct3.getId(), catProduct2.getId()); // 포함된 상품들
        }

        @Test
        @DisplayName("성공 - DOG, CAT 카테고리 모두 포함")
        void findPopularProductsExcluding_AllCategories_Success() {
            // given
            List<String> excludeProductIds = Arrays.asList(dogProduct1.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsExcluding(excludeProductIds);

            // then
            assertThat(result).isNotEmpty();

            // DOG 카테고리 상품들 확인
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct2.getId(), dogProduct3.getId());

            // CAT 카테고리 상품들 확인
            assertThat(result).extracting(Products::getId)
                    .contains(catProduct1.getId(), catProduct2.getId());

            // 두 카테고리 모두 포함되어야 함
            assertThat(result).extracting(Products::getPetCategory)
                    .containsAnyOf(PetCategory.DOG, PetCategory.CAT);
        }

        @Test
        @DisplayName("성공 - 빈 제외 목록")
        void findPopularProductsExcluding_EmptyExclusions_Success() {
            // given
            List<String> excludeProductIds = Collections.emptyList();

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsExcluding(excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct1.getId(), dogProduct2.getId(), dogProduct3.getId(),
                            catProduct1.getId(), catProduct2.getId());
        }

        @Test
        @DisplayName("인기도 순 정렬 확인 - 전체 카테고리")
        void findPopularProductsExcluding_OrderByPopularity_Success() {
            // given
            List<String> excludeProductIds = Collections.emptyList();

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsExcluding(excludeProductIds);

            // then
            assertThat(result).isNotEmpty();
            // dogProduct1이 가장 인기 있어야 함 (2번 주문됨)
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId());
        }
    }

    @Nested
    @DisplayName("전체 인기 상품 조회 (제외 상품 없을 때)")
    class FindPopularProductsAll {

        @Test
        @DisplayName("성공 - 전체 상품 조회")
        void findPopularProductsAll_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct1.getId(), dogProduct2.getId(), dogProduct3.getId(),
                            catProduct1.getId(), catProduct2.getId());
        }

        @Test
        @DisplayName("카테고리 구분 없이 조회")
        void findPopularProductsAll_AllCategories_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            assertThat(result).isNotEmpty();

            // DOG 카테고리 상품들이 포함되어야 함
            long dogCount = result.stream()
                    .filter(p -> p.getPetCategory() == PetCategory.DOG)
                    .count();
            assertThat(dogCount).isGreaterThan(0);

            // CAT 카테고리 상품들이 포함되어야 함
            long catCount = result.stream()
                    .filter(p -> p.getPetCategory() == PetCategory.CAT)
                    .count();
            assertThat(catCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("인기도 순 정렬 확인")
        void findPopularProductsAll_OrderByPopularity_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            assertThat(result).isNotEmpty();
            // dogProduct1이 첫 번째로 와야 함 (가장 많이 주문됨)
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId());
        }

        @Test
        @DisplayName("주문 상태 필터링 확인")
        void findPopularProductsAll_OrderStatusFiltering_Success() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            assertThat(result).isNotEmpty();
            // 취소된 주문의 상품은 포함되지 않아야 함
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(catProduct3.getId()); // 취소된 주문에만 있는 상품
        }
    }

    @Nested
    @DisplayName("주문 상태별 필터링 테스트")
    class OrderStatusFiltering {

        @Test
        @DisplayName("DELIVERED 상태 주문 포함")
        void orderStatusFiltering_DeliveredIncluded() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            // DELIVERED 상태인 completedOrder1의 상품들이 포함되어야 함
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct1.getId(), catProduct1.getId());
        }

        @Test
        @DisplayName("PAYMENT_COMPLETED 상태 주문 포함")
        void orderStatusFiltering_PaymentCompletedIncluded() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            // PAYMENT_COMPLETED 상태인 completedOrder2의 상품들이 포함되어야 함
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct2.getId(), dogProduct3.getId(), catProduct2.getId());
        }

        @Test
        @DisplayName("PENDING_PAYMENT 상태 주문 제외")
        void orderStatusFiltering_PendingPaymentExcluded() {
            // when
            List<Products> dogResult = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.DOG);

            // then
            // PENDING_PAYMENT 상태의 주문은 계산에서 제외되어야 함
            // dogProduct1은 DELIVERED에서 2번, PENDING_PAYMENT에서 1번 주문되었으므로
            // 결과적으로 2번으로 계산되어야 함 (PENDING_PAYMENT 제외)
            assertThat(dogResult).isNotEmpty();

            // 주문 횟수별로 그룹핑하여 검증
            Map<Long, List<Products>> countGroups = dogResult.stream()
                    .collect(Collectors.groupingBy(p -> getOrderCountForProduct(p)));

            // dogProduct1이 가장 많은 주문 횟수(2번)를 가져야 함
            Long maxCount = countGroups.keySet().stream().max(Long::compareTo).orElse(0L);
            assertThat(maxCount).isEqualTo(2L);

            // 최대 주문 횟수를 가진 상품 중에 dogProduct1이 포함되어야 함
            List<Products> mostPopularProducts = countGroups.get(maxCount);
            assertThat(mostPopularProducts).extracting(Products::getId)
                    .contains(dogProduct1.getId());

            // PENDING_PAYMENT 주문이 제외되었는지 확인하기 위해
            // dogProduct1의 실제 주문 횟수가 2회인지 검증
            assertThat(maxCount).isEqualTo(2L); // PENDING_PAYMENT 1번 제외된 결과
        }

        private Long getOrderCountForProduct(Products product) {
            List<Object[]> results = cartRecommendationRepository.findPopularProductsByCategoryWithCount(product.getPetCategory());

            return results.stream()
                    .filter(result -> ((Products) result[0]).getId().equals(product.getId()))
                    .map(result -> (Long) result[1])
                    .findFirst()
                    .orElse(0L);
        }

        @Test
        @DisplayName("CANCELLED 상태 주문 제외")
        void orderStatusFiltering_CancelledExcluded() {
            // when
            List<Products> catResult = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.CAT);

            // then
            // CANCELLED 상태인 cancelledOrder의 catProduct3는 포함되지 않아야 함
            assertThat(catResult).extracting(Products::getId)
                    .doesNotContain(catProduct3.getId());
        }
    }

    @Nested
    @DisplayName("JPQL 쿼리 동작 확인")
    class JPQLQueryValidation {

        @Test
        @DisplayName("LEFT JOIN 동작 확인 - 주문되지 않은 상품도 포함")
        void jpqlQuery_LeftJoinIncludesUnorderedProducts() {
            // given - 주문되지 않은 새로운 상품 생성
            Products unorderedProduct = Products.builder()
                    .productNumber(9999L)
                    .seller(seller)
                    .title("주문되지 않은 상품")
                    .contents("아직 주문되지 않은 상품")
                    .petCategory(PetCategory.DOG)
                    .productCategory(ProductCategory.HANDMADE)
                    .stockStatus(StockStatus.IN_STOCK)
                    .isDiscounted(false)
                    .price(5000L)
                    .leadTime((short) 2)
                    .stock(200)
                    .build();
            entityManager.persistAndFlush(unorderedProduct);

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.DOG);

            // then
            // LEFT JOIN이므로 주문되지 않은 상품도 포함되어야 함
            assertThat(result).extracting(Products::getId)
                    .contains(unorderedProduct.getId());
        }

        @Test
        @DisplayName("GROUP BY 동작 확인 - 중복 제거")
        void jpqlQuery_GroupByEliminatesDuplicates() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            // 같은 상품이 여러 주문에 있어도 중복되지 않아야 함
            List<String> productIds = result.stream()
                    .map(Products::getId)
                    .toList();

            assertThat(productIds).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("ORDER BY COUNT 동작 확인 - 주문 수량별 정렬")
        void jpqlQuery_OrderByCountWorksCorrectly() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsAll();

            // then
            assertThat(result).isNotEmpty();

            // dogProduct1 (2번 주문)이 가장 먼저 와야 함
            assertThat(result.get(0).getId()).isEqualTo(dogProduct1.getId());

            // 다른 상품들 (각 1번씩 주문)은 그 다음에 와야 함
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct2.getId(), dogProduct3.getId(), catProduct1.getId(), catProduct2.getId());
        }

        @Test
        @DisplayName("WHERE 조건 동작 확인 - 카테고리 필터링")
        void jpqlQuery_WhereConditionWorksCorrectly() {
            // when
            List<Products> dogResult = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.DOG);
            List<Products> catResult = cartRecommendationRepository.findPopularProductsByCategory(PetCategory.CAT);

            // then
            // DOG 카테고리 결과에는 DOG 상품만 있어야 함
            assertThat(dogResult).allMatch(p -> p.getPetCategory() == PetCategory.DOG);

            // CAT 카테고리 결과에는 CAT 상품만 있어야 함
            assertThat(catResult).allMatch(p -> p.getPetCategory() == PetCategory.CAT);
        }

        @Test
        @DisplayName("NOT IN 조건 동작 확인 - 제외 상품 필터링")
        void jpqlQuery_NotInConditionWorksCorrectly() {
            // given
            List<String> excludeProductIds = Arrays.asList(dogProduct1.getId(), dogProduct2.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.DOG, excludeProductIds);

            // then
            // 제외된 상품들이 결과에 없어야 함
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId(), dogProduct2.getId());

            // 제외되지 않은 DOG 상품은 포함되어야 함
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct3.getId());
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황 테스트")
    class EdgeCasesAndExceptions {

        @Test
        @DisplayName("빈 제외 목록으로 조회")
        void edgeCases_EmptyExclusionList() {
            // given
            List<String> emptyExcludeList = Collections.emptyList();

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsExcluding(emptyExcludeList);

            // then
            assertThat(result).isNotEmpty();
            // 제외할 상품이 없으므로 모든 주문된 상품이 포함되어야 함
            assertThat(result).extracting(Products::getId)
                    .contains(dogProduct1.getId(), dogProduct2.getId(), dogProduct3.getId(),
                            catProduct1.getId(), catProduct2.getId());
        }

        @Test
        @DisplayName("모든 상품을 제외하는 경우")
        void edgeCases_ExcludeAllProducts() {
            // given
            List<String> allProductIds = Arrays.asList(
                    dogProduct1.getId(), dogProduct2.getId(), dogProduct3.getId(),
                    catProduct1.getId(), catProduct2.getId(), catProduct3.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsExcluding(allProductIds);

            // then
            // 모든 상품을 제외했으므로 결과가 비어있거나 주문되지 않은 상품만 있어야 함
            assertThat(result).allMatch(p -> !allProductIds.contains(p.getId()));
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 조회")
        void edgeCases_NonExistentCategory() {
            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategory(null);

            // then
            // null 카테고리로 조회하면 결과가 비어있어야 함
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("중복된 제외 상품 ID")
        void edgeCases_DuplicateExclusionIds() {
            // given
            List<String> duplicateExcludeIds = Arrays.asList(
                    dogProduct1.getId(), dogProduct1.getId(), dogProduct2.getId());

            // when
            List<Products> result = cartRecommendationRepository.findPopularProductsByCategoryExcluding(
                    PetCategory.DOG, duplicateExcludeIds);

            // then
            assertThat(result).isNotEmpty();
            // 중복이 있어도 정상적으로 제외되어야 함
            assertThat(result).extracting(Products::getId)
                    .doesNotContain(dogProduct1.getId(), dogProduct2.getId())
                    .contains(dogProduct3.getId());
        }
    }
}