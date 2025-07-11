package com.team5.catdogeats.batch.service.impl;

import com.team5.catdogeats.batch.config.ForecastBatchProperties;
import com.team5.catdogeats.batch.ForecastBatchExecutionStatus;
import com.team5.catdogeats.batch.mapper.ForecastBatchMapper;
import com.team5.catdogeats.batch.service.ForecastBatchConcurrencyService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 수요예측 배치 동시성 제어 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastBatchConcurrencyServiceImpl implements ForecastBatchConcurrencyService {

    private final ForecastBatchMapper forecastBatchMapper;
    private final ForecastBatchProperties forecastBatchProperties;

    /**
     * 배치 실행 락 획득 시도
     *
     * @param batchName 배치 이름
     * @param executionId 실행 ID
     * @return 락 획득 성공 여부
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean tryAcquireLock(String batchName, String executionId) {
        try {
            log.info("수요예측 배치 실행 락 획득 시도 - batchName: {}, executionId: {}", batchName, executionId);

            // FOR UPDATE NOWAIT로 락 획득 시도
            Optional<ForecastBatchExecutionStatus> status = forecastBatchMapper.findByBatchNameForUpdate(batchName);

            if (status.isEmpty()) {
                log.error("수요예측 배치 상태 정보가 존재하지 않습니다 - batchName: {}", batchName);
                return false;
            }

            ForecastBatchExecutionStatus currentStatus = status.get();

            // 실행 가능한 상태인지 확인
            if (!currentStatus.canExecute()) {
                log.warn("수요예측 배치가 이미 실행중입니다 - batchName: {}, currentStatus: {}",
                        batchName, currentStatus.getExecutionStatus());
                return false;
            }

            // 상태를 RUNNING으로 변경
            OffsetDateTime now = OffsetDateTime.now();
            int updateCount = forecastBatchMapper.updateToRunning(batchName, executionId, now, now);

            if (updateCount == 1) {
                log.info("수요예측 배치 실행 락 획득 성공 - batchName: {}, executionId: {}", batchName, executionId);
                return true;
            } else {
                log.warn("수요예측 배치 상태 업데이트 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
                return false;
            }

        } catch (CannotAcquireLockException e) {
            log.warn("수요예측 배치 실행 락 획득 실패 (다른 프로세스에서 실행중) - batchName: {}", batchName);
            return false;
        } catch (Exception e) {
            log.error("수요예측 배치 실행 락 획득 중 예외 발생 - batchName: {}", batchName, e);
            return false;
        }
    }

    /**
     * 배치 완료 처리 (락 해제)
     *
     * @param batchName 배치 이름
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void releaseLockAsCompleted(String batchName) {
        try {
            log.info("수요예측 배치 완료 처리 - batchName: {}", batchName);

            OffsetDateTime now = OffsetDateTime.now();
            int updateCount = forecastBatchMapper.updateToCompleted(batchName, now, now);

            if (updateCount == 1) {
                log.info("수요예측 배치 완료 처리 성공 - batchName: {}", batchName);
            } else {
                log.warn("수요예측 배치 완료 처리 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
            }

        } catch (Exception e) {
            log.error("수요예측 배치 완료 처리 중 예외 발생 - batchName: {}", batchName, e);
        }
    }

    /**
     * 배치 실패 처리 (락 해제)
     *
     * @param batchName 배치 이름
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void releaseLockAsFailed(String batchName) {
        try {
            log.info("수요예측 배치 실패 처리 - batchName: {}", batchName);

            OffsetDateTime now = OffsetDateTime.now();
            int updateCount = forecastBatchMapper.updateToFailed(batchName, now, now);

            if (updateCount == 1) {
                log.info("수요예측 배치 실패 처리 성공 - batchName: {}", batchName);
            } else {
                log.warn("수요예측 배치 실패 처리 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
            }

        } catch (Exception e) {
            log.error("수요예측 배치 실패 처리 중 예외 발생 - batchName: {}", batchName, e);
        }
    }

    /**
     * 배치 상태 초기화 (강제 락 해제)
     * 관리자 기능으로 사용
     *
     * @param batchName 배치 이름
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void forceReleaseLock(String batchName) {
        try {
            log.info("수요예측 배치 상태 강제 초기화 - batchName: {}", batchName);

            OffsetDateTime now = OffsetDateTime.now();
            int updateCount = forecastBatchMapper.updateToIdle(batchName, now);

            if (updateCount == 1) {
                log.info("수요예측 배치 상태 강제 초기화 성공 - batchName: {}", batchName);
            } else {
                log.warn("수요예측 배치 상태 강제 초기화 실패 - batchName: {}, updateCount: {}", batchName, updateCount);
            }

        } catch (Exception e) {
            log.error("수요예측 배치 상태 강제 초기화 중 예외 발생 - batchName: {}", batchName, e);
        }
    }

    /**
     * 현재 실행중인 배치 목록 조회
     */
    @MybatisTransactional(readOnly = true)
    @Override
    public List<ForecastBatchExecutionStatus> getCurrentRunningBatches() {
        try {
            List<ForecastBatchExecutionStatus> allStatus = forecastBatchMapper.findAllBatchStatus();
            return allStatus.stream()
                    .filter(ForecastBatchExecutionStatus::isRunning)
                    .toList();

        } catch (Exception e) {
            log.error("실행중인 수요예측 배치 목록 조회 중 예외 발생", e);
            return List.of();
        }
    }

    /**
     * 타임아웃된 배치 정리
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void cleanupTimeoutBatches() {
        try {
            log.debug("타임아웃된 수요예측 배치 정리 시작");

            // 타임아웃 기준 시간 계산
            OffsetDateTime timeoutThreshold = OffsetDateTime.now()
                    .minusMinutes(forecastBatchProperties.getTimeoutMinutes());

            List<ForecastBatchExecutionStatus> timeoutBatches =
                    forecastBatchMapper.findTimeoutBatches(timeoutThreshold);

            if (timeoutBatches.isEmpty()) {
                log.debug("타임아웃된 수요예측 배치가 없습니다");
                return;
            }

            log.warn("타임아웃된 수요예측 배치 발견 - 개수: {}", timeoutBatches.size());

            // 각 타임아웃 배치를 FAILED로 변경
            for (ForecastBatchExecutionStatus batch : timeoutBatches) {
                try {
                    releaseLockAsFailed(batch.getBatchName());
                    log.warn("타임아웃 배치 정리 완료 - batchName: {}, 실행시간: {}분",
                            batch.getBatchName(),
                            java.time.Duration.between(
                                    batch.getStartedAt(), OffsetDateTime.now()).toMinutes());

                } catch (Exception e) {
                    log.error("타임아웃 배치 정리 실패 - batchName: {}", batch.getBatchName(), e);
                }
            }

            log.info("타임아웃된 수요예측 배치 정리 완료 - 처리 개수: {}", timeoutBatches.size());

        } catch (Exception e) {
            log.error("타임아웃 수요예측 배치 정리 중 예외 발생", e);
        }
    }

    /**
     * 애플리케이션 시작시 배치 상태 초기화
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void initializeBatchStatuses() {
        try {
            log.info("수요예측 배치 상태 초기화 시작");

            OffsetDateTime now = OffsetDateTime.now();

            // 모든 배치 타입에 대해 상태 초기화
            for (ForecastBatchExecutionStatus.BatchName batchName :
                    ForecastBatchExecutionStatus.BatchName.values()) {

                String id = UUID.randomUUID().toString();
                forecastBatchMapper.initializeBatchStatus(id, batchName.getValue(), now);

                log.debug("수요예측 배치 상태 초기화 - batchName: {}", batchName.getValue());
            }

            log.info("수요예측 배치 상태 초기화 완료");

        } catch (Exception e) {
            log.error("수요예측 배치 상태 초기화 중 예외 발생", e);
        }
    }

    /**
     * 강제로 모든 실행중인 배치를 정리
     * 애플리케이션 시작시 비정상 종료된 배치들 정리용
     */
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void forceCleanupStuckBatches() {
        try {
            log.info("비정상 종료된 수요예측 배치 강제 정리 시작");

            List<ForecastBatchExecutionStatus> runningBatches = getCurrentRunningBatches();

            if (runningBatches.isEmpty()) {
                log.debug("정리할 실행중인 수요예측 배치가 없습니다");
                return;
            }

            log.warn("실행중인 수요예측 배치 발견 - 강제 정리 시작, 개수: {}", runningBatches.size());

            for (ForecastBatchExecutionStatus batch : runningBatches) {
                forceReleaseLock(batch.getBatchName());
                log.info("비정상 배치 정리 완료 - batchName: {}", batch.getBatchName());
            }

            log.info("비정상 종료된 수요예측 배치 강제 정리 완료 - 처리 개수: {}", runningBatches.size());

        } catch (Exception e) {
            log.error("비정상 수요예측 배치 강제 정리 중 예외 발생", e);
        }
    }

    /**
     * 배치 상태 조회
     */
    @MybatisTransactional(readOnly = true)
    @Override
    public Optional<ForecastBatchExecutionStatus> getBatchStatus(String batchName) {
        try {
            return forecastBatchMapper.findByBatchName(batchName);
        } catch (Exception e) {
            log.error("수요예측 배치 상태 조회 중 예외 발생 - batchName: {}", batchName, e);
            return Optional.empty();
        }
    }

    /**
     * 모든 배치 상태 조회
     */
    @MybatisTransactional(readOnly = true)
    @Override
    public List<ForecastBatchExecutionStatus> getAllBatchStatuses() {
        try {
            return forecastBatchMapper.findAllBatchStatus();
        } catch (Exception e) {
            log.error("모든 수요예측 배치 상태 조회 중 예외 발생", e);
            return List.of();
        }
    }
}