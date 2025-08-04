package com.team5.catdogeats.carts.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartResponse;

import java.util.List;

public interface CartService {

    CartResponse getCartByUserPrincipal(UserPrincipal userPrincipal);

    void addItemToCart(UserPrincipal userPrincipal, AddCartItemRequest request);

    CartResponse updateCartItem(UserPrincipal userPrincipal, String cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(UserPrincipal userPrincipal, String cartItemId);

    void clearCart(UserPrincipal userPrincipal);

    // 결제 완료 후 구매상품 장바구니에서 삭제 - 비동기, 실패시 결제 영향x
    void clearPurchasedItemsFromCart(String userId, List<String> purchasedProductIds);

}