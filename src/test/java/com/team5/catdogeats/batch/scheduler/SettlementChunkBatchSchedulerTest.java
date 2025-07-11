package com.team5.catdogeats.batch.scheduler;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import com.team5.catdogeats.batch.domain.SettlementBatchExecutionStatus;
import com.team5.catdogeats.batch.service.BatchConcurrencyServiceImpl;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionServiceImpl;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionServiceImpl.BatchExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 청크 배치 스케줄러 단위 테스트")
class SettlementChunkBatchSchedulerTest {

    @Mock
    private SettlementBatchExecutionServiceImpl batchExecutionService;

    @Mock
    private BatchConcurrencyServiceImpl batchConcurrencyServiceImpl;

    @Mock
    private SettlementBatchProperties batchProperties;

    @Mock
    private SettlementBatchProperties.NotificationConfig notificationConfig;

    private SettlementChunkBatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SettlementChunkBatchScheduler(
                batchExecutionService,
                batchConcurrencyServiceImpl,
                batchProperties
        );
    }

    @Test
    @DisplayName("일일 배치 스케줄러 - 정상 실행 성공")
    void runChunkDailySettlementJob_Success() {
        // Given
        given(batchProperties.isEnabled()).willReturn(true);
        given(batchProperties.getChunkSize()).willReturn(100);
        given(batchProperties.getSkipLimit()).willReturn(10);
        given(batchProperties.getRetryLimit()).willReturn(3);
        BatchExecutionResult successResult = createSuccessResult();
        given(batchExecutionService.executeChunkDailyJob()).willReturn(successResult);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkDailySettlementJob());

        // Then
        verify(batchProperties).isEnabled();
        verify(batchExecutionService).executeChunkDailyJob();
        verify(batchProperties).getChunkSize();
        verify(batchProperties).getSkipLimit();
        verify(batchProperties).getRetryLimit();
    }

    @Test
    @DisplayName("일일 배치 스케줄러 - 배치 비활성화 상태")
    void runChunkDailySettlementJob_BatchDisabled() {
        // Given
        given(batchProperties.isEnabled()).willReturn(false);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkDailySettlementJob());

        // Then
        verify(batchProperties).isEnabled();
        verify(batchExecutionService, never()).executeChunkDailyJob();
    }

    @Test
    @DisplayName("일일 배치 스케줄러 - 배치 실행 실패")
    void runChunkDailySettlementJob_ExecutionFailed() {
        // Given
        given(batchProperties.isEnabled()).willReturn(true);
        BatchExecutionResult failedResult = BatchExecutionResult.failed("배치 실행 실패");
        given(batchExecutionService.executeChunkDailyJob()).willReturn(failedResult);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkDailySettlementJob());

        // Then
        verify(batchExecutionService).executeChunkDailyJob();
    }

    @Test
    @DisplayName("일일 배치 스케줄러 - 예상치 못한 예외 발생")
    void runChunkDailySettlementJob_UnexpectedException() {
        // Given
        given(batchProperties.isEnabled()).willReturn(true);
        given(batchExecutionService.executeChunkDailyJob())
                .willThrow(new RuntimeException("예상치 못한 오류"));

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkDailySettlementJob());

        // Then
        verify(batchExecutionService).executeChunkDailyJob();
    }

    @Test
    @DisplayName("월간 배치 스케줄러 - 정상 실행 성공")
    void runChunkMonthlySettlementJob_Success() {
        // Given
        given(batchProperties.isEnabled()).willReturn(true);
        BatchExecutionResult successResult = createSuccessResult();
        given(batchExecutionService.executeChunkMonthlyJob()).willReturn(successResult);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkMonthlySettlementJob());

        // Then
        verify(batchProperties).isEnabled();
        verify(batchExecutionService).executeChunkMonthlyJob();
    }

    @Test
    @DisplayName("월간 배치 스케줄러 - 배치 비활성화 상태")
    void runChunkMonthlySettlementJob_BatchDisabled() {
        // Given
        given(batchProperties.isEnabled()).willReturn(false);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkMonthlySettlementJob());

        // Then
        verify(batchProperties).isEnabled();
        verify(batchExecutionService, never()).executeChunkMonthlyJob();
    }

    @Test
    @DisplayName("월간 배치 스케줄러 - 배치 실행 실패")
    void runChunkMonthlySettlementJob_ExecutionFailed() {
        // Given
        given(batchProperties.isEnabled()).willReturn(true);
        BatchExecutionResult failedResult = BatchExecutionResult.failed("월간 배치 실패");
        given(batchExecutionService.executeChunkMonthlyJob()).willReturn(failedResult);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.runChunkMonthlySettlementJob());

        // Then
        verify(batchExecutionService).executeChunkMonthlyJob();
    }

    @Test
    @DisplayName("수동 일일 배치 실행 - 성공")
    void runChunkDailyJobManually_Success() {
        // Given
        BatchExecutionResult successResult = createSuccessResult();
        given(batchExecutionService.executeChunkDailyJob()).willReturn(successResult);

        // When
        BatchExecutionResult result = scheduler.runChunkDailyJobManually();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(batchExecutionService).executeChunkDailyJob();
    }

    @Test
    @DisplayName("수동 일일 배치 실행 - 실패")
    void runChunkDailyJobManually_Failed() {
        // Given
        BatchExecutionResult failedResult = BatchExecutionResult.failed("수동 실행 실패");
        given(batchExecutionService.executeChunkDailyJob()).willReturn(failedResult);

        // When
        BatchExecutionResult result = scheduler.runChunkDailyJobManually();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("수동 실행 실패");
        verify(batchExecutionService).executeChunkDailyJob();
    }

    @Test
    @DisplayName("수동 일일 배치 실행 - 예외 발생 시 RuntimeException 래핑")
    void runChunkDailyJobManually_ThrowsException() {
        // Given
        given(batchExecutionService.executeChunkDailyJob())
                .willThrow(new RuntimeException("서비스 오류"));

        // When & Then
        assertThatThrownBy(() -> scheduler.runChunkDailyJobManually())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("정산 청크 일일 배치 작업 실행 실패")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(batchExecutionService).executeChunkDailyJob();
    }

    @Test
    @DisplayName("수동 월간 배치 실행 - 성공")
    void runChunkMonthlyJobManually_Success() {
        // Given
        BatchExecutionResult successResult = createSuccessResult();
        given(batchExecutionService.executeChunkMonthlyJob()).willReturn(successResult);

        // When
        BatchExecutionResult result = scheduler.runChunkMonthlyJobManually();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(batchExecutionService).executeChunkMonthlyJob();
    }

    @Test
    @DisplayName("수동 월간 배치 실행 - 예외 발생 시 RuntimeException 래핑")
    void runChunkMonthlyJobManually_ThrowsException() {
        // Given
        given(batchExecutionService.executeChunkMonthlyJob())
                .willThrow(new RuntimeException("월간 배치 오류"));

        // When & Then
        assertThatThrownBy(() -> scheduler.runChunkMonthlyJobManually())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("정산 청크 월간 완료 배치 작업 실행 실패")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(batchExecutionService).executeChunkMonthlyJob();
    }

    @Test
    @DisplayName("타임아웃 배치 정리 스케줄러 - 정상 실행")
    void cleanupTimeoutBatches_Success() {
        // Given
        willDoNothing().given(batchConcurrencyServiceImpl).cleanupTimeoutBatches();
        given(batchConcurrencyServiceImpl.getRunningBatches()).willReturn(List.of());

        // When
        assertThatNoException().isThrownBy(() -> scheduler.cleanupTimeoutBatches());

        // Then
        verify(batchConcurrencyServiceImpl).cleanupTimeoutBatches();
        verify(batchConcurrencyServiceImpl).getRunningBatches();
    }

    @Test
    @DisplayName("타임아웃 배치 정리 스케줄러 - 예외 발생")
    void cleanupTimeoutBatches_Exception() {
        // Given
        willThrow(new RuntimeException("정리 실패")).given(batchConcurrencyServiceImpl).cleanupTimeoutBatches();

        // When
        assertThatNoException().isThrownBy(() -> scheduler.cleanupTimeoutBatches());

        // Then
        verify(batchConcurrencyServiceImpl).cleanupTimeoutBatches();
    }

    @Test
    @DisplayName("배치 상태 체크 스케줄러 - 실행 중인 배치 없음")
    void checkBatchStatus_NoRunningBatches() {
        // Given
        given(batchConcurrencyServiceImpl.getRunningBatches()).willReturn(List.of());

        // When
        assertThatNoException().isThrownBy(() -> scheduler.checkBatchStatus());

        // Then
        verify(batchConcurrencyServiceImpl).getRunningBatches();
    }

    @Test
    @DisplayName("배치 상태 체크 스케줄러 - 실행 중인 배치 있음")
    void checkBatchStatus_HasRunningBatches() {
        // Given
        List<SettlementBatchExecutionStatus> runningBatches = createRunningBatches();
        given(batchConcurrencyServiceImpl.getRunningBatches()).willReturn(runningBatches);

        // When
        assertThatNoException().isThrownBy(() -> scheduler.checkBatchStatus());

        // Then
        verify(batchConcurrencyServiceImpl).getRunningBatches();
    }

    @Test
    @DisplayName("배치 상태 체크 스케줄러 - 처리 시간 과다 경고")
    void checkBatchStatus_SlowProcessingWarning() {
        // Given
        SettlementBatchExecutionStatus slowBatch = createSlowRunningBatch();
        given(batchConcurrencyServiceImpl.getRunningBatches()).willReturn(List.of(slowBatch));
        given(batchProperties.getNotification()).willReturn(notificationConfig);
        given(notificationConfig.getSlowProcessingThreshold()).willReturn(1800L); // 30분

        // When
        assertThatNoException().isThrownBy(() -> scheduler.checkBatchStatus());

        // Then
        verify(batchConcurrencyServiceImpl).getRunningBatches();
        verify(batchProperties).getNotification();
        verify(notificationConfig).getSlowProcessingThreshold();
    }

    @Test
    @DisplayName("배치 상태 체크 스케줄러 - 예외 발생")
    void checkBatchStatus_Exception() {
        // Given
        given(batchConcurrencyServiceImpl.getRunningBatches())
                .willThrow(new RuntimeException("상태 조회 실패"));

        // When
        assertThatNoException().isThrownBy(() -> scheduler.checkBatchStatus());

        // Then
        verify(batchConcurrencyServiceImpl).getRunningBatches();
    }

    @Test
    @DisplayName("고착 상태 배치 강제 정리 - 2시간 이상 실행 중인 배치")
    void forceCleanupStuckBatches_StuckBatchExists() {
        // Given
        SettlementBatchExecutionStatus stuckBatch = createStuckBatch();
        given(batchConcurrencyServiceImpl.getRunningBatches()).willReturn(List.of(stuckBatch));
        willDoNothing().given(batchConcurrencyServiceImpl).forceReleaseLock(anyString());

        // When
        assertThatNoException().isThrownBy(() -> scheduler.cleanupTimeoutBatches());

        // Then
        verify(batchConcurrencyServiceImpl).getRunningBatches();
        verify(batchConcurrencyServiceImpl).forceReleaseLock("SETTLEMENT_CREATE");
    }

    @Test
    @DisplayName("설정값 검증 - 배치 속성 정보 출력")
    void validateBatchProperties() {
        // Given
        given(batchProperties.isEnabled()).willReturn(true);
        given(batchProperties.getChunkSize()).willReturn(100);
        given(batchProperties.getSkipLimit()).willReturn(10);
        given(batchProperties.getRetryLimit()).willReturn(3);
        BatchExecutionResult successResult = createSuccessResult();
        given(batchExecutionService.executeChunkDailyJob()).willReturn(successResult);

        // When
        scheduler.runChunkDailySettlementJob();

        // Then
        verify(batchProperties).getChunkSize();
        verify(batchProperties).getSkipLimit();
        verify(batchProperties).getRetryLimit();
    }

    // === Helper Methods ===

    private BatchExecutionResult createSuccessResult() {
        // Mock JobExecution for success result
        JobExecution mockJobExecution = new JobExecution(1L);
        mockJobExecution.setStatus(BatchStatus.COMPLETED);
        mockJobExecution.setStartTime(java.time.LocalDateTime.now().minusMinutes(5));
        mockJobExecution.setEndTime(java.time.LocalDateTime.now());

        return BatchExecutionResult.success(mockJobExecution);
    }

    private List<SettlementBatchExecutionStatus> createRunningBatches() {
        SettlementBatchExecutionStatus batch1 = SettlementBatchExecutionStatus.builder()
                .batchName("SETTLEMENT_CREATE")
                .executionStatus(SettlementBatchExecutionStatus.ExecutionStatus.RUNNING)
                .startedAt(OffsetDateTime.now().minusMinutes(30))
                .build();

        SettlementBatchExecutionStatus batch2 = SettlementBatchExecutionStatus.builder()
                .batchName("SETTLEMENT_COMPLETE")
                .executionStatus(SettlementBatchExecutionStatus.ExecutionStatus.RUNNING)
                .startedAt(OffsetDateTime.now().minusMinutes(45))
                .build();

        return List.of(batch1, batch2);
    }

    private SettlementBatchExecutionStatus createSlowRunningBatch() {
        return SettlementBatchExecutionStatus.builder()
                .batchName("SETTLEMENT_CREATE")
                .executionStatus(SettlementBatchExecutionStatus.ExecutionStatus.RUNNING)
                .startedAt(OffsetDateTime.now().minusHours(2)) // 2시간 전 시작
                .build();
    }

    private SettlementBatchExecutionStatus createStuckBatch() {
        return SettlementBatchExecutionStatus.builder()
                .batchName("SETTLEMENT_CREATE")
                .executionStatus(SettlementBatchExecutionStatus.ExecutionStatus.RUNNING)
                .startedAt(OffsetDateTime.now().minusHours(3)) // 3시간 전 시작 (2시간 임계치 초과)
                .build();
    }
}