package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import com.team5.catdogeats.batch.domain.SettlementBatchExecutionStatus;
import com.team5.catdogeats.batch.scheduler.SettlementChunkBatchScheduler;
import com.team5.catdogeats.batch.service.BatchConcurrencyService;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionService.BatchExecutionResult;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 정산 청크 배치 관리용 컨트롤러 (관리자 기능)
 * 기존 Tasklet 방식을 대체하는 청크 기반 배치 관리
 */
@Slf4j
@RestController
@RequestMapping("/admin/settlements/chunk-batch")
@RequiredArgsConstructor
@Tag(name = "정산 청크 배치 관리", description = "관리자용 정산 청크 배치 실행 및 모니터링 API")
public class SettlementChunkBatchAdminController {

    private final SettlementChunkBatchScheduler settlementChunkBatchScheduler;
    private final BatchConcurrencyService batchConcurrencyService;
    private final SettlementChunkMapper settlementChunkMapper;
    private final SettlementBatchProperties batchProperties;

    /**
     * 정산 청크 배치 수동 실행 (일일)
     */
    @PostMapping("/daily/run")
    @Operation(
            summary = "정산 청크 일일 배치 수동 실행",
            description = "정산 데이터 생성 및 상태 갱신 청크 배치를 수동으로 실행합니다. (성능 최적화된 버전)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runChunkDailyBatch() {
        try {
            log.info("관리자 요청 - 정산 청크 일일 배치 수동 실행");

            // 처리 전 현황 조회
            int unsettledCount = settlementChunkMapper.countUnsettledItems();


            long startTime = System.currentTimeMillis();
            BatchExecutionResult result = settlementChunkBatchScheduler.runChunkDailyJobManually();
            long endTime = System.currentTimeMillis();

            Map<String, Object> response = Map.of(
                    "status", result.isSuccess() ? "success" : "failed",
                    "message", result.getMessage(),
                    "executionSummary", result.getExecutionSummary(),
                    "executionTime", endTime - startTime,
                    "batchType", "chunk-based",
                    "configuration", Map.of(
                            "chunkSize", batchProperties.getChunkSize(),
                            "skipLimit", batchProperties.getSkipLimit(),
                            "retryLimit", batchProperties.getRetryLimit()
                    )
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, response.toString()));
            }

        } catch (Exception e) {
            log.error("정산 청크 일일 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 청크 일일 배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 정산 청크 배치 수동 실행 (월간)
     */
    @PostMapping("/monthly/run")
    @Operation(
            summary = "정산 청크 월간 완료 배치 수동 실행",
            description = "처리중인 정산들을 완료 상태로 변경하는 청크 배치를 수동으로 실행합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runChunkMonthlyBatch() {
        try {
            log.info("관리자 요청 - 정산 청크 월간 완료 배치 수동 실행");

            // 처리 전 현황 조회
            int inProgressCount = settlementChunkMapper.countInProgressSettlements();

            long startTime = System.currentTimeMillis();
            BatchExecutionResult result = settlementChunkBatchScheduler.runChunkMonthlyJobManually();
            long endTime = System.currentTimeMillis();

            Map<String, Object> response = Map.of(
                    "status", result.isSuccess() ? "success" : "failed",
                    "message", result.getMessage(),
                    "beforeExecution", Map.of(
                            "inProgressSettlementsCount", inProgressCount
                    ),
                    "executionSummary", result.getExecutionSummary(),
                    "executionTime", endTime - startTime,
                    "batchType", "chunk-based"
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, response.toString()));
            }

        } catch (Exception e) {
            log.error("정산 청크 월간 완료 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 청크 월간 완료 배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 정산 청크 배치 현황 조회
     */
    @GetMapping("/status")
    @Operation(
            summary = "정산 청크 배치 현황 조회",
            description = "정산 청크 배치 처리 대상 건수, 실행 상태 및 성능 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChunkBatchStatus() {
        try {
            log.info("정산 청크 배치 현황 조회 요청");

            int unsettledCount = settlementChunkMapper.countUnsettledItems();
            int inProgressCount = settlementChunkMapper.countInProgressSettlements();


            // 현재 실행중인 배치 조회
            List<SettlementBatchExecutionStatus> runningBatches = batchConcurrencyService.getRunningBatches();

            Map<String, Object> result = Map.of(
                    "currentStatus", Map.of(
                            "unsettledItemsCount", unsettledCount,
                            "inProgressSettlementsCount", inProgressCount,
                            "runningBatchesCount", runningBatches.size()
                    ),
                    "runningBatches", runningBatches.stream().map(batch -> Map.of(
                            "batchName", batch.getBatchName(),
                            "status", batch.getExecutionStatus().getDescription(),
                            "startedAt", batch.getStartedAt(),
                            "lastExecutionId", batch.getLastExecutionId()
                    )).toList(),
                    "batchConfiguration", Map.of(
                            "type", "chunk-based",
                            "chunkSize", batchProperties.getChunkSize(),
                            "skipLimit", batchProperties.getSkipLimit(),
                            "retryLimit", batchProperties.getRetryLimit(),
                            "commissionRate", batchProperties.getCommissionRate(),
                            "confirmationPeriodDays", batchProperties.getConfirmationPeriodDays(),
                            "enabled", batchProperties.isEnabled()
                    ),
                    "batchSchedule", Map.of(
                            "dailyBatch", batchProperties.getDailyCron() + " - 정산 데이터 생성 및 상태 갱신 (청크)",
                            "monthlyBatch", batchProperties.getMonthlyCron() + " - 정산 완료 처리 (청크)"
                    ),
                    "performance", Map.of(
                            "expectedThroughput", batchProperties.getChunkSize() + "건/트랜잭션",
                            "faultTolerance", "Skip 정책: " + batchProperties.getSkipLimit() + "건, Retry: " + batchProperties.getRetryLimit() + "회",
                            "concurrencyControl", "DB 기반 분산락 사용"
                    ),
                    "systemInfo", Map.of(
                            "batchType", "Spring Batch Chunk Processing",
                            "lastChecked", System.currentTimeMillis()
                    )
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));

        } catch (Exception e) {
            log.error("정산 청크 배치 현황 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 청크 배치 현황 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 배치 실행 락 강제 해제
     */
    @PostMapping("/lock/release/{batchName}")
    @Operation(
            summary = "배치 실행 락 강제 해제",
            description = "특정 배치의 실행 락을 강제로 해제합니다. (관리자 전용 - 주의해서 사용)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> forceReleaseLock(
            @Parameter(description = "배치 이름", example = "SETTLEMENT_CREATE")
            @PathVariable String batchName) {
        try {
            log.warn("관리자 요청 - 배치 락 강제 해제: {}", batchName);

            // 현재 상태 확인
            var currentStatus = batchConcurrencyService.getBatchStatus(batchName);

            if (currentStatus.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(ResponseCode.INVALID_TYPE_VALUE,
                                "존재하지 않는 배치 이름: " + batchName));
            }

            SettlementBatchExecutionStatus status = currentStatus.get();

            // 강제 락 해제
            batchConcurrencyService.forceReleaseLock(batchName);

            Map<String, Object> result = Map.of(
                    "status", "success",
                    "message", "배치 락이 강제로 해제되었습니다.",
                    "batchName", batchName,
                    "previousStatus", status.getExecutionStatus().getDescription(),
                    "releasedAt", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));

        } catch (Exception e) {
            log.error("배치 락 강제 해제 실패 - batchName: {}", batchName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "배치 락 강제 해제 실패: " + e.getMessage()));
        }
    }

    /**
     * 배치 설정 조회
     */
    @GetMapping("/configuration")
    @Operation(
            summary = "정산 청크 배치 설정 조회",
            description = "현재 적용된 정산 청크 배치 설정값들을 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBatchConfiguration() {
        try {
            Map<String, Object> config = Map.of(
                    "chunkProcessing", Map.of(
                            "chunkSize", batchProperties.getChunkSize(),
                            "skipLimit", batchProperties.getSkipLimit(),
                            "retryLimit", batchProperties.getRetryLimit()
                    ),
                    "business", Map.of(
                            "commissionRate", batchProperties.getCommissionRate(),
                            "confirmationPeriodDays", batchProperties.getConfirmationPeriodDays()
                    ),
                    "schedule", Map.of(
                            "enabled", batchProperties.isEnabled(),
                            "dailyCron", batchProperties.getDailyCron(),
                            "monthlyCron", batchProperties.getMonthlyCron()
                    ),
                    "monitoring", Map.of(
                            "performanceMonitoringEnabled", batchProperties.isPerformanceMonitoringEnabled(),
                            "detailLoggingEnabled", batchProperties.isDetailLoggingEnabled(),
                            "timeoutMinutes", batchProperties.getTimeoutMinutes()
                    ),
                    "notification", Map.of(
                            "failureNotificationEnabled", batchProperties.getNotification().isFailureNotificationEnabled(),
                            "successNotificationEnabled", batchProperties.getNotification().isSuccessNotificationEnabled(),
                            "highSkipRateThreshold", batchProperties.getNotification().getHighSkipRateThreshold(),
                            "slowProcessingThreshold", batchProperties.getNotification().getSlowProcessingThreshold()
                    )
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, config));

        } catch (Exception e) {
            log.error("배치 설정 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "배치 설정 조회 실패: " + e.getMessage()));
        }
    }
}