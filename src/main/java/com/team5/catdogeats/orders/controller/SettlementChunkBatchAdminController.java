package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import com.team5.catdogeats.batch.scheduler.SettlementChunkBatchScheduler;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 정산 배치 관리용 컨트롤러 (단순화됨)
 */
@Slf4j
@RestController
@RequestMapping("/admin/settlements/batch")
@RequiredArgsConstructor
@Tag(name = "정산 배치 관리", description = "관리자용 정산 배치 실행 API")
public class SettlementChunkBatchAdminController {

    private final SettlementChunkBatchScheduler settlementChunkBatchScheduler;
    private final SettlementChunkMapper settlementChunkMapper;
    private final SettlementBatchProperties batchProperties;

    /**
     * 정산 일일 배치 수동 실행
     */
    @PostMapping("/daily/run")
    @Operation(
            summary = "정산 일일 배치 수동 실행",
            description = "정산 데이터 생성 배치를 수동으로 실행합니다."
    )
    public ResponseEntity<APIResponse<Map<String, Object>>> runDailyBatch() {
        try {
            log.info("관리자 요청 - 정산 일일 배치 수동 실행");

            // 처리 전 현황 조회
            int unsettledCount = settlementChunkMapper.countUnsettledItems();

            long startTime = System.currentTimeMillis();

            // Spring Batch JobExecution 직접 사용
            JobExecution jobExecution = settlementChunkBatchScheduler.runDailyJobManually();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 실행 통계 계산
            long totalRead = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getReadCount).sum();
            long totalWrite = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount).sum();
            long totalSkip = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getSkipCount).sum();

            boolean isSuccess = BatchStatus.COMPLETED.equals(jobExecution.getStatus());

            Map<String, Object> response = Map.of(
                    "success", isSuccess,
                    "status", jobExecution.getStatus().toString(),
                    "jobExecutionId", jobExecution.getId(),
                    "beforeExecution", Map.of(
                            "unsettledItemsCount", unsettledCount
                    ),
                    "executionSummary", Map.of(
                            "totalRead", totalRead,
                            "totalWrite", totalWrite,
                            "totalSkip", totalSkip,
                            "executionTimeMs", executionTime
                    ),
                    "configuration", Map.of(
                            "chunkSize", batchProperties.getChunkSize(),
                            "skipLimit", batchProperties.getSkipLimit(),
                            "retryLimit", batchProperties.getRetryLimit()
                    )
            );

            if (isSuccess) {
                return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                                "배치 실행 실패: " + jobExecution.getExitStatus().getExitDescription()));
            }

        } catch (Exception e) {
            log.error("정산 일일 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 일일 배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 정산 월간 배치 수동 실행
     */
    @PostMapping("/monthly/run")
    @Operation(
            summary = "정산 월간 완료 배치 수동 실행",
            description = "처리중인 정산들을 완료 상태로 변경하는 배치를 수동으로 실행합니다."
    )
    public ResponseEntity<APIResponse<Map<String, Object>>> runMonthlyBatch() {
        try {
            log.info("관리자 요청 - 정산 월간 완료 배치 수동 실행");

            // 처리 전 현황 조회
            int inProgressCount = settlementChunkMapper.countInProgressSettlements();

            long startTime = System.currentTimeMillis();

            // Spring Batch JobExecution 직접 사용
            JobExecution jobExecution = settlementChunkBatchScheduler.runMonthlyJobManually();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 실행 통계 계산
            long totalRead = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getReadCount).sum();
            long totalWrite = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount).sum();
            long totalSkip = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getSkipCount).sum();

            boolean isSuccess = BatchStatus.COMPLETED.equals(jobExecution.getStatus());

            Map<String, Object> response = Map.of(
                    "success", isSuccess,
                    "status", jobExecution.getStatus().toString(),
                    "jobExecutionId", jobExecution.getId(),
                    "beforeExecution", Map.of(
                            "inProgressSettlementsCount", inProgressCount
                    ),
                    "executionSummary", Map.of(
                            "totalRead", totalRead,
                            "totalWrite", totalWrite,
                            "totalSkip", totalSkip,
                            "executionTimeMs", executionTime
                    )
            );

            if (isSuccess) {
                return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                                "배치 실행 실패: " + jobExecution.getExitStatus().getExitDescription()));
            }

        } catch (Exception e) {
            log.error("정산 월간 완료 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 월간 완료 배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 배치 현황 조회
     */
    @GetMapping("/status")
    @Operation(
            summary = "정산 배치 현황 조회",
            description = "정산 배치 처리 대상 건수 및 설정 정보를 조회합니다."
    )
    public ResponseEntity<APIResponse<Map<String, Object>>> getBatchStatus() {
        try {
            int unsettledCount = settlementChunkMapper.countUnsettledItems();
            int inProgressCount = settlementChunkMapper.countInProgressSettlements();

            Map<String, Object> status = Map.of(
                    "currentStatus", Map.of(
                            "unsettledItemsCount", unsettledCount,
                            "inProgressSettlementsCount", inProgressCount
                    ),
                    "batchConfiguration", Map.of(
                            "chunkSize", batchProperties.getChunkSize(),
                            "skipLimit", batchProperties.getSkipLimit(),
                            "retryLimit", batchProperties.getRetryLimit(),
                            "commissionRate", batchProperties.getCommissionRate(),
                            "enabled", batchProperties.isEnabled()
                    ),
                    "schedule", Map.of(
                            "dailyCron", batchProperties.getDailyCron(),
                            "monthlyCron", batchProperties.getMonthlyCron()
                    )
            );

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, status));

        } catch (Exception e) {
            log.error("배치 현황 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "배치 현황 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 배치 설정 조회
     */
    @GetMapping("/configuration")
    @Operation(
            summary = "정산 배치 설정 조회",
            description = "현재 적용된 정산 배치 설정값들을 조회합니다."
    )
    public ResponseEntity<APIResponse<Map<String, Object>>> getBatchConfiguration() {
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
                    )
            );

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, config));

        } catch (Exception e) {
            log.error("배치 설정 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "배치 설정 조회 실패: " + e.getMessage()));
        }
    }
}