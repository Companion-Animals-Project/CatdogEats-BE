package com.team5.catdogeats.batch.forecast.controller;

import com.team5.catdogeats.batch.forecast.scheduler.ForecastBatchScheduler;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchConcurrencyService;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchExecutionService.BatchExecutionResult;
import com.team5.catdogeats.batch.forecast.domain.ForecastBatchExecutionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 수요예측 배치 수동 실행 컨트롤러
 * 기존 DemandForecastManualController를 대체하는 새로운 배치 기반 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/forecast-batch")
@RequiredArgsConstructor
@Tag(name = "수요예측 배치 관리", description = "수요예측 배치 수동 실행 및 모니터링 API")
public class ForecastBatchManualController {

    private final ForecastBatchScheduler forecastBatchScheduler;
    private final ForecastBatchConcurrencyService batchConcurrencyService;

    /**
     * 일별 판매 집계 배치 수동 실행
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
}