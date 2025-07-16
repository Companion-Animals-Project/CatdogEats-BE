package com.team5.catdogeats.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 스케줄링 설정 클래스
 * 정산 스케줄러 등의 백그라운드 작업을 위한 설정
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 스케줄러 전용 스레드 풀 설정 (스케줄러 작업이 서로 블로킹되지 않도록)
        taskRegistrar.setScheduler(taskExecutor());
    }

    /**
     * 스케줄러 전용 스레드 풀
     * 정산 스케줄러와 기타 스케줄된 작업들을 병렬로 실행
     */
    public Executor taskExecutor() {
        log.info("스케줄러 스레드 풀 초기화 - 스레드 수: 3");
        return Executors.newScheduledThreadPool(3);
    }
}
