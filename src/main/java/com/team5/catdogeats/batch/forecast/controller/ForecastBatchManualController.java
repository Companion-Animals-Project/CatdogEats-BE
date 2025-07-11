package com.team5.catdogeats.batch.forecast.controller;

import com.team5.catdogeats.batch.forecast.scheduler.ForecastBatchScheduler;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchConcurrencyService;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchExecutionService.BatchExecutionResult;
import com.team5.catdogeats.batch.forecast.domain.ForecastBatchExecutionStatus;
import com.team5.catdogeats.forecast.service.DailySalesAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 수요예측 배치 수동 실행 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/admin/forecast-batch")
@RequiredArgsConstructor
@Tag(name = "수요예측 배치 관리", description = "수요예측 배치 수동 실행 및 모니터링 API")
public class ForecastBatchManualController {

    private final ForecastBatchScheduler forecastBatchScheduler;
    private final ForecastBatchConcurrencyService batchConcurrencyService;
    private final DailySalesAggregationService dailySalesAggregationService;

    // ================================
    // 기본 배치 실행 API
    // ================================

    /**
     * 일별 판매 집계 배치 수동 실행 (어제 데이터)
     */
    @PostMapping("/aggregation/manual")
    @Operation(summary = "일별 판매 집계 배치 수동 실행", description = "어제 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationBatch() {
        try {
            log.info("수동 일별 집계 배치 요청");

            BatchExecutionResult result = forecastBatchScheduler.runAggregationJobManually();

            Map<String, Object> response = Map.of(
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "executionSummary", result.getExecutionSummary(),
                    "batchType", "aggregation"
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("수동 일별 집계 배치 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "배치 실행 실패: " + e.getMessage(),
                    "batchType", "aggregation"
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ================================
    // 기간별 집계 API (새로 추가)
    // ================================

    /**
     * 특정 날짜 판매 집계 수동 실행
     */
    @PostMapping("/aggregation/date/{targetDate}")
    @Operation(summary = "특정 날짜 판매 집계 수동 실행", description = "지정된 날짜의 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationForDate(
            @Parameter(description = "집계할 날짜 (YYYY-MM-DD)", example = "2024-01-15")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {

        try {
            log.info("특정 날짜 집계 배치 요청 - targetDate: {}", targetDate);

            // 미래 날짜 검증
            if (targetDate.isAfter(LocalDate.now())) {
                Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "미래 날짜는 집계할 수 없습니다: " + targetDate,
                        "batchType", "date_aggregation"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 실제 집계 실행
            int aggregatedRecords = dailySalesAggregationService.aggregateDailySales(targetDate);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "특정 날짜 집계 완료",
                    "targetDate", targetDate.toString(),
                    "aggregatedRecords", aggregatedRecords,
                    "batchType", "date_aggregation"
            );

            log.info("특정 날짜 집계 완료 - targetDate: {}, 처리건수: {}", targetDate, aggregatedRecords);
            return ResponseEntity.ok(response);

        } catch (DateTimeParseException e) {
            log.error("잘못된 날짜 형식 - targetDate: {}", targetDate, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "잘못된 날짜 형식입니다. YYYY-MM-DD 형식을 사용하세요.",
                    "batchType", "date_aggregation"
            );
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("특정 날짜 집계 실패 - targetDate: {}", targetDate, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "집계 실행 실패: " + e.getMessage(),
                    "targetDate", targetDate.toString(),
                    "batchType", "date_aggregation"
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 기간별 판매 집계 수동 실행 (startDate ~ endDate)
     */
    @PostMapping("/aggregation/range")
    @Operation(summary = "기간별 판매 집계 수동 실행", description = "지정된 기간의 판매 데이터를 일별로 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationForDateRange(
            @Parameter(description = "시작 날짜 (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2024-01-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "기존 데이터 덮어쓰기 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean overwrite) {

        try {
            log.info("기간별 집계 배치 요청 - startDate: {}, endDate: {}, overwrite: {}",
                    startDate, endDate, overwrite);

            // 입력값 검증
            ValidationResult validation = validateDateRange(startDate, endDate);
            if (!validation.isValid()) {
                Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", validation.getErrorMessage(),
                        "batchType", "range_aggregation"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 기간별 집계 실행
            RangeAggregationResult result = executeRangeAggregation(startDate, endDate, overwrite);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "기간별 집계 완료");
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("totalDays", result.getTotalDays());
            response.put("successDays", result.getSuccessDays());
            response.put("skippedDays", result.getSkippedDays());
            response.put("failedDays", result.getFailedDays());
            response.put("totalRecords", result.getTotalRecords());
            response.put("details", result.getDetails());
            response.put("batchType", "range_aggregation");

            log.info("기간별 집계 완료 - 총 {}일, 성공 {}일, 스킵 {}일, 실패 {}일, 총 처리건수: {}",
                    result.getTotalDays(), result.getSuccessDays(), result.getSkippedDays(),
                    result.getFailedDays(), result.getTotalRecords());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("기간별 집계 실패 - startDate: {}, endDate: {}", startDate, endDate, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "기간별 집계 실행 실패: " + e.getMessage(),
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "batchType", "range_aggregation"
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 최근 N일 판매 집계 수동 실행
     */
    @PostMapping("/aggregation/recent-days/{days}")
    @Operation(summary = "최근 N일 판매 집계 수동 실행", description = "최근 N일간의 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationForRecentDays(
            @Parameter(description = "최근 일수 (1-30)", example = "7")
            @PathVariable int days,

            @Parameter(description = "기존 데이터 덮어쓰기 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean overwrite) {

        try {
            log.info("최근 {}일 집계 배치 요청 - overwrite: {}", days, overwrite);

            // 일수 검증
            if (days < 1 || days > 30) {
                Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "일수는 1-30 사이의 값이어야 합니다: " + days,
                        "batchType", "recent_days_aggregation"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 날짜 범위 계산 (오늘 제외, 어제부터 역산)
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(days - 1);

            log.info("최근 {}일 집계 범위 - startDate: {}, endDate: {}", days, startDate, endDate);

            // 기간별 집계 실행
            RangeAggregationResult result = executeRangeAggregation(startDate, endDate, overwrite);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "최근 " + days + "일 집계 완료");
            response.put("days", days);
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("successDays", result.getSuccessDays());
            response.put("skippedDays", result.getSkippedDays());
            response.put("failedDays", result.getFailedDays());
            response.put("totalRecords", result.getTotalRecords());
            response.put("details", result.getDetails());
            response.put("batchType", "recent_days_aggregation");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("최근 {}일 집계 실패", days, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "최근 " + days + "일 집계 실행 실패: " + e.getMessage(),
                    "days", days,
                    "batchType", "recent_days_aggregation"
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ================================
    // 기존 배치 실행 API (유지)
    // ================================

    /**
     * 수요예측 실행 배치 수동 실행
     */
    @PostMapping("/prediction/manual")
    @Operation(summary = "수요예측 실행 배치 수동 실행", description = "모든 활성 판매자의 수요예측을 실행합니다")
    public ResponseEntity<Map<String, Object>> runPredictionBatch() {
        try {
            log.info("수동 수요예측 배치 요청");

            BatchExecutionResult result = forecastBatchScheduler.runPredictionJobManually();

            Map<String, Object> response = Map.of(
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "executionSummary", result.getExecutionSummary(),
                    "batchType", "prediction"
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("수동 수요예측 배치 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "배치 실행 실패: " + e.getMessage(),
                    "batchType", "prediction"
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 데이터 정리 배치 수동 실행
     */
    @PostMapping("/cleanup/manual")
    @Operation(summary = "데이터 정리 배치 수동 실행", description = "오래된 예측 데이터를 정리합니다")
    public ResponseEntity<Map<String, Object>> runCleanupBatch() {
        try {
            log.info("수동 데이터 정리 배치 요청");

            BatchExecutionResult result = forecastBatchScheduler.runCleanupJobManually();

            Map<String, Object> response = Map.of(
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "executionSummary", result.getExecutionSummary(),
                    "batchType", "cleanup"
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("수동 데이터 정리 배치 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "배치 실행 실패: " + e.getMessage(),
                    "batchType", "cleanup"
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 전체 배치 작업 수동 실행 (집계 + 예측)
     */
    @PostMapping("/full/manual")
    @Operation(summary = "전체 배치 작업 수동 실행", description = "일별 집계 + 수요예측을 순차적으로 실행합니다")
    public ResponseEntity<Map<String, Object>> runFullBatch() {
        try {
            log.info("전체 수요예측 배치 작업 수동 실행 시작");

            // 1. 일별 집계 실행
            BatchExecutionResult aggregationResult = forecastBatchScheduler.runAggregationJobManually();

            if (!aggregationResult.isSuccess()) {
                log.error("일별 집계 실패로 전체 배치 중단");
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "일별 집계 실패: " + aggregationResult.getMessage(),
                        "batchType", "full",
                        "failedStep", "aggregation"
                );
                return ResponseEntity.status(500).body(response);
            }

            // 2. 수요예측 실행
            BatchExecutionResult predictionResult = forecastBatchScheduler.runPredictionJobManually();

            Map<String, Object> response = Map.of(
                    "success", predictionResult.isSuccess(),
                    "message", predictionResult.isSuccess() ? "전체 배치 작업 완료" : "수요예측 실패: " + predictionResult.getMessage(),
                    "batchType", "full",
                    "aggregationSummary", aggregationResult.getExecutionSummary(),
                    "predictionSummary", predictionResult.getExecutionSummary()
            );

            if (predictionResult.isSuccess()) {
                log.info("전체 수요예측 배치 작업 완료");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("전체 수요예측 배치 작업 수동 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "전체 배치 작업 실행 실패: " + e.getMessage(),
                    "batchType", "full"
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ================================
    // 배치 상태 및 설정 API (기존 유지)
    // ================================

    /**
     * 배치 상태 확인
     */
    @GetMapping("/status")
    @Operation(summary = "배치 상태 확인", description = "수요예측 배치 관련 상태를 확인합니다")
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        try {
            List<ForecastBatchExecutionStatus> allStatuses = batchConcurrencyService.getAllBatchStatuses();

            Map<String, Object> statusMap = Map.of(
                    "currentTime", java.time.LocalDateTime.now().toString(),
                    "batchEnabled", true,
                    "batches", allStatuses.stream().map(status -> Map.of(
                            "batchName", status.getBatchName(),
                            "status", status.getExecutionStatus().getDescription(),
                            "lastExecutionId", status.getLastExecutionId() != null ? status.getLastExecutionId() : "없음",
                            "startedAt", status.getStartedAt() != null ? status.getStartedAt().toString() : "없음",
                            "finishedAt", status.getFinishedAt() != null ? status.getFinishedAt().toString() : "없음",
                            "executionTimeSeconds", status.getExecutionTimeSeconds(),
                            "summary", status.getStatusSummary()
                    )).toList()
            );

            return ResponseEntity.ok(statusMap);

        } catch (Exception e) {
            log.error("배치 상태 확인 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "상태 확인 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 특정 배치 상태 조회
     */
    @GetMapping("/status/{batchName}")
    @Operation(summary = "특정 배치 상태 조회", description = "특정 배치의 상세 상태를 조회합니다")
    public ResponseEntity<Map<String, Object>> getSpecificBatchStatus(@PathVariable String batchName) {
        try {
            var statusOpt = batchConcurrencyService.getBatchStatus(batchName);

            if (statusOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ForecastBatchExecutionStatus status = statusOpt.get();

            Map<String, Object> response = Map.of(
                    "batchName", status.getBatchName(),
                    "status", status.getExecutionStatus().getDescription(),
                    "lastExecutionId", status.getLastExecutionId() != null ? status.getLastExecutionId() : "없음",
                    "startedAt", status.getStartedAt() != null ? status.getStartedAt().toString() : "없음",
                    "finishedAt", status.getFinishedAt() != null ? status.getFinishedAt().toString() : "없음",
                    "executionTimeSeconds", status.getExecutionTimeSeconds(),
                    "isRunning", status.isRunning(),
                    "canExecute", status.canExecute(),
                    "summary", status.getStatusSummary()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("특정 배치 상태 조회 실패 - batchName: {}", batchName, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "상태 조회 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 배치 강제 중지 (락 해제)
     */
    @PostMapping("/force-stop/{batchName}")
    @Operation(summary = "배치 강제 중지", description = "실행중인 배치를 강제로 중지합니다 (관리자용)")
    public ResponseEntity<Map<String, Object>> forceStopBatch(@PathVariable String batchName) {
        try {
            log.warn("배치 강제 중지 요청 - batchName: {}", batchName);

            batchConcurrencyService.forceReleaseLock(batchName);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "배치가 강제 중지되었습니다",
                    "batchName", batchName
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("배치 강제 중지 실패 - batchName: {}", batchName, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "강제 중지 실패: " + e.getMessage(),
                    "batchName", batchName
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 타임아웃된 배치 정리
     */
    @PostMapping("/cleanup-timeout")
    @Operation(summary = "타임아웃된 배치 정리", description = "타임아웃된 배치들을 정리합니다")
    public ResponseEntity<Map<String, Object>> cleanupTimeoutBatches() {
        try {
            log.info("타임아웃된 배치 정리 요청");

            batchConcurrencyService.cleanupTimeoutBatches();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "타임아웃된 배치 정리 완료"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("타임아웃된 배치 정리 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "타임아웃 배치 정리 실패: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 배치 설정 정보 조회
     */
    @GetMapping("/config")
    @Operation(summary = "배치 설정 정보 조회", description = "현재 배치 설정 정보를 조회합니다")
    public ResponseEntity<Map<String, Object>> getBatchConfig() {
        try {
            Map<String, Object> config = new java.util.HashMap<>();
            config.put("enabled", true);
            config.put("aggregationCron", "매일 새벽 2시");
            config.put("forecastCron", "매일 새벽 3시");
            config.put("cleanupCron", "매주 일요일 새벽 4시");
            config.put("chunkSize", 100);
            config.put("skipLimit", 50);
            config.put("retryLimit", 3);
            config.put("timeoutMinutes", 120);
            config.put("predictionPeriodDays", 7);
            config.put("historicalDataDays", 30);
            config.put("minSalesDataDays", 15);
            config.put("cleanupOldDataDays", 30);

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("배치 설정 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "설정 조회 실패: " + e.getMessage()
            ));
        }
    }

    // ================================
    // 헬퍼 메서드들
    // ================================

    /**
     * 날짜 범위 검증
     */
    private ValidationResult validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return ValidationResult.invalid("시작날짜와 종료날짜는 필수입니다.");
        }

        if (startDate.isAfter(endDate)) {
            return ValidationResult.invalid("시작날짜는 종료날짜보다 이전이어야 합니다.");
        }

        if (endDate.isAfter(LocalDate.now())) {
            return ValidationResult.invalid("종료날짜는 오늘보다 이전이어야 합니다.");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysBetween > 90) {
            return ValidationResult.invalid("최대 90일까지만 처리할 수 있습니다. 현재 요청: " + daysBetween + "일");
        }

        return ValidationResult.valid();
    }

    /**
     * 기간별 집계 실행
     */
    private RangeAggregationResult executeRangeAggregation(LocalDate startDate, LocalDate endDate, boolean overwrite) {
        RangeAggregationResult result = new RangeAggregationResult();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            try {
                log.debug("날짜별 집계 실행 - date: {}", currentDate);

                int aggregatedRecords = dailySalesAggregationService.aggregateDailySales(currentDate);

                if (aggregatedRecords > 0) {
                    result.addSuccess(currentDate, aggregatedRecords);
                    log.debug("날짜별 집계 성공 - date: {}, records: {}", currentDate, aggregatedRecords);
                } else {
                    result.addSkipped(currentDate, "집계할 데이터가 없음");
                    log.debug("날짜별 집계 스킵 - date: {}, 데이터 없음", currentDate);
                }

            } catch (Exception e) {
                result.addFailed(currentDate, e.getMessage());
                log.error("날짜별 집계 실패 - date: {}, error: {}", currentDate, e.getMessage(), e);
            }

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    /**
     * 날짜 범위 검증 결과
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 기간별 집계 결과 클래스
     */
    @Getter
    private static class RangeAggregationResult {
        // Getter 메서드들
        private int totalDays = 0;
        private int successDays = 0;
        private int skippedDays = 0;
        private int failedDays = 0;
        private int totalRecords = 0;
        private final List<Map<String, Object>> details = new ArrayList<>();

        public void addSuccess(LocalDate date, int records) {
            totalDays++;
            successDays++;
            totalRecords += records;

            details.add(Map.of(
                    "date", date.toString(),
                    "status", "SUCCESS",
                    "records", records,
                    "message", "집계 완료"
            ));
        }

        public void addSkipped(LocalDate date, String reason) {
            totalDays++;
            skippedDays++;

            details.add(Map.of(
                    "date", date.toString(),
                    "status", "SKIPPED",
                    "records", 0,
                    "message", reason
            ));
        }

        public void addFailed(LocalDate date, String errorMessage) {
            totalDays++;
            failedDays++;

            details.add(Map.of(
                    "date", date.toString(),
                    "status", "FAILED",
                    "records", 0,
                    "message", "실패: " + errorMessage
            ));
        }

    }
}