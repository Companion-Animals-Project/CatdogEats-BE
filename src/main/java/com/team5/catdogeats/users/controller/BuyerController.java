// BuyerController.java
package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.dto.NameMaskingStatusResponseDto;
import com.team5.catdogeats.users.service.BuyerMaskingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/v1/buyers")
@RequiredArgsConstructor
@Tag(name = "Buyer", description = "구매자 관련 API")
public class BuyerController {

    private final BuyerMaskingService buyerMaskingService;

    @Operation(summary = "이름 마스킹 상태 조회", description = "구매자의 현재 이름 마스킹 설정 상태를 조회합니다.")
    @GetMapping("/mask")
    public ResponseEntity<ApiResponse<NameMaskingStatusResponseDto>> getNameMaskingStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            NameMaskingStatusResponseDto response = buyerMaskingService.getNameMaskingStatus(userPrincipal);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "이름 마스킹 상태 변경",
            description = "구매자의 이름 마스킹 설정을 변경합니다. 현재 상태가 활성화면 비활성화로  비활성화면 활성화로 변경됩니다.")
    @PostMapping("/mask")
    public ResponseEntity<ApiResponse<NameMaskingStatusResponseDto>> changeNameMaskingStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            NameMaskingStatusResponseDto response = buyerMaskingService.changeNameMaskingStatus(userPrincipal);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}