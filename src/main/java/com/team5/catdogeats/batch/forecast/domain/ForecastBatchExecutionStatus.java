package com.team5.catdogeats.batch.forecast.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 수요예측 배치 실행 상태 도메인
 * 동시성 제어 및 배치 실행 상태 추적용
 */
@Entity
@Table(name = "forecast_batch_execution_status",
        indexes = {
                @Index(name = "idx_forecast_batch_name", columnList = "batch_name"),
                @Index(name = "idx_forecast_execution_status", columnList = "execution_status"),
                @Index(name = "idx_forecast_started_at", columnList = "started_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_forecast_batch_name", columnNames = "batch_name")
        })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ForecastBatchExecutionStatus extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "batch_name", nullable = false, unique = true)
    private String batchName;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    private ExecutionStatus executionStatus;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "last_execution_id")
    private String lastExecutionId;

    /**
     * 배치 실행 상태 Enum
     */
    @Getter
    public enum ExecutionStatus {
        IDLE("대기"),
        RUNNING("실행중"),
        COMPLETED("완료"),
        FAILED("실패");

        private final String description;

        ExecutionStatus(String description) {
            this.description = description;
        }

    }

    /**
     * 배치 이름 Enum (수요예측 관련)
     */
    public enum BatchName {
        FORECAST_AGGREGATION("forecast_aggregation"),
        FORECAST_PREDICTION("forecast_prediction"),
        FORECAST_CLEANUP("forecast_cleanup");

        private final String value;

        BatchName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 배치 실행 가능 여부 확인
     */
    public boolean canExecute() {
        return ExecutionStatus.IDLE.equals(this.executionStatus) ||
                ExecutionStatus.COMPLETED.equals(this.executionStatus) ||
                ExecutionStatus.FAILED.equals(this.executionStatus);
    }

    /**
     * 배치 실행중 여부 확인
     */
    public boolean isRunning() {
        return ExecutionStatus.RUNNING.equals(this.executionStatus);
    }


    /**
     * 배치 실패 여부 확인
     */
    public boolean isFailed() {
        return ExecutionStatus.FAILED.equals(this.executionStatus);
    }


    /**
     * 실행 시간 계산 (초)
     */
    public Long getExecutionTimeSeconds() {
        if (startedAt == null) return null;

        OffsetDateTime endTime = finishedAt != null ? finishedAt : OffsetDateTime.now();
        return java.time.Duration.between(startedAt, endTime).getSeconds();
    }

    /**
     * 배치 상태 요약 정보
     */
    public String getStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("배치: ").append(batchName)
                .append(", 상태: ").append(executionStatus.getDescription());

        if (startedAt != null) {
            summary.append(", 시작: ").append(startedAt);
        }

        if (finishedAt != null) {
            summary.append(", 완료: ").append(finishedAt);
        }

        Long executionTime = getExecutionTimeSeconds();
        if (executionTime != null) {
            summary.append(", 소요시간: ").append(executionTime).append("초");
        }

        return summary.toString();
    }
}