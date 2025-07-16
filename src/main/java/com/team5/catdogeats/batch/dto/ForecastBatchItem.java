package com.team5.catdogeats.batch.dto;

import java.time.LocalDate;

/**
 * 수요예측 배치 처리용 아이템 DTO
 * Spring Batch의 ItemReader, ItemProcessor, ItemWriter에서 사용
 */
public record ForecastBatchItem(
        /**
         * 판매자 ID
         */
        String sellerId,

        /**
         * 판매자명 (업체명)
         */
        String vendorName,

        /**
         * 판매자 활성화 상태
         */
        Boolean isActive,

        /**
         * 판매자 가입일
         */
        LocalDate joinDate,

        /**
         * 최근 주문 접수일 (활성도 판단용)
         */
        LocalDate lastOrderDate,

        /**
         * 등록된 상품 수
         */
        Integer totalProductCount,

        /**
         * 활성 상품 수 (재고 있는 상품)
         */
        Integer activeProductCount,

        /**
         * 처리 결과 필드들 (Processor에서 설정)
         */
        ProcessingResult processingResult
) {

    /**
     * ProcessingResult 없는 생성자 (ItemReader용)
     */
    public ForecastBatchItem(String sellerId, String vendorName, Boolean isActive,
                             LocalDate joinDate, LocalDate lastOrderDate,
                             Integer totalProductCount, Integer activeProductCount) {
        this(sellerId, vendorName, isActive, joinDate, lastOrderDate,
                totalProductCount, activeProductCount, null);
    }

    /**
     * 기본 유효성 검증
     */
    public boolean isValid() {
        return sellerId != null && !sellerId.trim().isEmpty() &&
                vendorName != null && !vendorName.trim().isEmpty() &&
                totalProductCount != null && totalProductCount >= 0 &&
                activeProductCount != null && activeProductCount >= 0;
    }

    /**
     * 처리 대상 여부 확인
     */
    public boolean isEligibleForProcessing() {
        return isActive != null && isActive &&
                activeProductCount != null && activeProductCount > 0;
    }

    /**
     * 최근 활동 여부 확인 (30일 기준)
     */
    public boolean hasRecentActivity() {
        if (lastOrderDate == null) {
            return false;
        }
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        return lastOrderDate.isAfter(thirtyDaysAgo);
    }

    /**
     * 우선순위 점수 계산 (0-100)
     */
    public int getPriorityScore() {
        int score = 0;

        // 활성 상품 수에 따른 점수 (최대 40점)
        if (activeProductCount != null) {
            score += Math.min(activeProductCount * 2, 40);
        }

        // 최근 활동에 따른 점수 (최대 30점)
        if (hasRecentActivity()) {
            score += 30;
        }

        // 총 상품 수에 따른 점수 (최대 20점)
        if (totalProductCount != null) {
            score += Math.min(totalProductCount, 20);
        }

        // 기본 점수 (최대 10점)
        if (isEligibleForProcessing()) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 처리 결과 설정 (Processor에서 사용)
     */
    public ForecastBatchItem withProcessingResult(ProcessingResult result) {
        return new ForecastBatchItem(
                sellerId, vendorName, isActive, joinDate, lastOrderDate,
                totalProductCount, activeProductCount, result
        );
    }

    /**
     * 배치 처리 결과 Record
     */
    public record ProcessingResult(
            /**
             * 처리 성공 여부
             */
            boolean success,

            /**
             * 예측 완료된 상품 수
             */
            int processedProductCount,

            /**
             * 처리 시간 (밀리초)
             */
            long processingTimeMs,

            /**
             * 오류 메시지 (실패시)
             */
            String errorMessage,

            /**
             * 신뢰도 점수 평균
             */
            Double averageConfidenceScore,

            /**
             * 재고 부족 상품 수
             */
            int shortageProductCount,

            /**
             * 처리 시작 시간
             */
            LocalDate processedDate
    ) {

        /**
         * 성공 결과 생성
         */
        public static ProcessingResult success(int processedProductCount, long processingTimeMs,
                                               Double averageConfidenceScore, int shortageProductCount) {
            return new ProcessingResult(
                    true, processedProductCount, processingTimeMs, null,
                    averageConfidenceScore, shortageProductCount, LocalDate.now()
            );
        }

        /**
         * 실패 결과 생성
         */
        public static ProcessingResult failure(String errorMessage, long processingTimeMs) {
            return new ProcessingResult(
                    false, 0, processingTimeMs, errorMessage,
                    null, 0, LocalDate.now()
            );
        }

        /**
         * 스킵 결과 생성
         */
        public static ProcessingResult skipped(String reason) {
            return new ProcessingResult(
                    false, 0, 0L, "SKIPPED: " + reason,
                    null, 0, LocalDate.now()
            );
        }


        /**
         * 스킵된 처리인지 확인
         */
        public boolean isSkipped() {
            return errorMessage != null && errorMessage.startsWith("SKIPPED:");
        }
    }
}