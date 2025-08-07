package com.team5.catdogeats.carts.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.domain.mapping.CartItems;
import com.team5.catdogeats.carts.dto.response.RecommendationResponse;
import com.team5.catdogeats.carts.repository.CartItemRepository;
import com.team5.catdogeats.carts.repository.CartRecommendationRepository;
import com.team5.catdogeats.carts.service.CartRecommendationService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@JpaTransactional(readOnly = true)
public class CartRecommendationServiceImpl implements CartRecommendationService {

    private final CartRecommendationRepository cartRecommendationRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    // ㅐ상수로 추천 개수 관리 (향후 설정으로 변경 가능)
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 4;

    @Override
    public List<RecommendationResponse> getCartBasedRecommendations(UserPrincipal userPrincipal) {
        log.debug("장바구니 기반 추천 조회 시작 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        try {
            // 1. 사용자 조회
            Users user = getUserByPrincipal(userPrincipal);

            // 2. 사용자의 장바구니 아이템들 조회 (상품 정보 포함)
            List<CartItems> cartItems = cartItemRepository.findCartItemsWithProductByBuyerId(user.getId());

            // 3. 장바구니가 비어있으면 전체 인기 상품 추천
            if (cartItems.isEmpty()) {
                log.info("장바구니가 비어있어 전체 인기 상품 추천 - userId: {}", user.getId());
                return getPopularProductsAll(Collections.emptyList());
            }

            // 4. 장바구니 상품들의 카테고리 분석
            Set<PetCategory> categories = cartItems.stream()
                    .map(cartItem -> cartItem.getProduct().getPetCategory())
                    .collect(Collectors.toSet());

            // 5. 장바구니에 이미 담긴 상품 ID들 추출 (중복 방지용)
            List<String> excludeProductIds = cartItems.stream()
                    .map(cartItem -> cartItem.getProduct().getId())
                    .collect(Collectors.toList());

            // 6. 카테고리에 따른 추천 전략 결정
            List<RecommendationResponse> recommendations = determineRecommendationStrategy(
                    categories, excludeProductIds);

            log.info("장바구니 기반 추천 완료 - userId: {}, 장바구니 상품: {}개, 추천 상품: {}개",
                    user.getId(), cartItems.size(), recommendations.size());

            return recommendations;

        } catch (Exception e) {
            log.error("장바구니 기반 추천 중 오류 발생 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);

            // 오류 발생 시 전체 인기 상품
            return getPopularProductsAll(Collections.emptyList());
        }
    }

    // 카테고리 분석에 따른 추천 전략 결정
    private List<RecommendationResponse> determineRecommendationStrategy(
            Set<PetCategory> categories, List<String> excludeProductIds) {

        if (categories.size() == 1) {
            // 단일 카테고리 (강아지 또는 고양이)
            PetCategory singleCategory = categories.iterator().next();
            log.debug("단일 카테고리 추천 - category: {}", singleCategory);
            return getPopularProductsByCategory(singleCategory, excludeProductIds);

        } else {
            // 혼합 카테고리 (강아지 + 고양이)
            log.debug("혼합 카테고리 장바구니 - 전체 인기 상품 추천");
            return getPopularProductsAll(excludeProductIds);
        }
    }

    // 특정 카테고리 인기 상품 조회
    private List<RecommendationResponse> getPopularProductsByCategory(
            PetCategory petCategory, List<String> excludeProductIds) {

        try {
            List<Products> products;

            // 제외할 상품이 있는지 확인하고 적절한 메서드 호출
            if (excludeProductIds == null || excludeProductIds.isEmpty()) {
                // DB에서 4개 조회
                products = cartRecommendationRepository.findTopPopularProductsByCategory(
                        petCategory.name(), DEFAULT_RECOMMENDATION_LIMIT);
            } else {
                // DB에서 4개 조회
                products = cartRecommendationRepository.findTopPopularProductsByCategoryExcluding(
                        petCategory.name(), excludeProductIds, DEFAULT_RECOMMENDATION_LIMIT);
            }

            return products.stream()
                    .map(this::convertToRecommendationResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("카테고리별 추천 조회 실패, 전체 인기 상품으로 대체 - category: {}", petCategory, e);
            return getPopularProductsAll(excludeProductIds);
        }
    }

    // 전체 인기 상품 조회
    private List<RecommendationResponse> getPopularProductsAll(List<String> excludeProductIds) {
        try {
            List<Products> products;

            // 제외할 상품이 있는지 확인하고 적절한 메서드 호출
            if (excludeProductIds == null || excludeProductIds.isEmpty()) {
                // DB에서 4개 조회
                products = cartRecommendationRepository.findTopPopularProductsAll(DEFAULT_RECOMMENDATION_LIMIT);
            } else {
                // DB에서 4개 조회
                products = cartRecommendationRepository.findTopPopularProductsExcluding(
                        excludeProductIds, DEFAULT_RECOMMENDATION_LIMIT);
            }

            return products.stream()
                    .map(this::convertToRecommendationResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("전체 인기 상품 조회 실패", e);
            return Collections.emptyList();
        }
    }

    // Products 엔티티를 RecommendationResponse로 변환
    private RecommendationResponse convertToRecommendationResponse(Products product) {
        return RecommendationResponse.builder()
                .productId(product.getId())
                .productNumber(product.getProductNumber())
                .title(product.getTitle())
                .price(product.getPrice())
                .petCategory(product.getPetCategory())
                .purchaseCount(0L) // 임시로 0, 필요시 별도 쿼리로 조회
                .build();
    }

    // 사용자 조회
    private Users getUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(),
                        userPrincipal.providerId())
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - provider: {}, providerId: {}",
                            userPrincipal.provider(), userPrincipal.providerId());
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }
}