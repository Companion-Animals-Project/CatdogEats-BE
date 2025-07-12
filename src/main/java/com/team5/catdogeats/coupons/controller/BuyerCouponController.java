package com.team5.catdogeats.coupons.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.dto.BuyerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.CouponFilterType;
import com.team5.catdogeats.coupons.exception.DuplicateCouponException;
import com.team5.catdogeats.coupons.service.BuyerCouponService;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/buyers/coupons")
@Tag(name = "구매자 쿠폰 관리", description = "구매자 쿠폰 관련 API입니다.")
public class BuyerCouponController {
    private final BuyerCouponService buyerCouponService;

    @GetMapping
    public ResponseEntity<APIResponse<BuyerCouponListResponseDTO>> getCoupons(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                              @RequestParam CouponFilterType filter,
                                                                              @RequestParam(defaultValue = "0") int page) {
        try {
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, buyerCouponService.getBuyerCoupons(userPrincipal,filter,page,10)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }


    @PostMapping
    public ResponseEntity<APIResponse<Void>> createCoupon(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                          @Valid @RequestBody BuyerCreateCouponRequestDTO dto) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {
            buyerCouponService.createCoupon(userPrincipal, dto);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE));
        } catch (DuplicateCouponException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(APIResponse.error(ResponseCode.DUPLICATE_COUPON_CODE));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }
}
