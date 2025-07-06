package com.team5.catdogeats.batch.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 배치 실행 상태 엔티티
 * 동시성 제어를 위한 배치 실행 상태 관리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchExecutionStatus {

    private Long id;

    /**
     * 배치 작업 이름 (고유값)
     * SETTLEMENT_CREATE, SETTLEMENT_UPDATE, SETTLEMENT_COMPLETE
     */
    private String batchName;

    /**
     * 실행 상태
     * IDLE: 대기중, RUNNING: 실행중, COMPLETED: 완료, FAILED: 실패
     */
    private ExecutionStatus executionStatus;

    /**
     * 배치 시작 시간
     */
    private OffsetDateTime startedAt;

    /**
     * 배치 완료 시간
     */
    private OffsetDateTime finishedAt;

    /**
     * 마지막 실행 ID (Spring Batch Job Execution ID)
     */
    private String lastExecutionId;

    /**
     * 생성 시간
     */
    private OffsetDateTime createdAt;

    /**
     * 수정 시간
     */
    private OffsetDateTime updatedAt;

    /**
     * 배치 실행 상태 열거형
     */
    public enum ExecutionStatus {
        IDLE("대기중"),
        RUNNING("실행중"),
        COMPLETED("완료"),
        FAILED("실패");

        private final String description;

        ExecutionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 배치 이름 열거형
     */
    public enum BatchName {
        SETTLEMENT_CREATE("SETTLEMENT_CREATE"),
        SETTLEMENT_UPDATE("SETTLEMENT_UPDATE"),
        SETTLEMENT_COMPLETE("SETTLEMENT_COMPLETE");

        private final String value;

        BatchName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    /**
     * 실행 가능한지 확인 (대기중이거나 완료/실패 상태)
     */
    public boolean canExecute() {
        return ExecutionStatus.IDLE.equals(this.executionStatus)
                || ExecutionStatus.COMPLETED.equals(this.executionStatus)
                || ExecutionStatus.FAILED.equals(this.executionStatus);
    }
}