// DemandForecastManualController.java - 수동 실행용 컨트롤러
package com.team5.catdogeats.forecast.controller;

import com.team5.catdogeats.forecast.scheduler.DemandForecastBatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 수요예측 배치 수동 실행 컨트롤러
 * 개발/테스트 환경에서 배치를 수동으로 실행하기 위한 API
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/demand-forecast")
@RequiredArgsConstructor
@Tag(name = "수요예측 관리", description = "수요예측 배치 수동 실행 API")
public class DemandForecastManualController {

    private final DemandForecastBatchScheduler batchScheduler;

    /**
     * 특정 날짜의 일별 판매 집계 수동 실행
     */
    @PostMapping("/aggregation/manual")
    @Operation(summary = "일별 판매 집계 수동 실행", description = "특정 날짜의 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runManualAggregation(
            @RequestParam(required = false) String date) {

        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now().minusDays(1);

            log.info("수동 일별 집계 요청 - targetDate: {}", targetDate);

            DemandForecastBatchScheduler.BatchExecutionResult result =
                    batchScheduler.runManualAggregation(targetDate);

            Map<String, Object> response = Map.of(
                    "success", result.success(),
                    "message", result.message(),
                    "processedCount", result.processedCount(),
                    "targetDate", targetDate.toString()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("수동 일별 집계 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "집계 실행 실패: " + e.getMessage(),
                    "processedCount", 0
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 여러 날짜 범위의 일별 판매 집계 수동 실행
     */
    @PostMapping("/aggregation/range")
    @Operation(summary = "기간별 판매 집계 수동 실행", description = "시작일부터 종료일까지의 판매 데이터를 집계합니다")
    public ResponseEntity<Map<String, Object>> runManualAggregationRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "시작일이 종료일보다 늦을 수 없습니다"
                ));
            }

            log.info("수동 기간별 집계 요청 - startDate: {}, endDate: {}", start, end);

            int totalProcessed = 0;
            LocalDate currentDate = start;

            while (!currentDate.isAfter(end)) {
                DemandForecastBatchScheduler.BatchExecutionResult result =
                        batchScheduler.runManualAggregation(currentDate);

                if (result.success()) {
                    totalProcessed += result.processedCount();
                }

                currentDate = currentDate.plusDays(1);

                // 과부하 방지
                Thread.sleep(100);
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", String.format("기간별 집계 완료: %s ~ %s", start, end),
                    "processedCount", totalProcessed,
                    "startDate", start.toString(),
                    "endDate", end.toString()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("수동 기간별 집계 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "기간별 집계 실행 실패: " + e.getMessage(),
                    "processedCount", 0
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 특정 판매자의 수요예측 수동 실행
     */
    @PostMapping("/forecast/manual/{sellerId}")
    @Operation(summary = "특정 판매자 수요예측 수동 실행", description = "특정 판매자의 수요예측을 실행합니다")
    public ResponseEntity<Map<String, Object>> runManualForecast(
            @PathVariable String sellerId) {

        try {
            log.info("수동 수요예측 요청 - sellerId: {}", sellerId);

            DemandForecastBatchScheduler.BatchExecutionResult result =
                    batchScheduler.runManualForecast(sellerId);

            Map<String, Object> response = Map.of(
                    "success", result.success(),
                    "message", result.message(),
                    "processedCount", result.processedCount(),
                    "sellerId", sellerId
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("수동 수요예측 실행 실패 - sellerId: {}", sellerId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "수요예측 실행 실패: " + e.getMessage(),
                    "processedCount", 0
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 전체 배치 작업 수동 실행 (집계 + 예측)
     */
    @PostMapping("/batch/full")
    @Operation(summary = "전체 배치 작업 수동 실행", description = "일별 집계 + 모든 판매자 수요예측을 실행합니다")
    public ResponseEntity<Map<String, Object>> runFullBatch() {

        try {
            log.info("전체 배치 작업 수동 실행 시작");

            // 배치 스케줄러의 메인 메서드 호출
            batchScheduler.runDemandForecastBatch();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "전체 배치 작업 완료",
                    "note", "자세한 내용은 서버 로그를 확인하세요"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("전체 배치 작업 수동 실행 실패", e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "전체 배치 작업 실행 실패: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 배치 상태 확인
     */
    @GetMapping("/status")
    @Operation(summary = "배치 상태 확인", description = "수요예측 배치 관련 통계를 확인합니다")
    public ResponseEntity<Map<String, Object>> getBatchStatus() {

        try {
            // 간단한 상태 정보 반환
            Map<String, Object> status = Map.of(
                    "currentTime", LocalDate.now().toString(),
                    "batchEnabled", true,
                    "lastExecutionTime", "수동 실행 확인 필요",
                    "note", "구체적인 실행 정보는 로그에서 확인하세요"
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("배치 상태 확인 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "상태 확인 실패: " + e.getMessage()
            ));
        }
    }
}