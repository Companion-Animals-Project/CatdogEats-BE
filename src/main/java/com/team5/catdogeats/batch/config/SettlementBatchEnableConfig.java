package com.team5.catdogeats.batch.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 정산 배치 스케줄링 활성화 설정
 * application.yml의 settlement.batch.enabled 값에 따라 스케줄링 활성화/비활성화
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "batch.settlement.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SettlementBatchEnableConfig {
    // 설정만 담당하므로 별도 구현 없음
}
