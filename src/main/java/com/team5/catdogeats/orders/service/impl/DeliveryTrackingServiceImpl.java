package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.external.DeliveryTrackingApiClient;
import com.team5.catdogeats.orders.external.DeliveryTrackingApiConfiguration.SmartCourierApiException;
import com.team5.catdogeats.orders.dto.response.TrackingValidationResponse;
import com.team5.catdogeats.orders.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 배송 추적 서비스 구현체
 * 스마트택배 배송 추적 API를 호출하여 배송 상태를 확인하는 서비스
 * API 제한사항 관리:
 * - 프리티어: 동일 운송장 일 최대 10건 조회 제한
 * - 일일 전체 호출 제한: 1000건
 * - 8시간마다 배치 실행으로 제한 준수
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingServiceImpl implements DeliveryTrackingService {

    private final DeliveryTrackingApiClient deliveryTrackingApiClient;

    @Value("${smart-courier.api.key}")
    private String apiKey;

    @Value("${smart-courier.api.rate-limit.daily-limit-per-tracking:10}")
    private int dailyLimitPerTracking;

    @Value("${smart-courier.api.rate-limit.daily-total-limit:1000}")
    private int dailyTotalLimit;

    // 일일 API 호출 횟수 추적 (메모리 기반)
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);
    private String lastResetDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

    // 운송장별 일일 호출 횟수 추적
    private final ConcurrentHashMap<String, AtomicInteger> trackingCallCounts = new ConcurrentHashMap<>();

    @Override
    @Retryable(
            retryFor = {SmartCourierApiException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public boolean checkDeliveryStatus(String courierCode, String trackingNumber) {
        try {
            log.debug("배송 상태 확인 시작 - courierCode: {}, trackingNumber: {}", courierCode, trackingNumber);

            // API 호출 제한 확인
            validateApiCallLimits(trackingNumber);

            // 스마트택배 API 호출
            TrackingValidationResponse response = deliveryTrackingApiClient.validateTrackingNumber(
                    apiKey, courierCode, trackingNumber);
            // 호출 횟수 증가
            incrementCallCount(trackingNumber);

            // 응답 검증
            if (!response.isSuccess()) {
                log.warn("배송 상태 조회 실패 - trackingNumber: {}, message: {}",
                        trackingNumber, response.message());
                throw new DeliveryTrackingApiException("배송 상태 조회 실패: " + response.message());
            }

            boolean isDelivered = response.isValidTrackingNumber() && "배송완료".equals(response.level());
            log.debug("배송 상태 확인 완료 - trackingNumber: {}, isDelivered: {}", trackingNumber, isDelivered);

            return isDelivered;

        } catch (SmartCourierApiException e) {
            if (e.isRetryable()) {
                log.warn("재시도 가능한 API 오류 - trackingNumber: {}, error: {}", trackingNumber, e.getMessage());
                throw e; // 재시도를 위해 예외를 다시 던짐
            } else {
                log.error("재시도 불가능한 API 오류 - trackingNumber: {}, error: {}", trackingNumber, e.getMessage());
                throw new DeliveryTrackingApiException("배송 상태 확인 실패", e);
            }
        } catch (Exception e) {
            log.error("배송 상태 확인 중 예상치 못한 오류 - trackingNumber: {}", trackingNumber, e);
            throw new DeliveryTrackingApiException("배송 상태 확인 중 오류 발생", e);
        }
    }

    @Override
    @Retryable(
            retryFor = {SmartCourierApiException.class},
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public ValidationResult validateTrackingNumber(String courierCode, String trackingNumber) {
        try {
            log.debug("운송장 번호 검증 시작 - courierCode: {}, trackingNumber: {}", courierCode, trackingNumber);

            // 기본 형식 검증
            if (!isValidFormat(courierCode, trackingNumber)) {
                return ValidationResult.invalid("운송장 번호 형식이 올바르지 않습니다");
            }

            // API 호출 제한 확인
            try {
                validateApiCallLimits(trackingNumber);
            } catch (Exception e) {
                log.warn("API 제한으로 인한 기본 검증만 수행 - trackingNumber: {}", trackingNumber);
                return ValidationResult.skipped("API 제한으로 기본 형식 검증만 수행했습니다");
            }

            // 스마트택배 API로 실제 검증
            TrackingValidationResponse response = deliveryTrackingApiClient.validateTrackingNumber(
                    apiKey, courierCode, trackingNumber);

            // 호출 횟수 증가
            incrementCallCount(trackingNumber);

            // 응답 처리
            if (!response.isSuccess()) {
                log.warn("운송장 검증 API 실패 - trackingNumber: {}, message: {}",
                        trackingNumber, response.message());
                return ValidationResult.error("API 검증 실패: " + response.message());
            }

            boolean isValid = response.isValidTrackingNumber();
            String message = response.getValidationMessage();

            log.debug("운송장 번호 검증 완료 - trackingNumber: {}, isValid: {}", trackingNumber, isValid);

            return isValid ? ValidationResult.success(message) : ValidationResult.invalid(message);

        } catch (SmartCourierApiException e) {
            if (e.isRetryable()) {
                log.warn("재시도 가능한 검증 API 오류 - trackingNumber: {}", trackingNumber);
                throw e; // 재시도를 위해 예외를 다시 던짐
            } else {
                log.warn("재시도 불가능한 검증 API 오류 - trackingNumber: {}, 기본 검증으로 대체", trackingNumber);
                // API 실패 시 기본 형식 검증 결과로 대체
                boolean isValidFormat = isValidFormat(courierCode, trackingNumber);
                return isValidFormat
                        ? ValidationResult.success("기본 형식 검증 통과 (API 검증 실패)")
                        : ValidationResult.invalid("운송장 번호 형식이 올바르지 않습니다");
            }
        } catch (Exception e) {
            log.error("운송장 번호 검증 중 예상치 못한 오류 - trackingNumber: {}", trackingNumber, e);
            // 예외 발생 시 기본 형식 검증으로 대체
            boolean isValidFormat = isValidFormat(courierCode, trackingNumber);
            return isValidFormat
                    ? ValidationResult.success("기본 형식 검증 통과 (API 오류)")
                    : ValidationResult.invalid("운송장 번호 형식이 올바르지 않습니다");
        }
    }

    /**
     * API 호출 제한 검증
     */
    private void validateApiCallLimits(String trackingNumber) {
        resetCountsIfNewDay();

        // 전체 일일 호출 제한 확인
        if (dailyCallCount.get() >= dailyTotalLimit) {
            throw new DeliveryTrackingApiException("일일 API 호출 제한을 초과했습니다");
        }

        // 운송장별 일일 호출 제한 확인
        AtomicInteger trackingCount = trackingCallCounts.computeIfAbsent(
                trackingNumber, k -> new AtomicInteger(0));

        if (trackingCount.get() >= dailyLimitPerTracking) {
            throw new DeliveryTrackingApiException(
                    String.format("운송장 %s의 일일 조회 제한(%d회)을 초과했습니다",
                            trackingNumber, dailyLimitPerTracking));
        }
    }

    /**
     * API 호출 횟수 증가
     */
    private void incrementCallCount(String trackingNumber) {
        dailyCallCount.incrementAndGet();
        trackingCallCounts.computeIfAbsent(trackingNumber, k -> new AtomicInteger(0))
                .incrementAndGet();

        log.debug("API 호출 횟수 증가 - 전체: {}/{}, 운송장: {}/{}",
                dailyCallCount.get(), dailyTotalLimit,
                trackingCallCounts.get(trackingNumber).get(), dailyLimitPerTracking);
    }

    /**
     * 새로운 날짜 시 호출 횟수 초기화
     */
    private void resetCountsIfNewDay() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        if (!currentDate.equals(lastResetDate)) {
            dailyCallCount.set(0);
            trackingCallCounts.clear();
            lastResetDate = currentDate;

            log.info("API 호출 횟수 초기화 - 날짜: {}", currentDate);
        }
    }

    /**
     * 기본 운송장 번호 형식 검증
     */
    private boolean isValidFormat(String courierCode, String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }

        String normalized = trackingNumber.trim().replaceAll("-", "");

        return switch (courierCode) {
            case "01" -> normalized.matches("^[0-9]{13}$"); // 우체국택배: 13자리 숫자
            case "04" -> normalized.matches("^[0-9]{10,12}$"); // CJ대한통운: 10-12자리 숫자
            case "05" -> normalized.matches("^[0-9]{12}$"); // 한진택배: 12자리 숫자
            case "06" -> normalized.matches("^[0-9]{11,12}$"); // 로젠택배: 11-12자리 숫자
            case "08" -> normalized.matches("^[0-9]{12,13}$"); // 롯데택배: 12-13자리 숫자
            default -> {
                log.warn("지원하지 않는 택배사 코드: {}", courierCode);
                yield false;
            }
        };
    }
}