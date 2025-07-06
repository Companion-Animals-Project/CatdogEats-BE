package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.batch.domain.SettlementBatchExecutionStatus;
import org.apache.ibatis.annotations.*;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 배치 실행 상태 관리를 위한 MyBatis Mapper
 * 동시성 제어 및 배치 상태 추적용
 */
@Mapper
public interface BatchExecutionStatusMapper {

    /**
     * 배치 이름으로 상태 조회
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at, 
               last_execution_id, created_at, updated_at
        FROM batch_execution_status 
        WHERE batch_name = #{batchName}
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "batchName", column = "batch_name"),
            @Result(property = "executionStatus", column = "execution_status",
                    typeHandler = org.apache.ibatis.type.EnumTypeHandler.class),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "lastExecutionId", column = "last_execution_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<SettlementBatchExecutionStatus> findByBatchName(@Param("batchName") String batchName);

    /**
     * 배치 실행 락 획득 시도 (FOR UPDATE 사용)
     * 동시성 제어를 위해 row-level lock 사용
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at, 
               last_execution_id, created_at, updated_at
        FROM batch_execution_status 
        WHERE batch_name = #{batchName}
        FOR UPDATE NOWAIT
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "batchName", column = "batch_name"),
            @Result(property = "executionStatus", column = "execution_status",
                    typeHandler = org.apache.ibatis.type.EnumTypeHandler.class),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "lastExecutionId", column = "last_execution_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<SettlementBatchExecutionStatus> findByBatchNameForUpdate(@Param("batchName") String batchName);

    /**
     * 배치 상태를 실행중으로 변경
     */
    @Update("""
        UPDATE batch_execution_status 
        SET execution_status = 'RUNNING',
            started_at = #{startedAt},
            finished_at = NULL,
            last_execution_id = #{executionId},
            updated_at = NOW()
        WHERE batch_name = #{batchName}
          AND execution_status IN ('IDLE', 'COMPLETED', 'FAILED')
        """)
    int updateToRunning(@Param("batchName") String batchName,
                        @Param("executionId") String executionId,
                        @Param("startedAt") OffsetDateTime startedAt);

    /**
     * 배치 상태를 완료로 변경
     */
    @Update("""
        UPDATE batch_execution_status 
        SET execution_status = 'COMPLETED',
            finished_at = #{finishedAt},
            updated_at = NOW()
        WHERE batch_name = #{batchName}
          AND execution_status = 'RUNNING'
        """)
    int updateToCompleted(@Param("batchName") String batchName,
                          @Param("finishedAt") OffsetDateTime finishedAt);

    /**
     * 배치 상태를 실패로 변경
     */
    @Update("""
        UPDATE batch_execution_status 
        SET execution_status = 'FAILED',
            finished_at = #{finishedAt},
            updated_at = NOW()
        WHERE batch_name = #{batchName}
          AND execution_status = 'RUNNING'
        """)
    int updateToFailed(@Param("batchName") String batchName,
                       @Param("finishedAt") OffsetDateTime finishedAt);

    /**
     * 배치 상태를 대기중으로 초기화
     */
    @Update("""
        UPDATE batch_execution_status 
        SET execution_status = 'IDLE',
            started_at = NULL,
            finished_at = NULL,
            updated_at = NOW()
        WHERE batch_name = #{batchName}
        """)
    int updateToIdle(@Param("batchName") String batchName);

    /**
     * 실행중인 배치 목록 조회
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at, 
               last_execution_id, created_at, updated_at
        FROM batch_execution_status 
        WHERE execution_status = 'RUNNING'
        ORDER BY started_at DESC
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "batchName", column = "batch_name"),
            @Result(property = "executionStatus", column = "execution_status",
                    typeHandler = org.apache.ibatis.type.EnumTypeHandler.class),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "lastExecutionId", column = "last_execution_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    java.util.List<SettlementBatchExecutionStatus> findRunningBatches();

    /**
     * 특정 시간 이상 실행중인 배치 조회 (데드락 방지용)
     * @param timeoutMinutes 타임아웃 시간(분)
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at, 
               last_execution_id, created_at, updated_at
        FROM batch_execution_status 
        WHERE execution_status = 'RUNNING'
          AND started_at < NOW() - #{timeoutMinutes} * INTERVAL '1 minute'
        ORDER BY started_at ASC
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "batchName", column = "batch_name"),
            @Result(property = "executionStatus", column = "execution_status",
                    typeHandler = org.apache.ibatis.type.EnumTypeHandler.class),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "lastExecutionId", column = "last_execution_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    java.util.List<SettlementBatchExecutionStatus> findTimeoutBatches(@Param("timeoutMinutes") int timeoutMinutes);
}