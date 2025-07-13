package com.team5.catdogeats.carts.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.response.RecommendationResponse;

import java.util.List;

// 장바구니 추천 서비스 인터페이스
public interface CartRecommendationService {

    /**
     * 장바구니 기반 추천 상품 조회
     * 1. 장바구니에 강아지 상품만 있으면 → 강아지 카테고리 인기 상품 추천
     * 2. 장바구니에 고양이 상품만 있으면 → 고양이 카테고리 인기 상품 추천
     * 3. 장바구니에 강아지+고양이 섞여있으면 → 전체 인기 상품 추천
     * 4. 장바구니가 비어있으면 → 전체 인기 상품 추천
     * @return 추천 상품 목록 (4개 고정)
     */
    List<RecommendationResponse> getCartBasedRecommendations(UserPrincipal userPrincipal);
}