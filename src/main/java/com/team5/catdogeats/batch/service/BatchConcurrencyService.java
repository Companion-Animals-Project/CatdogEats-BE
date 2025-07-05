package com.team5.catdogeats.batch.service;

import com.team5.catdogeats.batch.domain.entity.BatchExecutionStatus;
import com.team5.catdogeats.batch.mapper.BatchExecutionStatusMapper;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 배치 동시성 제어 서비스
 * DB 기반 분산락을 통한 배치 중복 실행 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchConcurrencyService {

    private final BatchExecutionStatusMapper batchExecutionStatusMapper;

    // 배치 타임아웃 시간 (기본 60분)
    private static final int DEFAULT_TIMEOUT_MINUTES = 60;

    /**
     * 배치 실행 락 획득 시도
     *
     * @param batchName 배치 이름
     * @param executionId 실행 ID
     * @return 락 획득 성공 여부
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquireLock(String batchName, String executionId) {
        try {
            log.info("배치 실행 락 획득 시도 - batchName: {}, executionId: {}", batchName, executionId);

            // FOR UPDATE NOWAIT로 락 획득 시도
            Optional<BatchExecutionStatus> status = batchExecutionStatusMapper.findByBatchNameForUpdate(batchName);

            if (status.isEmpty()) {
                log.error("배치 상태 정보가 존재하지 않습니다 - batchName: {}", batchName);
                return false;
            }

            BatchExecutionStatus currentStatus = status.get();

            // 실행 가능한 상태인지 확인
            if (!currentStatus.canExecute()) {
                log.warn("배치가 이미 실행중입니다 - batchName: {}, currentStatus: {}",
                        batchName, currentStatus.getExecutionStatus());
                return false;
            }

            // 상태를 RUNNING으로 변경
            int updateCount = batchExecutionStatusMapper.updateToRunning(
                    batchName, executionId, OffsetDateTime.now());

            if (updateCount == 1) {
                log.info("배치 실행 락 획득 성공 - batchName: {}, executionId: {}", batchName, executionId);
                return true;
            } else {
                log.warn("배치 상태 업데이트 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
                return false;
            }

        } catch (CannotAcquireLockException e) {
            log.warn("배치 실행 락 획득 실패 (다른 프로세스에서 실행중) - batchName: {}", batchName);
            return false;
        } catch (Exception e) {
            log.error("배치 실행 락 획득 중 예외 발생 - batchName: {}", batchName, e);
            return false;
        }
    }

    /**
     * 배치 완료 처리 (락 해제)
     *
     * @param batchName 배치 이름
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLockAsCompleted(String batchName) {
        try {
            log.info("배치 완료 처리 - batchName: {}", batchName);

            int updateCount = batchExecutionStatusMapper.updateToCompleted(batchName, OffsetDateTime.now());

            if (updateCount == 1) {
                log.info("배치 완료 처리 성공 - batchName: {}", batchName);
            } else {
                log.warn("배치 완료 처리 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
            }

        } catch (Exception e) {
            log.error("배치 완료 처리 중 예외 발생 - batchName: {}", batchName, e);
        }
    }

    /**
     * 배치 실패 처리 (락 해제)
     *
     * @param batchName 배치 이름
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLockAsFailed(String batchName) {
        try {
            log.info("배치 실패 처리 - batchName: {}", batchName);

            int updateCount = batchExecutionStatusMapper.updateToFailed(batchName, OffsetDateTime.now());

            if (updateCount == 1) {
                log.info("배치 실패 처리 성공 - batchName: {}", batchName);
            } else {
                log.warn("배치 실패 처리 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
            }

        } catch (Exception e) {
            log.error("배치 실패 처리 중 예외 발생 - batchName: {}", batchName, e);
        }
    }

    /**
     * 배치 상태 초기화 (강제 락 해제)
     * 관리자 기능으로 사용
     *
     * @param batchName 배치 이름
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    public void forceReleaseLock(String batchName) {
        try {
            log.info("배치 상태 강제 초기화 - batchName: {}", batchName);

            int updateCount = batchExecutionStatusMapper.updateToIdle(batchName);

            if (updateCount == 1) {
                log.info("배치 상태 강제 초기화 성공 - batchName: {}", batchName);
            } else {
                log.warn("배치 상태 강제 초기화 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
            }

        } catch (Exception e) {
            log.error("배치 상태 강제 초기화 중 예외 발생 - batchName: {}", batchName, e);
        }
    }

    /**
     * 현재 실행중인 배치 목록 조회
     */
    @MybatisTransactional(readOnly = true)
    public List<BatchExecutionStatus> getRunningBatches() {
        return batchExecutionStatusMapper.findRunningBatches();
    }

    /**
     * 타임아웃된 배치 정리
     * 스케줄러에서 주기적으로 호출하여 데드락 방지
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupTimeoutBatches() {
        try {
            List<BatchExecutionStatus> timeoutBatches =
                    batchExecutionStatusMapper.findTimeoutBatches(DEFAULT_TIMEOUT_MINUTES);

            if (!timeoutBatches.isEmpty()) {
                log.warn("타임아웃된 배치 발견 - count: {}", timeoutBatches.size());

                for (BatchExecutionStatus batch : timeoutBatches) {
                    log.warn("타임아웃 배치 정리 - batchName: {}, startedAt: {}",
                            batch.getBatchName(), batch.getStartedAt());

                    batchExecutionStatusMapper.updateToFailed(batch.getBatchName(), OffsetDateTime.now());
                }
            }

        } catch (Exception e) {
            log.error("타임아웃 배치 정리 중 예외 발생", e);
        }
    }

    /**
     * 배치 실행 가능 여부 확인
     */
    @MybatisTransactional(readOnly = true)
    public boolean canExecuteBatch(String batchName) {
        Optional<BatchExecutionStatus> status = batchExecutionStatusMapper.findByBatchName(batchName);
        return status.map(BatchExecutionStatus::canExecute).orElse(false);
    }

    /**
     * 배치 상태 조회
     */
    @MybatisTransactional(readOnly = true)
    public Optional<BatchExecutionStatus> getBatchStatus(String batchName) {
        return batchExecutionStatusMapper.findByBatchName(batchName);
    }
}