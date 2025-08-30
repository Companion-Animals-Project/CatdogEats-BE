//package com.team5.catdogeats.carts.service.impl;
//
//import com.team5.catdogeats.auth.dto.UserPrincipal;
//import com.team5.catdogeats.carts.domain.Carts;
//import com.team5.catdogeats.carts.domain.mapping.CartItems;
//import com.team5.catdogeats.carts.dto.TestBuyerDTO;
//import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
//import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
//import com.team5.catdogeats.carts.dto.response.CartResponse;
//import com.team5.catdogeats.carts.repository.CartItemRepository;
//import com.team5.catdogeats.carts.repository.CartRepository;
//import com.team5.catdogeats.pets.domain.enums.PetCategory;
//import com.team5.catdogeats.products.domain.Products;
//import com.team5.catdogeats.products.domain.enums.ProductCategory;
//import com.team5.catdogeats.products.repository.ProductRepository;
//import com.team5.catdogeats.users.domain.Users;
//import com.team5.catdogeats.users.domain.enums.Role;
//import com.team5.catdogeats.users.domain.mapping.Buyers;
//import com.team5.catdogeats.users.domain.mapping.Sellers;
//import com.team5.catdogeats.users.repository.BuyerRepository;
//import com.team5.catdogeats.users.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.willDoNothing;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("CartServiceImpl 테스트")
//class CartServiceImplTest {
//
//    @Mock
//    private CartRepository cartRepository;
//
//    @Mock
//    private CartItemRepository cartItemRepository;
//
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private BuyerRepository buyerRepository;
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @InjectMocks
//    private CartServiceImpl cartService;
//
//    private UserPrincipal userPrincipal;
//    private Users testUser;
//    private Buyers testBuyer;
//    private Users testSeller;
//    private Sellers seller;
//    private Carts testCart;
//    private Products testProduct;
//    private CartItems testCartItem;
//
//    @BeforeEach
//    void setUp() {
//        userPrincipal = new UserPrincipal("google", "test-provider-id");
//
//        testUser = Users.builder()
//                .id("test-user-id")
//                .provider("google")
//                .providerId("test-provider-id")
//                .userNameAttribute("sub")
//                .name("테스트 사용자")
//                .role(Role.ROLE_BUYER)
//                .build();
//
//        testBuyer = Buyers.builder()
//                .userId("test-user-id")
//                .nameMaskingStatus(true)
//                .isDeleted(false)
//                .build();
//
//        testSeller = Users.builder()
//                .id("test-seller-id")
//                .provider("google")
//                .providerId("test-seller-provider-id")
//                .userNameAttribute("sub")
//                .name("테스트 판매자")
//                .role(Role.ROLE_SELLER)
//                .build();
//
//        seller = Sellers.builder()
//                .user(testSeller)
//                .build();
//
//        testCart = Carts.builder()
//                .id("test-cart-id")
//                .buyers(testBuyer)
//                .build();
//
//        testProduct = Products.builder()
//                .id("test-product-id")
//                .productNumber(1001L)
//                .seller(seller)
//                .title("테스트 상품")
//                .contents("테스트 상품 설명")
//                .petCategory(PetCategory.DOG)
//                .productCategory(ProductCategory.HANDMADE)
//                .discounted(false)
//                .price(10000L)
//                .leadTime((short) 3)
//                .stock(100)
//                .build();
//
//        testCartItem = CartItems.builder()
//                .id("test-cart-item-id")
//                .carts(testCart)
//                .product(testProduct)
//                .quantity(1)
//                .build();
//    }
//
//    @Test
//    @DisplayName("사용자 장바구니 조회 - 성공")
//    void getCartByUserPrincipal_Success() {
//        // given
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Arrays.asList(testCartItem));
//
//        // when
//        CartResponse response = cartService.getCartByUserPrincipal(userPrincipal);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getCartId()).isEqualTo("test-cart-id");
//        assertThat(response.getItems()).hasSize(1);
//        assertThat(response.getTotalAmount()).isEqualTo(10000L);
//        assertThat(response.getTotalItemCount()).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("사용자 장바구니 조회 - 장바구니가 없는 경우 새로 생성")
//    void getCartByUserPrincipal_CreateNewCart() {
//        // given
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.empty());
//        given(buyerRepository.findById("test-user-id"))
//                .willReturn(Optional.of(testBuyer));
//        given(cartRepository.save(any(Carts.class)))
//                .willReturn(testCart);
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Collections.emptyList());
//
//        // when
//        CartResponse response = cartService.getCartByUserPrincipal(userPrincipal);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getCartId()).isEqualTo("test-cart-id");
//        assertThat(response.getItems()).isEmpty();
//        assertThat(response.getTotalAmount()).isEqualTo(0L);
//        assertThat(response.getTotalItemCount()).isEqualTo(0);
//        verify(cartRepository).save(any(Carts.class));
//    }
//
//    @Test
//    @DisplayName("장바구니에 상품 추가 - 새로운 상품")
//    void addItemToCart_NewProduct() {
//        // given
//        AddCartItemRequest request = new AddCartItemRequest();
//        request.setProductId("test-product-id");
//        request.setQuantity(2);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(productRepository.findById("test-product-id"))
//                .willReturn(Optional.of(testProduct));
//        given(cartItemRepository.findByCartsIdAndProductNumber("test-cart-id", "test-product-id"))
//                .willReturn(Optional.empty());
//
//        CartItems newCartItem = CartItems.builder()
//                .id("new-cart-item-id")
//                .carts(testCart)
//                .product(testProduct)
//                .quantity(2)
//                .build();
//
//        given(cartItemRepository.save(any(CartItems.class)))
//                .willReturn(newCartItem);
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Arrays.asList(newCartItem));
//
//        // when
//        cartService.addItemToCart(userPrincipal, request);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getItems()).hasSize(1);
//        assertThat(response.getTotalAmount()).isEqualTo(20000L);
//        verify(cartItemRepository).save(any(CartItems.class));
//    }
//
//    @Test
//    @DisplayName("장바구니에 상품 추가 - 기존 상품 수량 증가")
//    void addItemToCart_ExistingProduct() {
//        // given
//        AddCartItemRequest request = new AddCartItemRequest();
//        request.setProductId("test-product-id");
//        request.setQuantity(2);
//
//        CartItems existingItem = CartItems.builder()
//                .id("existing-item-id")
//                .carts(testCart)
//                .product(testProduct)
//                .quantity(1)
//                .build();
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(productRepository.findById("test-product-id"))
//                .willReturn(Optional.of(testProduct));
//        given(cartItemRepository.findByCartsIdAndProductId("test-cart-id", "test-product-id"))
//                .willReturn(Optional.of(existingItem));
//        given(cartItemRepository.save(existingItem))
//                .willReturn(existingItem);
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Arrays.asList(existingItem));
//
//        // when
//        CartResponse response = cartService.addItemToCart(userPrincipal, request);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(existingItem.getQuantity()).isEqualTo(3); // 1 + 2
//        verify(cartItemRepository).save(existingItem);
//    }
//
//    @Test
//    @DisplayName("장바구니 아이템 수량 수정 - 성공")
//    void updateCartItem_Success() {
//        // given
//        UpdateCartItemRequest request = new UpdateCartItemRequest();
//        request.setQuantity(5);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByIdAndBuyerId("test-cart-item-id", "test-user-id"))
//                .willReturn(Optional.of(testCartItem));
//        given(cartItemRepository.save(testCartItem))
//                .willReturn(testCartItem);
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Arrays.asList(testCartItem));
//
//        // when
//        CartResponse response = cartService.updateCartItem(userPrincipal, "test-cart-item-id", request);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(testCartItem.getQuantity()).isEqualTo(5);
//        verify(cartItemRepository).save(testCartItem);
//    }
//
//    @Test
//    @DisplayName("장바구니 아이템 수량 수정 - 권한 없음")
//    void updateCartItem_NoPermission() {
//        // given
//        UpdateCartItemRequest request = new UpdateCartItemRequest();
//        request.setQuantity(5);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
////        given(cartRepository.findByUserId("test-user-id"))
////                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByIdAndBuyerId("other-cart-item-id", "test-user-id"))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> cartService.updateCartItem(userPrincipal, "other-cart-item-id", request))
//                .isInstanceOf(SecurityException.class)
//                .hasMessage("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
//
//        verify(cartItemRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("장바구니 아이템 삭제 - 성공")
//    void removeCartItem_Success() {
//        // given
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByIdAndBuyerId("test-cart-item-id", "test-user-id"))
//                .willReturn(Optional.of(testCartItem));
//        willDoNothing().given(cartItemRepository).delete(testCartItem);
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Collections.emptyList());
//
//        // when
//        CartResponse response = cartService.removeCartItem(userPrincipal, "test-cart-item-id");
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getItems()).isEmpty();
//        assertThat(response.getTotalAmount()).isEqualTo(0L);
//        assertThat(response.getTotalItemCount()).isEqualTo(0);
//        verify(cartItemRepository).delete(testCartItem);
//    }
//
//    @Test
//    @DisplayName("장바구니 아이템 삭제 - 권한 없음")
//    void removeCartItem_NoPermission() {
//        // given
//        Users otherUser = Users.builder()
//                .id("other-user-id")
//                .provider("google")
//                .providerId("other-provider-id")
//                .userNameAttribute("sub")
//                .name("다른 사용자")
//                .role(Role.ROLE_BUYER)
//                .build();
//
//        Buyers otherBuyer = Buyers.builder()
//                .user(otherUser)
//                .nameMaskingStatus(true)
//                .userId(otherUser.getId())
//                .build();
//
//        Carts otherCart = Carts.builder()
//                .id("other-cart-id")
//                .buyers(otherBuyer)
//                .build();
//
//        CartItems otherCartItem = CartItems.builder()
//                .id("other-cart-item-id")
//                .carts(otherCart)
//                .product(testProduct)
//                .quantity(1)
//                .build();
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
////        given(cartRepository.findByUserId("test-user-id"))
////                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByIdAndBuyerId("other-cart-item-id", "test-user-id"))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> cartService.removeCartItem(userPrincipal, "other-cart-item-id"))
//                .isInstanceOf(SecurityException.class)
//                .hasMessage("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
//
//        verify(cartItemRepository, never()).delete(any());
//    }
//
//    @Test
//    @DisplayName("장바구니 비우기 - 성공")
//    void clearCart_Success() {
//        // given
//        List<CartItems> cartItems = Arrays.asList(testCartItem);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByCartsId("test-cart-id"))
//                .willReturn(cartItems);
//        willDoNothing().given(cartItemRepository).deleteAll(cartItems);
//
//        // when
//        cartService.clearCart(userPrincipal);
//
//        // then
//        verify(cartItemRepository).deleteAll(cartItems);
//    }
//
//    @Test
//    @DisplayName("빈 장바구니 비우기")
//    void clearCart_EmptyCart() {
//        // given
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByCartsId("test-cart-id"))
//                .willReturn(Collections.emptyList());
//        willDoNothing().given(cartItemRepository).deleteAll(Collections.emptyList());
//
//        // when
//        cartService.clearCart(userPrincipal);
//
//        // then
//        verify(cartItemRepository).deleteAll(Collections.emptyList());
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 사용자로 장바구니 조회")
//    void getCartByUserPrincipal_UserNotFound() {
//        // given
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> cartService.getCartByUserPrincipal(userPrincipal))
//                .isInstanceOf(NoSuchElementException.class)
//                .hasMessageContaining("사용자를 찾을 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 상품을 장바구니에 추가")
//    void addItemToCart_ProductNotFound() {
//        // given
//        AddCartItemRequest request = new AddCartItemRequest();
//        request.setProductId("nonexistent-product-id");
//        request.setQuantity(1);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(productRepository.findById("nonexistent-product-id"))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> cartService.addItemToCart(userPrincipal, request))
//                .isInstanceOf(NoSuchElementException.class)
//                .hasMessageContaining("상품을 찾을 수 없습니다");
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 장바구니 아이템 수량 수정")
//    void updateCartItem_ItemNotFound() {
//        // given
//        UpdateCartItemRequest request = new UpdateCartItemRequest();
//        request.setQuantity(3);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
////        given(cartRepository.findByUserId("test-user-id"))
////                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByIdAndBuyerId("nonexistent-item-id", "test-user-id"))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> cartService.updateCartItem(userPrincipal, "nonexistent-item-id", request))
//                .isInstanceOf(SecurityException.class)
//                .hasMessage("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 장바구니 아이템 삭제")
//    void removeCartItem_ItemNotFound() {
//        // given
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
////        given(cartRepository.findByUserId("test-user-id"))
////                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByIdAndBuyerId("nonexistent-item-id", "test-user-id"))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> cartService.removeCartItem(userPrincipal, "nonexistent-item-id"))
//                .isInstanceOf(SecurityException.class)
//                .hasMessage("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
//    }
//
//    @Test
//    @DisplayName("여러 상품이 있는 장바구니 총 금액 계산")
//    void getCartByUserPrincipal_MultipleItems_TotalAmountCalculation() {
//        // given
//        Products expensiveProduct = Products.builder()
//                .id("expensive-product-id")
//                .productNumber(1002L)
//                .seller(seller)
//                .title("비싼 상품")
//                .contents("비싼 상품 설명")
//                .petCategory(PetCategory.CAT)
//                .productCategory(ProductCategory.FINISHED)
//                .discounted(false)
//                .price(50000L)
//                .leadTime((short) 5)
//                .stock(20)
//                .build();
//
//        CartItems expensiveCartItem = CartItems.builder()
//                .id("expensive-cart-item-id")
//                .carts(testCart)
//                .product(expensiveProduct)
//                .quantity(2)
//                .build();
//
//        List<CartItems> multipleItems = Arrays.asList(testCartItem, expensiveCartItem);
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(multipleItems);
//
//        // when
//        CartResponse response = cartService.getCartByUserPrincipal(userPrincipal);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getItems()).hasSize(2);
//        assertThat(response.getTotalAmount()).isEqualTo(110000L); // (10000 * 1) + (50000 * 2)
//        assertThat(response.getTotalItemCount()).isEqualTo(2);
//    }
//
//    @Test
//    @DisplayName("할인 상품이 포함된 장바구니 총 금액 계산")
//    void getCartByUserPrincipal_WithDiscountedProduct() {
//        // given
//        Products discountedProduct = Products.builder()
//                .id("discounted-product-id")
//                .productNumber(1003L)
//                .seller(seller)
//                .title("할인 상품")
//                .contents("할인 상품 설명")
//                .petCategory(PetCategory.DOG)
//                .productCategory(ProductCategory.HANDMADE)
//                .discounted(true)
//                .discountRate((short) 20)
//                .price(20000L) // 할인 전 가격
//                .leadTime((short) 3)
//                .stock(50)
//                .build();
//
//        CartItems discountedCartItem = CartItems.builder()
//                .id("discounted-cart-item-id")
//                .carts(testCart)
//                .product(discountedProduct)
//                .quantity(1)
//                .build();
//
//        given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "test-provider-id"))
//                .willReturn(Optional.of(TestBuyerDTO.from(testBuyer)));
//        given(cartRepository.findByBuyerId("test-user-id"))
//                .willReturn(Optional.of(testCart));
//        given(cartItemRepository.findByCartsIdWithProduct("test-cart-id"))
//                .willReturn(Arrays.asList(discountedCartItem));
//
//        // when
//        CartResponse response = cartService.getCartByUserPrincipal(userPrincipal);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getItems()).hasSize(1);
//        // 할인은 상품 가격에 이미 반영되어 있다고 가정 (실제 비즈니스 로직에 따라 다를 수 있음)
//        assertThat(response.getTotalAmount()).isEqualTo(20000L);
//        assertThat(response.getTotalItemCount()).isEqualTo(1);
//    }
//}