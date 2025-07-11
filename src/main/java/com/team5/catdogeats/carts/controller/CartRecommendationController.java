package com.team5.catdogeats.carts.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.response.RecommendationResponse;
import com.team5.catdogeats.carts.service.CartRecommendationService;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/buyers/carts")
@RequiredArgsConstructor
@Tag(name = "Cart Recommendation", description = "장바구니 추천 상품 API")
public class CartRecommendationController {

    private final CartRecommendationService cartRecommendationService;

    @Operation(
            summary = "장바구니 기반 추천 상품 조회",
            description = """
                    사용자의 장바구니 상품들을 분석하여 추천 상품을 제공합니다.
                  
                    - 강아지 상품만 담긴 장바구니 → 강아지 카테고리 인기 상품 추천
                    - 고양이 상품만 담긴 장바구니 → 고양이 카테고리 인기 상품 추천
                    - 강아지+고양이 혼합 장바구니 → 전체 인기 상품 추천
                    - 빈 장바구니 → 전체 인기 상품 추천
                    
                    **기준:** 주문 완료/배송 완료 기준 구매 횟수 순
                    **결과:** 4개 상품 고정 반환
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 상품 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    @GetMapping("/recommendation")
    public ResponseEntity<APIResponse<List<RecommendationResponse>>> getCartBasedRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("장바구니 기반 추천 상품 조회 요청 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        try {
            // 추천 상품 조회
            List<RecommendationResponse> recommendations =
                    cartRecommendationService.getCartBasedRecommendations(userPrincipal);

            log.info("장바구니 기반 추천 상품 조회 성공 - provider: {}, providerId: {}, 추천 개수: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), recommendations.size());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, recommendations));

        } catch (Exception e) {
            log.error("장바구니 기반 추천 상품 조회 실패 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);

            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }
}