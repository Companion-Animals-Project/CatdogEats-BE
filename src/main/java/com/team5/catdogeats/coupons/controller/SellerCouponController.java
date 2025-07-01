package com.team5.catdogeats.coupons.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.SellerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerDeleteCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerModifyCouponRequestDTO;
import com.team5.catdogeats.coupons.service.SellerCouponListService;
import com.team5.catdogeats.coupons.service.SellerCouponService;
import com.team5.catdogeats.coupons.service.SellerUpdateCouponService;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sellers/coupons")
@Tag(name = "판매자 쿠폰", description = "판매자 전용 쿠폰 api")
public class SellerCouponController {
    private final SellerCouponService sellerCouponService;
    private final SellerUpdateCouponService sellerUpdateCouponService;
    private final SellerCouponListService sellerCouponListService;


    @GetMapping("/{vendorName}")
    public ResponseEntity<ApiResponse<List<SellerCouponListResponseDTO>>> getCoupons(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                                     @PathVariable String vendorName,
                                                                                     @RequestParam(defaultValue = "0") int page) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, sellerCouponListService.getSellerCouponsWithVendorName(vendorName, page, 10)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerCouponListResponseDTO>>> getCoupons(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                               @RequestParam(defaultValue = "0") int page,
                                                                               @RequestParam(defaultValue = "10") int size) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {

            List<SellerCouponListResponseDTO> coupons = sellerCouponListService.getSellerCoupons(userPrincipal, page, size);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, coupons));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCoupon(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                          @Valid @RequestBody SellerCreateCouponRequestDTO dto) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            sellerCouponService.createCoupon(userPrincipal, dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ResponseCode.DUPLICATE_COUPON_CODE));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateCoupon(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                          @Valid @RequestBody SellerModifyCouponRequestDTO dto) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            sellerUpdateCouponService.modifierCoupon(userPrincipal, dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                          @Valid @RequestBody SellerDeleteCouponRequestDTO dto) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {
            sellerUpdateCouponService.deleteCoupon(userPrincipal, dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

}
