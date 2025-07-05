package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.batch.scheduler.SettlementBatchScheduler;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.service.SettlementBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 정산 배치 관리용 컨트롤러 (관리자 기능)
 */
@Slf4j
@RestController
@RequestMapping("/admin/settlements/batch")
@RequiredArgsConstructor
@Tag(name = "정산 배치 관리", description = "관리자용 정산 배치 실행 및 모니터링 API")
public class SettlementBatchAdminController {

    private final SettlementBatchService settlementBatchService;
    private final SettlementBatchScheduler settlementBatchScheduler;

    /**
     * 정산 배치 수동 실행 (일일)
     */
    @PostMapping("/daily/run")
    @Operation(
            summary = "정산 일일 배치 수동 실행",
            description = "정산 데이터 생성 및 상태 갱신 배치를 수동으로 실행합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runDailyBatch() {
        try {
            log.info("관리자 요청 - 정산 일일 배치 수동 실행");

            // 처리 전 현황 조회
            int unsettledCount = settlementBatchService.getUnsettledItemsCount();
            int pendingReadyCount = settlementBatchService.getPendingSettlementsReadyForProgressCount();

            settlementBatchScheduler.runDailyJobManually();

            Map<String, Object> result = Map.of(
                    "status", "success",
                    "message", "정산 일일 배치가 성공적으로 실행되었습니다.",
                    "beforeExecution", Map.of(
                            "unsettledItemsCount", unsettledCount,
                            "pendingReadyForProgressCount", pendingReadyCount
                    ),
                    "executionTime", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));
        } catch (Exception e) {
            log.error("정산 일일 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 일일 배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 정산 배치 수동 실행 (월간)
     */
    @PostMapping("/monthly/run")
    @Operation(
            summary = "정산 월간 완료 배치 수동 실행",
            description = "처리중인 정산들을 완료 상태로 변경하는 배치를 수동으로 실행합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runMonthlyBatch() {
        try {
            log.info("관리자 요청 - 정산 월간 완료 배치 수동 실행");

            // 처리 전 현황 조회
            int inProgressCount = settlementBatchService.getInProgressSettlementsCount();

            settlementBatchScheduler.runMonthlyJobManually();

            Map<String, Object> result = Map.of(
                    "status", "success",
                    "message", "정산 월간 완료 배치가 성공적으로 실행되었습니다.",
                    "beforeExecution", Map.of(
                            "inProgressSettlementsCount", inProgressCount
                    ),
                    "executionTime", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));
        } catch (Exception e) {
            log.error("정산 월간 완료 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 월간 완료 배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 정산 배치 현황 조회
     */
    @GetMapping("/status")
    @Operation(
            summary = "정산 배치 현황 조회",
            description = "정산 배치 처리 대상 건수 및 현재 상태를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBatchStatus() {
        try {
            log.info("정산 배치 현황 조회 요청");

            int unsettledCount = settlementBatchService.getUnsettledItemsCount();
            int inProgressCount = settlementBatchService.getInProgressSettlementsCount();
            int pendingReadyCount = settlementBatchService.getPendingSettlementsReadyForProgressCount();

            Map<String, Object> result = Map.of(
                    "currentStatus", Map.of(
                            "unsettledItemsCount", unsettledCount,
                            "inProgressSettlementsCount", inProgressCount,
                            "pendingReadyForProgressCount", pendingReadyCount
                    ),
                    "batchSchedule", Map.of(
                            "dailyBatch", "매일 오전 2시 - 정산 데이터 생성 및 상태 갱신",
                            "monthlyBatch", "매월 1일 오전 4시 - 정산 완료 처리"
                    ),
                    "settlementProcess", Map.of(
                            "step1", "배송완료 → 정산 데이터 생성 (PENDING)",
                            "step2", "배송완료 + 7일 경과 → 정산 확정 (IN_PROGRESS)",
                            "step3", "매월 1일 → 정산 완료 (COMPLETED)"
                    ),
                    "systemInfo", Map.of(
                            "commissionRate", "5%",
                            "confirmationPeriod", "7일",
                            "lastChecked", System.currentTimeMillis()
                    )
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));
        } catch (Exception e) {
            log.error("정산 배치 현황 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 배치 현황 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 판매자의 정산 대상 건수 조회 (테스트용)
     */
    @GetMapping("/status/seller/{sellerId}")
    @Operation(
            summary = "판매자별 정산 대상 건수 조회",
            description = "특정 판매자의 정산 대상 건수를 조회합니다. (테스트용)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSellerBatchStatus(
            @Parameter(description = "판매자 ID", example = "seller-uuid-123")
            @PathVariable String sellerId) {
        try {
            log.info("판매자별 정산 배치 현황 조회 요청 - sellerId: {}", sellerId);

            // SettlementBatchMapper에 추가된 메서드 사용
            // int sellerUnsettledCount = settlementBatchService.getUnsettledItemsCountBySellerId(sellerId);

            Map<String, Object> result = Map.of(
                    "sellerId", sellerId,
                    "message", "판매자별 정산 대상 건수 조회",
                    "note", "필요시 SettlementBatchMapper.countUnsettledItemsBySellerId 메서드를 활용하여 구현 가능",
                    "currentlyAvailable", "전체 현황만 제공됩니다."
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));
        } catch (Exception e) {
            log.error("판매자별 정산 배치 현황 조회 실패 - sellerId: {}", sellerId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "판매자별 정산 배치 현황 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 정산 배치 프로세스 정보 조회
     */
    @GetMapping("/process-info")
    @Operation(
            summary = "정산 배치 프로세스 정보 조회",
            description = "정산 배치의 전체 프로세스와 상태별 설명을 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessInfo() {
        try {
            Map<String, Object> result = Map.of(
                    "settlementStates", Map.of(
                            "PENDING", "대기중 - 배송완료 7일 이내 (반품/교환 기간)",
                            "IN_PROGRESS", "처리중 - 배송완료 후 7일 경과 (정산 확정됨)",
                            "COMPLETED", "정산완료 - 매월 1일 자동 정산 처리됨"
                    ),
                    "batchJobs", Map.of(
                            "dailyJob", Map.of(
                                    "name", "settlementDailyJob",
                                    "schedule", "매일 오전 2시",
                                    "description", "정산 데이터 생성 및 상태 갱신",
                                    "steps", new String[]{
                                            "1. 배송완료된 주문아이템 → 정산 데이터 생성 (PENDING)",
                                            "2. 배송완료 + 7일 경과 → PENDING → IN_PROGRESS"
                                    }
                            ),
                            "monthlyJob", Map.of(
                                    "name", "settlementMonthlyJob",
                                    "schedule", "매월 1일 오전 4시",
                                    "description", "정산 완료 처리",
                                    "steps", new String[]{
                                            "1. IN_PROGRESS → COMPLETED",
                                            "2. settled_at 시간 기록"
                                    }
                            )
                    ),
                    "manualExecution", Map.of(
                            "dailyBatch", "POST /admin/settlements/batch/daily/run",
                            "monthlyBatch", "POST /admin/settlements/batch/monthly/run",
                            "status", "GET /admin/settlements/batch/status"
                    )
            );

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));
        } catch (Exception e) {
            log.error("정산 배치 프로세스 정보 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR,
                            "정산 배치 프로세스 정보 조회 실패: " + e.getMessage()));
        }
    }
}