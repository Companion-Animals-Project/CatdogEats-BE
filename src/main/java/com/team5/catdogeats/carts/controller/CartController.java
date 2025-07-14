package com.team5.catdogeats.carts.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartResponse;
import com.team5.catdogeats.carts.service.CartService;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/buyers/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "장바구니", description = "장바구니 관리 API")
public class CartController {

    private final CartService cartService;

    @Operation(
            summary = "장바구니 조회",
            description = "사용자의 장바구니 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<APIResponse<CartResponse>> getCartItems(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 인증 체크
        if (userPrincipal == null) {
            throw new SecurityException("인증이 필요합니다.");
        }

        CartResponse cartResponse = cartService.getCartByUserPrincipal(userPrincipal);

        log.info("장바구니 조회 완료 - provider: {}, providerId: {}, 총 상품 수: {}",
                userPrincipal.provider(), userPrincipal.providerId(), cartResponse.getTotalItemCount());

        return ResponseEntity.ok(APIResponse.success(ResponseCode.CART_SUCCESS, cartResponse));
    }

    @Operation(
            summary = "장바구니에 상품 추가",
            description = "장바구니에 새로운 상품을 추가하거나 기존 상품의 수량을 증가시킵니다."
    )
    @PostMapping
    public ResponseEntity<APIResponse<CartResponse>> addCartItem(
            @Valid @RequestBody AddCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 인증 체크
        if (userPrincipal == null) {
            throw new SecurityException("인증이 필요합니다.");
        }

        CartResponse cartResponse = cartService.addItemToCart(userPrincipal, request);

        log.info("장바구니 상품 추가 완료 - provider: {}, providerId: {}, productId: {}, quantity: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.getProductId(), request.getQuantity());

        return ResponseEntity.ok(APIResponse.success(ResponseCode.CART_ITEM_ADDED, cartResponse));
    }

    @Operation(
            summary = "장바구니 아이템 수량 수정",
            description = "장바구니에 있는 특정 상품의 수량을 수정합니다."
    )
    @PatchMapping("/{cartItemId}")
    public ResponseEntity<APIResponse<CartResponse>> updateCartItem(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 인증 체크
        if (userPrincipal == null) {
            throw new SecurityException("인증이 필요합니다.");
        }

        CartResponse cartResponse = cartService.updateCartItem(userPrincipal, cartItemId, request);

        log.info("장바구니 상품 수량 수정 완료 - provider: {}, providerId: {}, cartItemId: {}, newQuantity: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                cartItemId, request.getQuantity());

        return ResponseEntity.ok(APIResponse.success(ResponseCode.CART_ITEM_UPDATED, cartResponse));
    }

    @Operation(
            summary = "장바구니 아이템 삭제",
            description = "장바구니에서 특정 상품을 삭제합니다."
    )
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<APIResponse<Void>> removeCartItem(
            @PathVariable String cartItemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 인증 체크
        if (userPrincipal == null) {
            throw new SecurityException("인증이 필요합니다.");
        }

        cartService.removeCartItem(userPrincipal, cartItemId);

        log.info("장바구니 상품 삭제 완료 - provider: {}, providerId: {}, cartItemId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), cartItemId);

        return ResponseEntity.ok(APIResponse.success(ResponseCode.CART_ITEM_REMOVED));
    }

    @Operation(
            summary = "장바구니 비우기",
            description = "장바구니의 모든 상품을 삭제합니다."
    )
    @DeleteMapping
    public ResponseEntity<APIResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 인증 체크
        if (userPrincipal == null) {
            throw new SecurityException("인증이 필요합니다.");
        }

        cartService.clearCart(userPrincipal);

        log.info("장바구니 전체 비우기 완료 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        return ResponseEntity.ok(APIResponse.success(ResponseCode.CART_CLEARED));
    }
}