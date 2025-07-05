package com.team5.catdogeats.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementDailyJob;
    private final Job settlementMonthlyJob;

    /**
     * 정산 데이터 생성/갱신 스케줄러 (설정 파일의 cron 사용)
     * - 정산 데이터 생성: 배송완료된 주문아이템들에 대해 정산 생성
     * - 정산 상태 갱신: 배송완료 후 설정된 일수 경과한 정산을 PENDING -> IN_PROGRESS로 변경
     */
    @Scheduled(cron = "${batch.settlement.daily-cron}")
    public void runDailySettlementJob() {
        try {
            log.info("📊 정산 일일 배치 작업 시작 - cron: ${batch.settlement.daily-cron}");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", "daily")
                    .addString("description", "정산 데이터 생성 및 상태 갱신")
                    .toJobParameters();

            jobLauncher.run(settlementDailyJob, jobParameters);

            log.info("✅ 정산 일일 배치 작업 완료");
        } catch (Exception e) {
            log.error("❌ 정산 일일 배치 작업 실패", e);
        }
    }

    /**
     * 정산 완료 처리 스케줄러 (설정 파일의 cron 사용)
     * - 정산 완료: IN_PROGRESS 상태의 정산들을 COMPLETED로 변경
     */
    @Scheduled(cron = "${batch.settlement.monthly-cron}")
    public void runMonthlySettlementJob() {
        try {
            log.info("💰 정산 월간 완료 배치 작업 시작 - cron: ${batch.settlement.monthly-cron}");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", "monthly")
                    .addString("description", "정산 완료 처리")
                    .toJobParameters();

            jobLauncher.run(settlementMonthlyJob, jobParameters);

            log.info("✅ 정산 월간 완료 배치 작업 완료");
        } catch (Exception e) {
            log.error("❌ 정산 월간 완료 배치 작업 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 일일 배치 (테스트 및 관리자 기능)
     */
    public void runDailyJobManually() {
        try {
            log.info("🔧 수동 정산 일일 배치 작업 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", "manual-daily")
                    .addString("triggerBy", "manual")
                    .toJobParameters();

            jobLauncher.run(settlementDailyJob, jobParameters);

            log.info("✅ 수동 정산 일일 배치 작업 완료");
        } catch (Exception e) {
            log.error("❌ 수동 정산 일일 배치 작업 실패", e);
            throw new RuntimeException("정산 일일 배치 작업 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 월간 배치 (테스트 및 관리자 기능)
     */
    public void runMonthlyJobManually() {
        try {
            log.info("🔧 수동 정산 월간 완료 배치 작업 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", "manual-monthly")
                    .addString("triggerBy", "manual")
                    .toJobParameters();

            jobLauncher.run(settlementMonthlyJob, jobParameters);

            log.info("✅ 수동 정산 월간 완료 배치 작업 완료");
        } catch (Exception e) {
            log.error("❌ 수동 정산 월간 완료 배치 작업 실패", e);
            throw new RuntimeException("정산 월간 완료 배치 작업 실행 실패", e);
        }
    }
}