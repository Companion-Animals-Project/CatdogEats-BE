package com.team5.catdogeats.carts.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.domain.mapping.CartItems;
import com.team5.catdogeats.carts.dto.response.RecommendationResponse;
import com.team5.catdogeats.carts.repository.CartItemRepository;
import com.team5.catdogeats.carts.repository.CartRecommendationRepository;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartRecommendationServiceImpl 테스트 (리팩토링)")
class CartRecommendationServiceImplTest {

    @Mock
    private CartRecommendationRepository cartRecommendationRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartRecommendationServiceImpl cartRecommendationService;

    // ✅ 상수 추가 (실제 Service와 동일하게)
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 4;

    private UserPrincipal userPrincipal;
    private Users testUser;
    private Users testSeller;
    private Sellers seller;
    private Products dogProduct1;
    private Products dogProduct2;
    private Products catProduct1;
    private Products catProduct2;
    private Products recommendProduct1;
    private Products recommendProduct2;
    private CartItems dogCartItem;
    private CartItems catCartItem;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal("google", "test-provider-id");

        testUser = Users.builder()
                .id("test-user-id")
                .provider("google")
                .providerId("test-provider-id")
                .userNameAttribute("sub")
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER)
                .build();

        testSeller = Users.builder()
                .id("test-seller-id")
                .provider("google")
                .providerId("test-seller-provider-id")
                .userNameAttribute("sub")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .build();

        seller = Sellers.builder()
                .userId(testSeller.getId())
                .user(testSeller)
                .vendorName("테스트 상점")
                .build();

        // DOG 카테고리 장바구니 상품들
        dogProduct1 = Products.builder()
                .id("dog-product-1")
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

        dogProduct2 = Products.builder()
                .id("dog-product-2")
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

        // CAT 카테고리 장바구니 상품들
        catProduct1 = Products.builder()
                .id("cat-product-1")
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

        catProduct2 = Products.builder()
                .id("cat-product-2")
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

        // 추천 상품들
        recommendProduct1 = Products.builder()
                .id("recommend-product-1")
                .productNumber(3001L)
                .seller(seller)
                .title("추천 상품 1")
                .contents("인기 추천 상품")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.HANDMADE)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(true)
                .discountRate(15.0)
                .price(20000L)
                .leadTime((short) 3)
                .stock(200)
                .build();

        recommendProduct2 = Products.builder()
                .id("recommend-product-2")
                .productNumber(3002L)
                .seller(seller)
                .title("추천 상품 2")
                .contents("베스트 추천 상품")
                .petCategory(PetCategory.CAT)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(false)
                .price(25000L)
                .leadTime((short) 4)
                .stock(150)
                .build();

        // 장바구니 아이템들
        dogCartItem = CartItems.builder()
                .id("dog-cart-item-1")
                .product(dogProduct1)
                .quantity(2)
                .build();

        catCartItem = CartItems.builder()
                .id("cat-cart-item-1")
                .product(catProduct1)
                .quantity(1)
                .build();
    }

    @Nested
    @DisplayName("장바구니 기반 추천 상품 조회")
    class GetCartBasedRecommendations {

        @Test
        @DisplayName("성공 - DOG 카테고리만 있는 장바구니")
        void getCartBasedRecommendations_OnlyDogCategory_Success() {
            // given
            List<CartItems> dogOnlyCartItems = Arrays.asList(dogCartItem);
            List<Products> dogRecommendProducts = Arrays.asList(recommendProduct1);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(dogOnlyCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1"), DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(dogRecommendProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductId()).isEqualTo("recommend-product-1");
            assertThat(result.get(0).getTitle()).isEqualTo("추천 상품 1");
            assertThat(result.get(0).getPetCategory()).isEqualTo(PetCategory.DOG);
            assertThat(result.get(0).getPrice()).isEqualTo(20000L);

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1"), DEFAULT_RECOMMENDATION_LIMIT);
        }

        @Test
        @DisplayName("성공 - CAT 카테고리만 있는 장바구니")
        void getCartBasedRecommendations_OnlyCatCategory_Success() {
            // given
            List<CartItems> catOnlyCartItems = Arrays.asList(catCartItem);
            List<Products> catRecommendProducts = Arrays.asList(recommendProduct2);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(catOnlyCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.CAT.name(), Arrays.asList("cat-product-1"), DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(catRecommendProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductId()).isEqualTo("recommend-product-2");
            assertThat(result.get(0).getTitle()).isEqualTo("추천 상품 2");
            assertThat(result.get(0).getPetCategory()).isEqualTo(PetCategory.CAT);
            assertThat(result.get(0).getPrice()).isEqualTo(25000L);

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsByCategoryExcluding(
                    PetCategory.CAT.name(), Arrays.asList("cat-product-1"), DEFAULT_RECOMMENDATION_LIMIT);
        }

        @Test
        @DisplayName("성공 - DOG+CAT 혼합 카테고리 장바구니")
        void getCartBasedRecommendations_MixedCategories_Success() {
            // given
            List<CartItems> mixedCartItems = Arrays.asList(dogCartItem, catCartItem);
            List<Products> allRecommendProducts = Arrays.asList(recommendProduct1, recommendProduct2);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(mixedCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsExcluding(
                    Arrays.asList("dog-product-1", "cat-product-1"), DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(allRecommendProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(RecommendationResponse::getProductId)
                    .containsExactly("recommend-product-1", "recommend-product-2");

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsExcluding(
                    Arrays.asList("dog-product-1", "cat-product-1"), DEFAULT_RECOMMENDATION_LIMIT);
        }

        @Test
        @DisplayName("성공 - 빈 장바구니")
        void getCartBasedRecommendations_EmptyCart_Success() {
            // given
            List<CartItems> emptyCartItems = Collections.emptyList();
            List<Products> allRecommendProducts = Arrays.asList(recommendProduct1, recommendProduct2);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(emptyCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(allRecommendProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(RecommendationResponse::getProductId)
                    .containsExactly("recommend-product-1", "recommend-product-2");

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT);
        }

        @Test
        @DisplayName("오류 발생 시 전체 인기 상품 추천으로 대체 - 사용자 없음")
        void getCartBasedRecommendations_UserNotFound_ReturnsFallback() {
            // given
            List<Products> fallbackProducts = Arrays.asList(recommendProduct1, recommendProduct2);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.empty());
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(fallbackProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(RecommendationResponse::getProductId)
                    .containsExactly("recommend-product-1", "recommend-product-2");

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT);
            verify(userRepository).findByProviderAndProviderId("google", "test-provider-id");
        }

        @Test
        @DisplayName("오류 발생 시 전체 인기 상품 추천으로 대체")
        void getCartBasedRecommendations_ExceptionFallback_Success() {
            // given
            List<Products> fallbackProducts = Arrays.asList(recommendProduct1, recommendProduct2);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willThrow(new RuntimeException("DB 연결 오류"));
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(fallbackProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(RecommendationResponse::getProductId)
                    .containsExactly("recommend-product-1", "recommend-product-2");

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT);
        }
    }

    @Nested
    @DisplayName("Products 엔티티 변환")
    class ConvertToRecommendationResponse {

        @Test
        @DisplayName("정상 상품 변환")
        void convertToRecommendationResponse_NormalProduct_Success() {
            // given
            List<CartItems> emptyCartItems = Collections.emptyList();
            List<Products> products = Arrays.asList(recommendProduct1);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(emptyCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(products);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(1);

            RecommendationResponse response = result.get(0);
            assertThat(response.getProductId()).isEqualTo("recommend-product-1");
            assertThat(response.getProductNumber()).isEqualTo(3001L);
            assertThat(response.getTitle()).isEqualTo("추천 상품 1");
            assertThat(response.getPrice()).isEqualTo(20000L);
            assertThat(response.getPetCategory()).isEqualTo(PetCategory.DOG);
            assertThat(response.getPurchaseCount()).isEqualTo(0L); // 임시로 0
        }

        @Test
        @DisplayName("할인 상품 변환")
        void convertToRecommendationResponse_DiscountedProduct_Success() {
            // given
            List<CartItems> emptyCartItems = Collections.emptyList();
            List<Products> products = Arrays.asList(dogProduct2); // 할인 상품

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(emptyCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(products);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(1);

            RecommendationResponse response = result.get(0);
            assertThat(response.getProductId()).isEqualTo("dog-product-2");
            assertThat(response.getProductNumber()).isEqualTo(1002L);
            assertThat(response.getTitle()).isEqualTo("강아지 간식 2");
            assertThat(response.getPrice()).isEqualTo(15000L);
            assertThat(response.getPetCategory()).isEqualTo(PetCategory.DOG);
            assertThat(response.getPurchaseCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황 테스트")
    class EdgeCasesAndExceptions {

        @Test
        @DisplayName("null UserPrincipal 처리")
        void getCartBasedRecommendations_NullUserPrincipal_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> cartRecommendationService.getCartBasedRecommendations(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Repository에서 빈 목록 반환")
        void getCartBasedRecommendations_RepositoryReturnsEmpty_Success() {
            // given
            List<CartItems> dogOnlyCartItems = Arrays.asList(dogCartItem);
            List<Products> emptyProducts = Collections.emptyList();

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(dogOnlyCartItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1"), DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(emptyProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("같은 카테고리 상품이 여러 개 있는 장바구니")
        void getCartBasedRecommendations_MultipleSameCategory_Success() {
            // given
            CartItems anotherDogCartItem = CartItems.builder()
                    .id("another-dog-cart-item")
                    .product(dogProduct2)
                    .quantity(3)
                    .build();

            List<CartItems> multipleDogItems = Arrays.asList(dogCartItem, anotherDogCartItem);
            List<Products> dogRecommendProducts = Arrays.asList(recommendProduct1);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(multipleDogItems);
            // ✅ 새로운 메서드로 변경
            given(cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1", "dog-product-2"), DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(dogRecommendProducts);

            // when
            List<RecommendationResponse> result = cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductId()).isEqualTo("recommend-product-1");

            // ✅ 새로운 메서드 검증
            verify(cartRecommendationRepository).findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1", "dog-product-2"), DEFAULT_RECOMMENDATION_LIMIT);
        }
    }

    // ✅ 새로운 테스트: LIMIT 기능 검증
    @Nested
    @DisplayName("LIMIT 기능 검증")
    class LimitFunctionality {

        @Test
        @DisplayName("DEFAULT_RECOMMENDATION_LIMIT이 올바르게 전달되는지 확인")
        void verifyDefaultLimitIsPassed() {
            // given
            List<CartItems> emptyCartItems = Collections.emptyList();
            List<Products> products = Arrays.asList(recommendProduct1, recommendProduct2);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(emptyCartItems);
            given(cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(products);

            // when
            cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then - LIMIT 파라미터가 올바르게 전달되었는지 확인
            verify(cartRecommendationRepository).findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT);
        }

        @Test
        @DisplayName("카테고리별 추천에서도 LIMIT이 올바르게 전달되는지 확인")
        void verifyLimitPassedForCategoryRecommendation() {
            // given
            List<CartItems> dogOnlyCartItems = Arrays.asList(dogCartItem);
            List<Products> dogProducts = Arrays.asList(recommendProduct1);

            given(userRepository.findByProviderAndProviderId("google", "test-provider-id"))
                    .willReturn(Optional.of(testUser));
            given(cartItemRepository.findCartItemsWithProductByUserId("test-user-id"))
                    .willReturn(dogOnlyCartItems);
            given(cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1"), DEFAULT_RECOMMENDATION_LIMIT))
                    .willReturn(dogProducts);

            // when
            cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            // then - LIMIT 파라미터가 올바르게 전달되었는지 확인
            verify(cartRecommendationRepository).findTopPopularProductsByCategoryExcluding(
                    PetCategory.DOG.name(), Arrays.asList("dog-product-1"), DEFAULT_RECOMMENDATION_LIMIT);
        }
    }
}