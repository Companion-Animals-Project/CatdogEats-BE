package com.team5.catdogeats.forecast.controller;

import com.team5.catdogeats.batch.scheduler.ForecastBatchScheduler;
import com.team5.catdogeats.forecast.service.DailySalesAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
@Tag(name = "수요예측 배치 관리", description = "수요예측 배치 수동 실행 API")
public class ForecastBatchManualController {

    private final ForecastBatchScheduler forecastBatchScheduler;
    private final DailySalesAggregationService dailySalesAggregationService;

    // ================================
    // 기본 배치 실행 API
    // ================================

    @PostMapping("/aggregation/manual")
    @Operation(summary = "일별 판매 집계 배치 수동 실행", description = "어제 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationBatch() {
        try {
            log.info("수동 일별 집계 배치 요청");

            JobExecution jobExecution = forecastBatchScheduler.runAggregationJobManually();
            boolean success = BatchStatus.COMPLETED.equals(jobExecution.getStatus());

            Map<String, Object> response = Map.of(
                    "success", success,
                    "message", success ? "일별 집계 배치 완료" : "일별 집계 배치 실패",
                    "jobExecutionId", jobExecution.getId(),
                    "status", jobExecution.getStatus().toString(),
                    "batchType", "aggregation"
            );

            return success ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);

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

    @PostMapping("/prediction/manual")
    @Operation(summary = "수요예측 실행 배치 수동 실행", description = "모든 활성 판매자의 수요예측을 실행합니다")
    public ResponseEntity<Map<String, Object>> runPredictionBatch() {
        try {
            log.info("수동 수요예측 배치 요청");

            JobExecution jobExecution = forecastBatchScheduler.runPredictionJobManually();
            boolean success = BatchStatus.COMPLETED.equals(jobExecution.getStatus());

            Map<String, Object> response = Map.of(
                    "success", success,
                    "message", success ? "수요예측 배치 완료" : "수요예측 배치 실패",
                    "jobExecutionId", jobExecution.getId(),
                    "status", jobExecution.getStatus().toString(),
                    "batchType", "prediction"
            );

            return success ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);

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

    @PostMapping("/cleanup/manual")
    @Operation(summary = "데이터 정리 배치 수동 실행", description = "오래된 예측 데이터를 정리합니다")
    public ResponseEntity<Map<String, Object>> runCleanupBatch() {
        try {
            log.info("수동 데이터 정리 배치 요청");

            JobExecution jobExecution = forecastBatchScheduler.runCleanupJobManually();
            boolean success = BatchStatus.COMPLETED.equals(jobExecution.getStatus());

            Map<String, Object> response = Map.of(
                    "success", success,
                    "message", success ? "데이터 정리 배치 완료" : "데이터 정리 배치 실패",
                    "jobExecutionId", jobExecution.getId(),
                    "status", jobExecution.getStatus().toString(),
                    "batchType", "cleanup"
            );

            return success ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);

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

    @PostMapping("/full/manual")
    @Operation(summary = "전체 배치 작업 수동 실행", description = "일별 집계 + 수요예측을 순차적으로 실행합니다")
    public ResponseEntity<Map<String, Object>> runFullBatch() {
        try {
            log.info("전체 수요예측 배치 작업 수동 실행 시작");

            // 1. 일별 집계 실행
            JobExecution aggregationExecution = forecastBatchScheduler.runAggregationJobManually();
            boolean aggregationSuccess = BatchStatus.COMPLETED.equals(aggregationExecution.getStatus());

            if (!aggregationSuccess) {
                log.error("일별 집계 실패로 전체 배치 중단");
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "일별 집계 실패로 중단됨",
                        "batchType", "full",
                        "failedStep", "aggregation",
                        "aggregationStatus", aggregationExecution.getStatus().toString()
                );
                return ResponseEntity.status(500).body(response);
            }

            // 2. 수요예측 실행
            JobExecution predictionExecution = forecastBatchScheduler.runPredictionJobManually();
            boolean predictionSuccess = BatchStatus.COMPLETED.equals(predictionExecution.getStatus());

            Map<String, Object> response = Map.of(
                    "success", predictionSuccess,
                    "message", predictionSuccess ? "전체 배치 작업 완료" : "수요예측 단계 실패",
                    "batchType", "full",
                    "aggregationStatus", aggregationExecution.getStatus().toString(),
                    "predictionStatus", predictionExecution.getStatus().toString()
            );

            return predictionSuccess ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);

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
    // 기간별 집계 API
    // ================================

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

    @PostMapping("/aggregation/range")
    @Operation(summary = "기간별 판매 집계 수동 실행", description = "지정된 기간의 판매 데이터를 일별로 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationForDateRange(
            @Parameter(description = "시작 날짜 (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2024-01-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            log.info("기간별 집계 배치 요청 - startDate: {}, endDate: {}", startDate, endDate);

            // 입력값 검증
            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "시작날짜와 종료날짜는 필수입니다.",
                        "batchType", "range_aggregation"
                ));
            }

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "시작날짜는 종료날짜보다 이전이어야 합니다.",
                        "batchType", "range_aggregation"
                ));
            }

            if (endDate.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "종료날짜는 오늘보다 이전이어야 합니다.",
                        "batchType", "range_aggregation"
                ));
            }

            // 기간별 집계 실행
            RangeAggregationResult result = executeRangeAggregation(startDate, endDate);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "기간별 집계 완료",
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "totalDays", result.totalDays(),
                    "successDays", result.successDays(),
                    "failedDays", result.failedDays(),
                    "totalRecords", result.totalRecords(),
                    "batchType", "range_aggregation"
            );

            log.info("기간별 집계 완료 - 총 {}일, 성공 {}일, 실패 {}일, 총 처리건수: {}",
                    result.totalDays(), result.successDays(), result.failedDays(), result.totalRecords());

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

    @PostMapping("/aggregation/recent-days/{days}")
    @Operation(summary = "최근 N일 판매 집계 수동 실행", description = "최근 N일간의 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runAggregationForRecentDays(
            @Parameter(description = "최근 일수 (1-30)", example = "7")
            @PathVariable int days) {

        try {
            log.info("최근 {}일 집계 배치 요청", days);

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
            RangeAggregationResult result = executeRangeAggregation(startDate, endDate);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "최근 " + days + "일 집계 완료",
                    "days", days,
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "successDays", result.successDays(),
                    "failedDays", result.failedDays(),
                    "totalRecords", result.totalRecords(),
                    "batchType", "recent_days_aggregation"
            );

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
    // 헬퍼 메서드들
    // ================================

    /**
     * 기간별 집계 실행
     */
    private RangeAggregationResult executeRangeAggregation(LocalDate startDate, LocalDate endDate) {
        int totalDays = 0;
        int successDays = 0;
        int failedDays = 0;
        int totalRecords = 0;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            totalDays++;
            try {
                int aggregatedRecords = dailySalesAggregationService.aggregateDailySales(currentDate);
                if (aggregatedRecords >= 0) {
                    successDays++;
                    totalRecords += aggregatedRecords;
                    log.debug("날짜별 집계 성공 - date: {}, records: {}", currentDate, aggregatedRecords);
                } else {
                    failedDays++;
                    log.debug("날짜별 집계 스킵 - date: {}, 데이터 없음", currentDate);
                }
            } catch (Exception e) {
                failedDays++;
                log.error("날짜별 집계 실패 - date: {}, error: {}", currentDate, e.getMessage(), e);
            }

            currentDate = currentDate.plusDays(1);
        }

        return new RangeAggregationResult(totalDays, successDays, failedDays, totalRecords);
    }

    /**
     * 기간별 집계 결과
     */
    private record RangeAggregationResult(
            int totalDays,
            int successDays,
            int failedDays,
            int totalRecords
    ) {}
}