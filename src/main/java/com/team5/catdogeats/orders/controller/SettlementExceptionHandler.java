package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 정산현황 관련 예외 처리 핸들러
 * 정산 API에서 발생하는 예외들을 처리하고 일관된 응답 형식을 제공
 */
@Slf4j
@RestControllerAdvice(assignableTypes = SettlementController.class)
public class SettlementExceptionHandler {

    /**
     * Validation 검증 실패 처리 (Spring 표준)
     * @Valid 어노테이션으로 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("정산 API 검증 실패 - {}", errorMessage);

        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE,
                        "/v1/sellers/settlements", fieldErrors));
    }

    /**
     * NoSuchElementException 처리 (Java 표준)
     * 판매자를 찾을 수 없거나 데이터가 존재하지 않는 경우
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoSuchElement(NoSuchElementException e) {
        log.warn("정산 API 리소스 없음 - Message: {}", e.getMessage());

        return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
    }

    /**
     * IllegalArgumentException 처리 (Java 표준)
     * 잘못된 입력값, 권한 없음, 날짜 범위 오류 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("정산 API 잘못된 요청 - Message: {}", e.getMessage());

        // 권한 관련 예외인지 확인
        if (e.getMessage() != null &&
                (e.getMessage().contains("권한") || e.getMessage().contains("판매자"))) {
            return ResponseEntity.status(ResponseCode.SELLER_ACCESS_DENIED.getStatus())
                    .body(ApiResponse.error(ResponseCode.SELLER_ACCESS_DENIED, e.getMessage()));
        }

        // 일반적인 잘못된 입력
        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /**
     * DateTimeParseException 처리 (Java 표준)
     * 날짜 형식이 잘못된 경우
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse<Object>> handleDateTimeParse(DateTimeParseException e) {
        log.warn("정산 API 날짜 형식 오류 - Message: {}", e.getMessage());

        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE,
                        "날짜 형식이 올바르지 않습니다. YYYY-MM-DD 또는 YYYY-MM 형식으로 입력해주세요"));
    }

    /**
     * IllegalStateException 처리 (Java 표준)
     * 시스템 상태 오류 (DB 연결 실패, 트랜잭션 오류 등)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException e) {
        log.error("정산 API 시스템 상태 오류 - Message: {}", e.getMessage());

        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                        "시스템 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요"));
    }

    /**
     * RuntimeException 처리 (Java 표준)
     * MyBatis 쿼리 오류, DB 연결 실패 등의 런타임 예외
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntime(RuntimeException e) {
        log.error("정산 API 런타임 오류", e);

        // MyBatis 관련 오류인지 확인
        if (e.getMessage() != null && e.getMessage().contains("Mapper")) {
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "데이터 조회 중 오류가 발생했습니다"));
        }

        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                        "정산 처리 중 오류가 발생했습니다"));
    }

    /**
     * 기타 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception e) {
        log.error("정산 API 예상치 못한 오류", e);

        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                        "서버 오류가 발생했습니다. 관리자에게 문의해주세요"));
    }
}