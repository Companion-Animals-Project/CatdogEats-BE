package com.team5.catdogeats.carts.exception;

import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.global.exception.ExpiredTokenException;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import com.team5.catdogeats.global.exception.TokenErrorException;
import com.team5.catdogeats.global.exception.WithdrawnAccountException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice(basePackages = "com.team5.catdogeats.carts")
@Slf4j
public class CartExceptionHandler {

    // 탈퇴 계정 접근 - 401 Unauthorized
    @ExceptionHandler(WithdrawnAccountException.class)
    public ResponseEntity<APIResponse<Void>> handleWithdrawnAccountException(WithdrawnAccountException ex) {
        log.warn("Cart API 탈퇴 계정 접근: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.error(ResponseCode.UNAUTHORIZED, "탈퇴한 계정입니다. 다시 가입해주세요."));
    }

    // 토큰 만료 - 401 Unauthorized
    @ExceptionHandler({ExpiredTokenException.class, InvalidTokenException.class})
    public ResponseEntity<APIResponse<Void>> handleTokenException(TokenErrorException ex) {
        log.warn("Cart API 토큰 오류: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.error(ResponseCode.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해주세요."));
    }

    // 일반 인증 실패 - 401 Unauthorized
    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class,
            TokenErrorException.class
    })
    public ResponseEntity<APIResponse<Void>> handleCartAuthenticationException(Exception ex) {
        log.warn("Cart API 인증 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.error(ResponseCode.UNAUTHORIZED, "장바구니 이용을 위해 로그인이 필요합니다."));
    }

    // 권한 없음 - 403 Forbidden
    // Cart 관련 보안 예외 (다른 사용자 장바구니 접근 등)
    @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
    public ResponseEntity<APIResponse<Void>> handleCartAccessDeniedException(Exception ex) {
        log.warn("Cart API 접근 권한 없음: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(APIResponse.error(ResponseCode.ACCESS_DENIED, "해당 장바구니에 접근할 권한이 없습니다."));
    }

    // 리소스 찾을 수 없음 - 404 Not Found
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<APIResponse<Void>> handleCartItemNotFoundException(NoSuchElementException ex) {
        log.warn("Cart 리소스 찾을 수 없음: {}", ex.getMessage());

        String userFriendlyMessage;
        if (ex.getMessage().contains("장바구니 아이템")) {
            userFriendlyMessage = "요청한 장바구니 상품을 찾을 수 없습니다.";
        } else if (ex.getMessage().contains("상품")) {
            userFriendlyMessage = "해당 상품을 찾을 수 없습니다.";
        } else if (ex.getMessage().contains("사용자")) {
            userFriendlyMessage = "사용자 정보를 찾을 수 없습니다.";
        } else {
            userFriendlyMessage = "요청한 정보를 찾을 수 없습니다.";
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, userFriendlyMessage));
    }

    // 잘못된 요청 - 400 Bad Request
    // 수량, 상품 ID 등 잘못된 파라미터
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Void>> handleCartBadRequestException(IllegalArgumentException ex) {
        log.warn("Cart API 잘못된 요청: {}", ex.getMessage());

        String userFriendlyMessage;
        if (ex.getMessage().contains("수량")) {
            if (ex.getMessage().contains("10개")) {
                userFriendlyMessage = "상품은 최대 10개까지만 장바구니에 담을 수 있습니다.";
            } else if (ex.getMessage().contains("1개")) {
                userFriendlyMessage = "상품 수량은 1개 이상이어야 합니다.";
            } else {
                userFriendlyMessage = "올바른 수량을 입력해주세요. (1-10개)";
            }
        } else if (ex.getMessage().contains("상품")) {
            userFriendlyMessage = "올바른 상품을 선택해주세요.";
        } else {
            userFriendlyMessage = "입력 정보를 확인해주세요.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, userFriendlyMessage));
    }

    // 서버 내부 오류 - 500 Internal Server Error
    // Cart 도메인에서 예상치 못한 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleCartGenericException(Exception ex) {
        log.error("Cart API 서버 내부 오류: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                        "장바구니 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationException(
            jakarta.validation.ConstraintViolationException ex) {
        log.warn("Cart 엔티티 검증 실패: {}", ex.getMessage());

        String userFriendlyMessage = "데이터 검증에 실패했습니다. 올바른 값을 입력해주세요.";

        // 구체적인 검증 오류 메시지 파싱
        if (ex.getMessage().contains("수량")) {
            userFriendlyMessage = "상품 수량은 1개 이상 10개 이하여야 합니다.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, userFriendlyMessage));
    }
}