package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.external.DeliveryTrackingApiClient;
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
 *
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
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public boolean checkDeliveryStatus(String courierCode, String trackingNumber) {
        try {
            log.debug("배송 상태 확인 시작 - courier: {}, tracking: {}", courierCode, trackingNumber);

            // API 호출 제한 확인
            validateApiCallLimits(trackingNumber);

            // 스마트택배 API 호출
            DeliveryTrackingApiClient.DeliveryTrackingResponse response =
                    deliveryTrackingApiClient.getTrackingInfo(apiKey, courierCode, trackingNumber);

            // 호출 횟수 증가
            incrementCallCounts(trackingNumber);

            if (response == null) {
                log.warn("배송 추적 API 응답이 null - tracking: {}", trackingNumber);
                return false;
            }

            boolean isDelivered = response.isDelivered();
            log.info("배송 상태 확인 완료 - tracking: {}, status: {}, delivered: {}",
                    trackingNumber, response.getStatusMessage(), isDelivered);

            return isDelivered;

        } catch (Exception e) {
            log.error("배송 상태 확인 실패 - courier: {}, tracking: {}, error: {}",
                    courierCode, trackingNumber, e.getMessage());
            throw new DeliveryTrackingApiException("배송 상태 확인 중 오류가 발생했습니다", e);
        }
    }

    @Override
    @Retryable(
            value = {Exception.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 1000)
    )
    public ValidationResult validateTrackingNumber(String courierCode, String trackingNumber) {
        try {
            log.debug("운송장 번호 유효성 검증 시작 - courier: {}, tracking: {}",
                    courierCode, trackingNumber);

            // API 호출 제한 확인 (유효성 검증은 좀 더 관대하게)
            if (!canMakeApiCall()) {
                log.warn("API 호출 제한 초과 - 유효성 검증 생략");
                return ValidationResult.skipped("API 호출 제한으로 검증을 생략했습니다");
            }

            DeliveryTrackingApiClient.DeliveryValidationResponse response =
                    deliveryTrackingApiClient.validateInvoice(apiKey, courierCode, trackingNumber);

            incrementDailyCallCount();

            if (response == null) {
                return ValidationResult.error("API 응답이 없습니다");
            }

            if (response.isValid()) {
                log.info("운송장 번호 유효성 검증 성공 - tracking: {}", trackingNumber);
                return ValidationResult.success("운송장 번호가 유효합니다");
            } else {
                log.warn("운송장 번호 유효성 검증 실패 - tracking: {}, message: {}",
                        trackingNumber, response.message());
                return ValidationResult.invalid(response.message());
            }

        } catch (Exception e) {
            log.error("운송장 번호 유효성 검증 중 오류 - courier: {}, tracking: {}, error: {}",
                    courierCode, trackingNumber, e.getMessage());
            return ValidationResult.error("유효성 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public ApiCallStatus getApiCallStatus() {
        resetCountersIfNewDay();
        return new ApiCallStatus(
                dailyCallCount.get(),
                dailyTotalLimit,
                trackingCallCounts.size(),
                lastResetDate
        );
    }

    /**
     * API 호출 제한 확인
     */
    private void validateApiCallLimits(String trackingNumber) {
        // 날짜가 바뀌면 카운터 초기화
        resetCountersIfNewDay();

        // 전체 일일 호출 제한 확인
        if (dailyCallCount.get() >= dailyTotalLimit) {
            throw new DeliveryTrackingApiException("일일 API 호출 제한을 초과했습니다");
        }

        // 운송장별 일일 호출 제한 확인
        String trackingKey = formatTrackingKey(trackingNumber);
        AtomicInteger trackingCount = trackingCallCounts.computeIfAbsent(
                trackingKey, k -> new AtomicInteger(0));

        if (trackingCount.get() >= dailyLimitPerTracking) {
            throw new DeliveryTrackingApiException(
                    String.format("운송장 %s의 일일 조회 제한(%d회)을 초과했습니다",
                            trackingNumber, dailyLimitPerTracking));
        }
    }

    /**
     * API 호출 가능 여부 확인 (유효성 검증용)
     */
    private boolean canMakeApiCall() {
        resetCountersIfNewDay();
        return dailyCallCount.get() < dailyTotalLimit;
    }

    /**
     * API 호출 횟수 증가
     */
    private void incrementCallCounts(String trackingNumber) {
        incrementDailyCallCount();

        String trackingKey = formatTrackingKey(trackingNumber);
        trackingCallCounts.computeIfAbsent(trackingKey, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private void incrementDailyCallCount() {
        int newCount = dailyCallCount.incrementAndGet();
        log.debug("API 호출 횟수 증가 - 오늘: {}/{}", newCount, dailyTotalLimit);
    }

    /**
     * 날짜 변경시 카운터 초기화
     */
    private void resetCountersIfNewDay() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!today.equals(lastResetDate)) {
            log.info("날짜 변경 감지 - API 호출 카운터 초기화: {} -> {}", lastResetDate, today);
            dailyCallCount.set(0);
            trackingCallCounts.clear();
            lastResetDate = today;
        }
    }

    /**
     * 운송장 번호 키 포맷팅
     */
    private String formatTrackingKey(String trackingNumber) {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ":" + trackingNumber;
    }
}