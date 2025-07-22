//package com.team5.catdogeats.batch.forecast.scheduler;
//
//import com.team5.catdogeats.batch.config.ForecastBatchProperties;
//import com.team5.catdogeats.batch.scheduler.ForecastBatchScheduler;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.batch.core.JobExecution;
//import org.springframework.batch.core.JobInstance;
//import org.springframework.batch.core.JobParameters;
//
//import java.time.LocalDateTime;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("수요예측 배치 스케줄러 테스트")
//class ForecastBatchSchedulerTest {
//
//    @InjectMocks
//    private ForecastBatchScheduler forecastBatchScheduler;
//
//    @Mock
//    private ForecastBatchExecutionService batchExecutionService;
//
//    @Mock
//    private ForecastBatchConcurrencyService batchConcurrencyService;
//
//    @Mock
//    private ForecastBatchProperties forecastBatchProperties;
//
//    @Mock
//    private JobExecution mockJobExecution;
//
//    @Mock
//    private JobInstance mockJobInstance;
//
//    @Mock
//    private JobParameters mockJobParameters;
//
//    private BatchExecutionResult successResult;
//    private BatchExecutionResult failureResult;
//
//    @BeforeEach
//    void setUp() {
//        // 성공/실패 결과만 생성 (JobExecution stubbing은 필요한 테스트에서만 개별 설정)
//        successResult = BatchExecutionResult.success(mockJobExecution);
//        failureResult = BatchExecutionResult.failed("처리 중 오류 발생");
//    }
//
//    /**
//     * JobExecution Mock을 설정하는 헬퍼 메서드
//     * 필요한 테스트에서만 호출하여 불필요한 stubbing 방지
//     */
//    private void setupJobExecutionMock() {
//        given(mockJobExecution.getJobInstance()).willReturn(mockJobInstance);
//        given(mockJobExecution.getJobParameters()).willReturn(mockJobParameters);
//        given(mockJobExecution.getStartTime()).willReturn(LocalDateTime.now().minusMinutes(5));
//        given(mockJobExecution.getEndTime()).willReturn(LocalDateTime.now());
//        // getExecutionSummary에서 사용되는 getStepExecutions() stubbing 추가
//        given(mockJobExecution.getStepExecutions()).willReturn(java.util.Collections.emptyList());
//    }
//
//    @Test
//    @DisplayName("일별 집계 배치 수동 실행 - 성공")
//    void runAggregationJobManually_Success() {
//        // Given
//        given(batchExecutionService.executeAggregationJob()).willReturn(successResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runAggregationJobManually();
//
//        // Then
//        assertThat(result).isEqualTo(successResult);
//        assertThat(result.isSuccess()).isTrue();
//
//        verify(batchExecutionService).executeAggregationJob();
//    }
//
//    @Test
//    @DisplayName("일별 집계 배치 수동 실행 - 실패")
//    void runAggregationJobManually_Failure() {
//        // Given
//        given(batchExecutionService.executeAggregationJob()).willReturn(failureResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runAggregationJobManually();
//
//        // Then
//        assertThat(result).isEqualTo(failureResult);
//        assertThat(result.isSuccess()).isFalse();
//
//        verify(batchExecutionService).executeAggregationJob();
//    }
//
//    @Test
//    @DisplayName("일별 집계 배치 수동 실행 - 예외 발생")
//    void runAggregationJobManually_Exception() {
//        // Given
//        given(batchExecutionService.executeAggregationJob())
//                .willThrow(new RuntimeException("배치 실행 중 예외"));
//
//        // When & Then
//        assertThatThrownBy(() -> forecastBatchScheduler.runAggregationJobManually())
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage("수요예측 일별 집계 배치 작업 실행 실패");
//
//        verify(batchExecutionService).executeAggregationJob();
//    }
//
//    @Test
//    @DisplayName("수요예측 실행 배치 수동 실행 - 성공")
//    void runPredictionJobManually_Success() {
//        // Given
//        given(batchExecutionService.executePredictionJob()).willReturn(successResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runPredictionJobManually();
//
//        // Then
//        assertThat(result).isEqualTo(successResult);
//        assertThat(result.isSuccess()).isTrue();
//
//        verify(batchExecutionService).executePredictionJob();
//    }
//
//    @Test
//    @DisplayName("수요예측 실행 배치 수동 실행 - 실패")
//    void runPredictionJobManually_Failure() {
//        // Given
//        given(batchExecutionService.executePredictionJob()).willReturn(failureResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runPredictionJobManually();
//
//        // Then
//        assertThat(result).isEqualTo(failureResult);
//        assertThat(result.isSuccess()).isFalse();
//
//        verify(batchExecutionService).executePredictionJob();
//    }
//
//    @Test
//    @DisplayName("수요예측 실행 배치 수동 실행 - 예외 발생")
//    void runPredictionJobManually_Exception() {
//        // Given
//        given(batchExecutionService.executePredictionJob())
//                .willThrow(new RuntimeException("배치 실행 중 예외"));
//
//        // When & Then
//        assertThatThrownBy(() -> forecastBatchScheduler.runPredictionJobManually())
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage("수요예측 실행 배치 작업 실행 실패");
//
//        verify(batchExecutionService).executePredictionJob();
//    }
//
//    @Test
//    @DisplayName("데이터 정리 배치 수동 실행 - 성공")
//    void runCleanupJobManually_Success() {
//        // Given
//        given(batchExecutionService.executeCleanupJob()).willReturn(successResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runCleanupJobManually();
//
//        // Then
//        assertThat(result).isEqualTo(successResult);
//        assertThat(result.isSuccess()).isTrue();
//
//        verify(batchExecutionService).executeCleanupJob();
//    }
//
//    @Test
//    @DisplayName("데이터 정리 배치 수동 실행 - 실패")
//    void runCleanupJobManually_Failure() {
//        // Given
//        given(batchExecutionService.executeCleanupJob()).willReturn(failureResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runCleanupJobManually();
//
//        // Then
//        assertThat(result).isEqualTo(failureResult);
//        assertThat(result.isSuccess()).isFalse();
//
//        verify(batchExecutionService).executeCleanupJob();
//    }
//
//    @Test
//    @DisplayName("데이터 정리 배치 수동 실행 - 예외 발생")
//    void runCleanupJobManually_Exception() {
//        // Given
//        given(batchExecutionService.executeCleanupJob())
//                .willThrow(new RuntimeException("배치 실행 중 예외"));
//
//        // When & Then
//        assertThatThrownBy(() -> forecastBatchScheduler.runCleanupJobManually())
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage("수요예측 데이터 정리 배치 작업 실행 실패");
//
//        verify(batchExecutionService).executeCleanupJob();
//    }
//
//    @Test
//    @DisplayName("스케줄 실행 - 배치 비활성화 상태")
//    void runAggregationJob_BatchDisabled() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(false);
//
//        // When
//        forecastBatchScheduler.runAggregationJob();
//
//        // Then
//        verify(batchExecutionService, never()).executeAggregationJob();
//        verify(forecastBatchProperties).isEnabled();
//    }
//
//    @Test
//    @DisplayName("스케줄 실행 - 배치 활성화 상태에서 성공")
//    void runAggregationJob_BatchEnabled_Success() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(forecastBatchProperties.getChunkSize()).willReturn(100);
//        given(forecastBatchProperties.getSkipLimit()).willReturn(10);
//        given(forecastBatchProperties.getRetryLimit()).willReturn(3);
//        given(batchExecutionService.executeAggregationJob()).willReturn(successResult);
//
//        // When
//        forecastBatchScheduler.runAggregationJob();
//
//        // Then
//        verify(batchExecutionService).executeAggregationJob();
//        verify(forecastBatchProperties).isEnabled();
//        verify(forecastBatchProperties).getChunkSize();
//        verify(forecastBatchProperties).getSkipLimit();
//        verify(forecastBatchProperties).getRetryLimit();
//    }
//
//    @Test
//    @DisplayName("스케줄 실행 - 배치 활성화 상태에서 실패")
//    void runAggregationJob_BatchEnabled_Failure() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(forecastBatchProperties.getChunkSize()).willReturn(100);
//        given(forecastBatchProperties.getSkipLimit()).willReturn(10);
//        given(forecastBatchProperties.getRetryLimit()).willReturn(3);
//        given(batchExecutionService.executeAggregationJob()).willReturn(failureResult);
//
//        // When
//        forecastBatchScheduler.runAggregationJob();
//
//        // Then
//        verify(batchExecutionService).executeAggregationJob();
//        verify(forecastBatchProperties).isEnabled();
//        verify(forecastBatchProperties).getChunkSize();
//        verify(forecastBatchProperties).getSkipLimit();
//        verify(forecastBatchProperties).getRetryLimit();
//    }
//
//    @Test
//    @DisplayName("수요예측 스케줄 실행 - 배치 비활성화 상태")
//    void runPredictionJob_BatchDisabled() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(false);
//
//        // When
//        forecastBatchScheduler.runPredictionJob();
//
//        // Then
//        verify(batchExecutionService, never()).executePredictionJob();
//        verify(forecastBatchProperties).isEnabled();
//    }
//
//    @Test
//    @DisplayName("수요예측 스케줄 실행 - 배치 활성화 상태에서 성공")
//    void runPredictionJob_BatchEnabled_Success() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(batchExecutionService.executePredictionJob()).willReturn(successResult);
//
//        // When
//        forecastBatchScheduler.runPredictionJob();
//
//        // Then
//        verify(batchExecutionService).executePredictionJob();
//        verify(forecastBatchProperties).isEnabled();
//    }
//
//    @Test
//    @DisplayName("데이터 정리 스케줄 실행 - 배치 비활성화 상태")
//    void runCleanupJob_BatchDisabled() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(false);
//
//        // When
//        forecastBatchScheduler.runCleanupJob();
//
//        // Then
//        verify(batchExecutionService, never()).executeCleanupJob();
//        verify(forecastBatchProperties).isEnabled();
//    }
//
//    @Test
//    @DisplayName("데이터 정리 스케줄 실행 - 배치 활성화 상태에서 성공")
//    void runCleanupJob_BatchEnabled_Success() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(batchExecutionService.executeCleanupJob()).willReturn(successResult);
//
//        // When
//        forecastBatchScheduler.runCleanupJob();
//
//        // Then
//        verify(batchExecutionService).executeCleanupJob();
//        verify(forecastBatchProperties).isEnabled();
//    }
//
//    @Test
//    @DisplayName("타임아웃 배치 정리 - 정상 실행")
//    void cleanupTimeoutBatches_Success() {
//        // Given
//        willDoNothing().given(batchConcurrencyService).cleanupTimeoutBatches();
//        willDoNothing().given(batchConcurrencyService).forceCleanupStuckBatches();
//
//        // When
//        forecastBatchScheduler.cleanupTimeoutBatches();
//
//        // Then
//        verify(batchConcurrencyService).cleanupTimeoutBatches();
//        verify(batchConcurrencyService).forceCleanupStuckBatches();
//    }
//
//    @Test
//    @DisplayName("타임아웃 배치 정리 - 예외 발생")
//    void cleanupTimeoutBatches_Exception() {
//        // Given
//        willThrow(new RuntimeException("정리 중 오류"))
//                .given(batchConcurrencyService).cleanupTimeoutBatches();
//
//        // When
//        forecastBatchScheduler.cleanupTimeoutBatches();
//
//        // Then
//        verify(batchConcurrencyService).cleanupTimeoutBatches();
//        // 예외가 발생해도 메서드는 정상 종료되어야 함 (로그만 출력)
//    }
//
//    @Test
//    @DisplayName("애플리케이션 시작 시 초기화 - 정상 실행")
//    void cleanupOnStartup_Success() {
//        // Given
//        willDoNothing().given(batchConcurrencyService).initializeBatchStatuses();
//        willDoNothing().given(batchConcurrencyService).forceCleanupStuckBatches();
//
//        // When
//        forecastBatchScheduler.cleanupOnStartup();
//
//        // Then
//        verify(batchConcurrencyService).initializeBatchStatuses();
//        verify(batchConcurrencyService).forceCleanupStuckBatches();
//    }
//
//    @Test
//    @DisplayName("애플리케이션 시작 시 초기화 - 예외 발생")
//    void cleanupOnStartup_Exception() {
//        // Given
//        willThrow(new RuntimeException("초기화 중 오류"))
//                .given(batchConcurrencyService).initializeBatchStatuses();
//
//        // When
//        forecastBatchScheduler.cleanupOnStartup();
//
//        // Then
//        verify(batchConcurrencyService).initializeBatchStatuses();
//        // 예외가 발생해도 메서드는 정상 종료되어야 함 (로그만 출력)
//    }
//
//    @Test
//    @DisplayName("스케줄 실행 시 예외 처리 - 집계 배치")
//    void runAggregationJob_ExceptionHandling() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(forecastBatchProperties.getChunkSize()).willReturn(100);
//        given(forecastBatchProperties.getSkipLimit()).willReturn(10);
//        given(forecastBatchProperties.getRetryLimit()).willReturn(3);
//        given(batchExecutionService.executeAggregationJob())
//                .willThrow(new RuntimeException("예상치 못한 오류"));
//
//        // When
//        forecastBatchScheduler.runAggregationJob();
//
//        // Then
//        verify(batchExecutionService).executeAggregationJob();
//        // 예외가 발생해도 스케줄러 메서드는 정상 종료되어야 함
//    }
//
//    @Test
//    @DisplayName("스케줄 실행 시 예외 처리 - 수요예측 배치")
//    void runPredictionJob_ExceptionHandling() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(batchExecutionService.executePredictionJob())
//                .willThrow(new RuntimeException("예상치 못한 오류"));
//
//        // When
//        forecastBatchScheduler.runPredictionJob();
//
//        // Then
//        verify(batchExecutionService).executePredictionJob();
//        // 예외가 발생해도 스케줄러 메서드는 정상 종료되어야 함
//    }
//
//    @Test
//    @DisplayName("스케줄 실행 시 예외 처리 - 데이터 정리 배치")
//    void runCleanupJob_ExceptionHandling() {
//        // Given
//        given(forecastBatchProperties.isEnabled()).willReturn(true);
//        given(batchExecutionService.executeCleanupJob())
//                .willThrow(new RuntimeException("예상치 못한 오류"));
//
//        // When
//        forecastBatchScheduler.runCleanupJob();
//
//        // Then
//        verify(batchExecutionService).executeCleanupJob();
//        // 예외가 발생해도 스케줄러 메서드는 정상 종료되어야 함
//    }
//
//
//
//    @Test
//    @DisplayName("실패 결과 검증 - 에러 메시지 확인")
//    void failureResult_ErrorMessage() {
//        // Given
//        given(batchExecutionService.executeAggregationJob()).willReturn(failureResult);
//
//        // When
//        BatchExecutionResult result = forecastBatchScheduler.runAggregationJobManually();
//
//        // Then
//        assertThat(result.isSuccess()).isFalse();
//        assertThat(result.getMessage()).isEqualTo("처리 중 오류 발생");
//        assertThat(result.getJobExecution()).isNull();
//    }
//}