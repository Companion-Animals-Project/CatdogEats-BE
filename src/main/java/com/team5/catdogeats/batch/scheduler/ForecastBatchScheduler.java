package com.team5.catdogeats.batch.scheduler;

import com.team5.catdogeats.batch.config.ForecastBatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수요예측 배치 스케줄러 (단순화된 버전)
 * Spring Batch JobRepository를 사용한 표준 패턴
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "batch.forecast.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ForecastBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job forecastAggregationJob;
    private final Job forecastPredictionJob;
    private final Job forecastCleanupJob;
    private final ForecastBatchProperties forecastBatchProperties;

    /**
     * 일별 판매 집계 스케줄러 (매일 새벽 2시)
     */
    @Scheduled(cron = "${batch.forecast.aggregation-cron}")
    public void runAggregationJob() {
        if (!forecastBatchProperties.isEnabled()) {
            log.info("수요예측 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        executeJob(forecastAggregationJob, "일별 판매 집계");
    }

    /**
     * 수요예측 실행 스케줄러 (매일 새벽 3시)
     */
    @Scheduled(cron = "${batch.forecast.forecast-cron}")
    public void runPredictionJob() {
        if (!forecastBatchProperties.isEnabled()) {
            log.info("수요예측 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        executeJob(forecastPredictionJob, "수요예측 실행");
    }

    /**
     * 데이터 정리 스케줄러 (매주 일요일 새벽 4시)
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void runCleanupJob() {
        if (!forecastBatchProperties.isEnabled()) {
            log.info("수요예측 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        executeJob(forecastCleanupJob, "데이터 정리");
    }

    /**
     * 수동 실행용 메서드들
     */
    public JobExecution runAggregationJobManually() {
        return executeJob(forecastAggregationJob, "수동 일별 판매 집계");
    }

    public JobExecution runPredictionJobManually() {
        return executeJob(forecastPredictionJob, "수동 수요예측 실행");
    }

    public JobExecution runCleanupJobManually() {
        return executeJob(forecastCleanupJob, "수동 데이터 정리");
    }

    /**
     * 통합된 Job 실행 메서드
     */
    private JobExecution executeJob(Job job, String description) {
        log.info("{} 배치 작업 시작", description);
        long startTime = System.currentTimeMillis();

        try {
            // JobParameters 생성 (타임스탬프로 중복 방지)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("description", description)
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            // 동기 실행 대기
            while (jobExecution.isRunning()) {
                Thread.sleep(1000);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            if (BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {
                log.info("{} 배치 작업 완료 - 실행시간: {}ms", description, executionTime);
                logExecutionSummary(jobExecution);
            } else {
                log.error("{} 배치 작업 실패 - 상태: {}, 실행시간: {}ms",
                        description, jobExecution.getStatus(), executionTime);
                logFailureDetails(jobExecution);
            }

            return jobExecution;

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("{} 배치가 이미 실행중입니다", description);
            throw new RuntimeException("배치가 이미 실행중입니다", e);
        } catch (JobRestartException e) {
            log.error("{} 배치 재시작 오류", description, e);
            throw new RuntimeException("배치 재시작 오류", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("{} 배치가 이미 완료되었습니다", description);
            throw new RuntimeException("배치가 이미 완료되었습니다", e);
        } catch (JobParametersInvalidException e) {
            log.error("{} 배치 파라미터 오류", description, e);
            throw new RuntimeException("잘못된 Job 파라미터", e);
        } catch (Exception e) {
            log.error("{} 배치 작업 중 예상치 못한 오류 발생", description, e);
            throw new RuntimeException("배치 실행 실패", e);
        }
    }

    /**
     * 실행 결과 요약 로깅
     */
    private void logExecutionSummary(JobExecution jobExecution) {
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            String stepName = stepExecution.getStepName();

            if ("aggregationStep".equals(stepName)) {
                int aggregatedRecords = stepExecution.getExecutionContext().getInt("aggregatedRecords", 0);
                log.info("일별 집계: {}건", aggregatedRecords);

            } else if ("predictionStep".equals(stepName)) {
                log.info("수요예측: Read {}건, Write {}건, Skip {}건",
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount());

            } else if ("cleanupStep".equals(stepName)) {
                int deletedRecords = stepExecution.getExecutionContext().getInt("deletedRecords", 0);
                log.info("데이터 정리: {}건 삭제", deletedRecords);
            }
        });
    }

    /**
     * 실패 상세 정보 로깅
     */
    private void logFailureDetails(JobExecution jobExecution) {
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            if (BatchStatus.FAILED.equals(stepExecution.getStatus())) {
                log.error("실패한 Step: {}, 종료 코드: {}",
                        stepExecution.getStepName(),
                        stepExecution.getExitStatus().getExitDescription());
            }
        });
    }
}