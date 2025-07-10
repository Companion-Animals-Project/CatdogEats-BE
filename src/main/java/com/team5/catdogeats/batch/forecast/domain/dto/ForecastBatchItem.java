package com.team5.catdogeats.batch.forecast.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 수요예측 배치 처리용 아이템 DTO
 * Spring Batch의 ItemReader, ItemProcessor, ItemWriter에서 사용
 */
@Getter
@Builder
@ToString
public class ForecastBatchItem {

    /**
     * 판매자 ID
     */
    private final String sellerId;

    /**
     * 판매자명 (업체명)
     */
    private final String vendorName;

    /**
     * 판매자 활성화 상태
     */
    private final boolean isActive;

    /**
     * 판매자 가입일
     */
    private final LocalDate joinDate;

    /**
     * 최근 주문 접수일 (활성도 판단용)
     */
    private final LocalDate lastOrderDate;

    /**
     * 등록된 상품 수
     */
    private final int totalProductCount;

    /**
     * 활성 상품 수 (재고 있는 상품)
     */
    private final int activeProductCount;

    /**
     * 처리 결과 필드들 (Processor에서 설정)
     */
    private ProcessingResult processingResult;

    /**
     * 배치 처리 결과
     */
    @Getter
    @Builder
    @ToString
    public static class ProcessingResult {
        /**
         * 처리 성공 여부
         */
        private final boolean success;

        /**
         * 예측 완료된 상품 수
         */
        private final int processedProductCount;

        /**
         * 처리 시간 (밀리초)
         */
        private final long processingTimeMs;

        /**
         * 오류 메시지 (실패시)
         */
        private final String errorMessage;

        /**
         * 신뢰도 점수 평균
         */
        private final Double averageConfidenceScore;

        /**
         * 재고 부족 상품 수
         */
        private final int shortageProductCount;

        /**
         * 처리 시작 시간
         */
        private final LocalDate processedDate;

        /**
         * 성공 결과 생성
         */
        public static ProcessingResult success(int processedProductCount, long processingTimeMs,
                                               Double averageConfidenceScore, int shortageProductCount) {
            return ProcessingResult.builder()
                    .success(true)
                    .processedProductCount(processedProductCount)
                    .processingTimeMs(processingTimeMs)
                    .averageConfidenceScore(averageConfidenceScore)
                    .shortageProductCount(shortageProductCount)
                    .processedDate(LocalDate.now())
                    .build();
        }

        /**
         * 실패 결과 생성
         */
        public static ProcessingResult failure(String errorMessage, long processingTimeMs) {
            return ProcessingResult.builder()
                    .success(false)
                    .processedProductCount(0)
                    .processingTimeMs(processingTimeMs)
                    .errorMessage(errorMessage)
                    .averageConfidenceScore(0.0)
                    .shortageProductCount(0)
                    .processedDate(LocalDate.now())
                    .build();
        }

        /**
         * 건너뛰기 결과 생성 (데이터 부족 등)
         */
        public static ProcessingResult skipped(String reason) {
            return ProcessingResult.builder()
                    .success(true)
                    .processedProductCount(0)
                    .processingTimeMs(0L)
                    .errorMessage(reason)
                    .averageConfidenceScore(0.0)
                    .shortageProductCount(0)
                    .processedDate(LocalDate.now())
                    .build();
        }
    }

    /**
     * 처리 결과 설정
     */
    public void setProcessingResult(ProcessingResult result) {
        this.processingResult = result;
    }

    /**
     * 배치 처리 대상 여부 확인
     */
    public boolean isEligibleForProcessing() {
        // 활성 판매자이고 상품이 있는 경우
        return isActive && activeProductCount > 0;
    }

    /**
     * 최근 활동 여부 확인 (30일 내 주문)
     */
    public boolean hasRecentActivity() {
        if (lastOrderDate == null) return false;
        return lastOrderDate.isAfter(LocalDate.now().minusDays(30));
    }

    /**
     * 검증 - 필수 필드 확인
     */
    public boolean isValid() {
        return sellerId != null && !sellerId.trim().isEmpty() &&
                vendorName != null && !vendorName.trim().isEmpty();
    }

    /**
     * 예측 우선순위 점수 계산
     * 최근 활동도, 상품 수 등을 고려한 우선순위
     */
    public int getPriorityScore() {
        int score = 0;

        // 활성 상품 수에 따른 점수 (최대 50점)
        score += Math.min(activeProductCount * 2, 50);

        // 최근 활동도에 따른 점수 (최대 30점)
        if (hasRecentActivity()) {
            long daysSinceLastOrder = java.time.temporal.ChronoUnit.DAYS.between(lastOrderDate, LocalDate.now());
            score += Math.max(0, 30 - (int)(daysSinceLastOrder / 2));
        }

        // 기본 점수 (활성 판매자인 경우 20점)
        if (isActive) {
            score += 20;
        }

        return score;
    }

    /**
     * 배치 아이템 요약 정보
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("판매자: ").append(vendorName)
                .append(" (").append(sellerId).append(")")
                .append(", 활성상품: ").append(activeProductCount)
                .append(", 우선순위: ").append(getPriorityScore());

        if (processingResult != null) {
            summary.append(", 처리결과: ")
                    .append(processingResult.success ? "성공" : "실패")
                    .append(" (").append(processingResult.processedProductCount).append("개 예측완료)");
        }

        return summary.toString();
    }
}