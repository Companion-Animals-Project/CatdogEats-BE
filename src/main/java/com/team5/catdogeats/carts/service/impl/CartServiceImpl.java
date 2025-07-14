package com.team5.catdogeats.carts.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.domain.Carts;
import com.team5.catdogeats.carts.domain.mapping.CartItems;
import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartItemResponse;
import com.team5.catdogeats.carts.dto.response.CartResponse;
import com.team5.catdogeats.carts.repository.CartItemRepository;
import com.team5.catdogeats.carts.repository.CartRepository;
import com.team5.catdogeats.carts.service.CartService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@JpaTransactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;

    @Override
    public CartResponse getCartByUserPrincipal(UserPrincipal userPrincipal) {
        Buyers user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getUserId());
        List<CartItems> cartItems = cartItemRepository.findByCartsIdWithProduct(cart.getId());

        log.debug("장바구니 조회 - userId: {}, 상품 수: {}", user.getUserId(), cartItems.size());
        return buildCartResponse(cart, cartItems);
    }

    @Override
    @JpaTransactional
    public CartResponse addItemToCart(UserPrincipal userPrincipal, AddCartItemRequest request) {
        Buyers user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getUserId());
        Products product = getProductById(request.getProductId());

        // 기존에 같은 상품이 있는지 확인
        CartItems existingItem = cartItemRepository
                .findByCartsIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            // 기존 상품이 있으면 수량 증가
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            // ⭐ Entity 검증이 동작하도록 수정
            existingItem.setQuantity(newQuantity); // Entity @Max(10) 검증 실행
            cartItemRepository.save(existingItem);

            log.info("장바구니 기존 상품 수량 증가 - productId: {}, 기존: {}, 추가: {}, 총합: {}",
                    request.getProductId(), existingItem.getQuantity() - request.getQuantity(),
                    request.getQuantity(), newQuantity);
        } else {
            // 새로운 상품 추가
            CartItems newItem = CartItems.builder()
                    .carts(cart)
                    .product(product)
                    .quantity(request.getQuantity()) // Entity @Min(1), @Max(10) 검증 실행
                    .build();
            cartItemRepository.save(newItem);

            log.info("장바구니 새 상품 추가 - productId: {}, quantity: {}",
                    request.getProductId(), request.getQuantity());
        }

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @JpaTransactional
    public CartResponse updateCartItem(UserPrincipal userPrincipal, String cartItemId, UpdateCartItemRequest request) {
        Buyers user = getUserByPrincipal(userPrincipal);

        // 권한확인 + 조회
        CartItems cartItem = cartItemRepository.findByIdAndBuyerId(cartItemId, user.getUserId())
                .orElseThrow(() -> {
                    log.warn("장바구니 아이템 접근 권한 없음 또는 존재하지 않음 - userId: {}, cartItemId: {}",
                            user.getUserId(), cartItemId);
                    return new SecurityException("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
                });

        int oldQuantity = cartItem.getQuantity();

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        log.info("장바구니 상품 수량 수정 - cartItemId: {}, 이전: {}, 변경: {}",
                cartItemId, oldQuantity, request.getQuantity());

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @JpaTransactional
    public CartResponse removeCartItem(UserPrincipal userPrincipal, String cartItemId) {
        Buyers user = getUserByPrincipal(userPrincipal);

        // 권한 확인 + 조회
        CartItems cartItem = cartItemRepository.findByIdAndBuyerId(cartItemId, user.getUserId())
                .orElseThrow(() -> {
                    log.warn("장바구니 아이템 삭제 권한 없음 또는 존재하지 않음 - userId: {}, cartItemId: {}",
                            user.getUserId(), cartItemId);
                    return new SecurityException("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
                });

        String productName = cartItem.getProduct().getTitle();
        cartItemRepository.delete(cartItem);

        log.info("장바구니 상품 삭제 - cartItemId: {}, productName: {}", cartItemId, productName);

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @JpaTransactional
    public void clearCart(UserPrincipal userPrincipal) {
        Buyers user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getUserId());
        List<CartItems> cartItems = cartItemRepository.findByCartsId(cart.getId());

        int deletedCount = cartItems.size();
        cartItemRepository.deleteAll(cartItems);

        log.info("장바구니 전체 비우기 - userId: {}, 삭제된 상품 수: {}", user.getUserId(), deletedCount);
    }

    // === Private Helper Methods ===

    private Carts getOrCreateCart(String userId) {
        return cartRepository.findByBuyerId(userId)
                .orElseGet(() -> {
                    log.info("새 장바구니 생성 - userId: {}", userId);
                    return createNewCart(userId);
                });
    }

    private Carts createNewCart(String userId) {
        Buyers user = getUserById(userId);
        Carts newCart = Carts.builder()
                .buyers(user)
                .build();
        return cartRepository.save(newCart);
    }

    // 사용자 조회
    private Buyers getUserByPrincipal(UserPrincipal userPrincipal) {
         BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(),
                        userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
         return BuyerDTO.toEntity(buyerDTO);
    }

    private Buyers getUserById(String userId) {
        return buyerRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 ID로 조회 실패 - userId: {}", userId);
                    return new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId);
                });
    }

    private Products getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("상품을 찾을 수 없음 - productId: {}", productId);
                    return new NoSuchElementException("상품을 찾을 수 없습니다: " + productId);
                });
    }

    private CartResponse buildCartResponse(Carts cart, List<CartItems> cartItems) {
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());

        Long totalAmount = itemResponses.stream()
                .mapToLong(CartItemResponse::getTotalPrice)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .buyerId(cart.getBuyers().getUserId())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItemCount(itemResponses.size())
                .build();
    }

    private CartItemResponse convertToCartItemResponse(CartItems cartItem) {
        return CartItemResponse.from(cartItem);
    }

    // 결제 완료 후 구매상품 장바구니에서 삭제 - 이벤트 기반 비동기 처리, 결제 영향x
    @Override
    @JpaTransactional
    public void clearPurchasedItemsFromCart(String userId, List<String> purchasedProductIds) {
        log.info("결제 완료 후 구매 상품들만 장바구니에서 삭제 시작 - userId: {}, 구매 상품 수: {}",
                userId, purchasedProductIds != null ? purchasedProductIds.size() : 0);

        // 입력값 검증
        if (purchasedProductIds == null || purchasedProductIds.isEmpty()) {
            log.warn("구매 상품 목록이 비어있음 - userId: {}", userId);
            return;
        }

        try {
            // 장바구니 조회 후 상품들 삭제
            Carts cart = cartRepository.findByBuyerId(userId).orElse(null);
            if (cart == null) {
                log.info("장바구니가 존재하지 않음 - userId: {}", userId);
                return;
            }

            // 구매한 상품들만 장바구니에서 조회
            List<CartItems> itemsToDelete = cartItemRepository
                    .findByCartsIdAndProductIdIn(cart.getId(), purchasedProductIds);

            if (itemsToDelete.isEmpty()) {
                log.info("삭제할 장바구니 상품이 없음 - userId: {}, cartId: {}", userId, cart.getId());
                return;
            }

            // 삭제 전 로깅 (디버깅 및 모니터링용)
            List<String> deletedProductNames = itemsToDelete.stream()
                    .map(item -> item.getProduct().getTitle())
                    .collect(Collectors.toList());

            // 구매한 상품 삭제
            cartItemRepository.deleteAll(itemsToDelete);

            log.info("구매 상품들 장바구니에서 삭제 완료 - userId: {}, 삭제된 상품 수: {}, 상품들: {}",
                    userId, itemsToDelete.size(), deletedProductNames);

        } catch (Exception e) {
            log.error("구매 상품 장바구니 정리 실패 - userId: {}, purchasedProductIds: {}",
                    userId, purchasedProductIds, e);

            // 예외를 다시 던져서 이벤트 리스너에서 실패 처리
            throw new RuntimeException("장바구니 정리 실패: " + e.getMessage(), e);
        }
    }
}