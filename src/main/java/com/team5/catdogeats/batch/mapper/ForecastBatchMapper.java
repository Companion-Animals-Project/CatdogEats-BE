package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.batch.forecast.domain.ForecastBatchExecutionStatus;
import com.team5.catdogeats.batch.forecast.dto.ForecastBatchItem;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 수요예측 배치 관련 MyBatis 매퍼
 */
@Mapper
public interface ForecastBatchMapper {

    // ================================
    // 배치 실행 상태 관리
    // ================================

    /**
     * 배치 실행 상태 조회
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at,
               last_execution_id, created_at, updated_at
        FROM forecast_batch_execution_status
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
    Optional<ForecastBatchExecutionStatus> findByBatchName(@Param("batchName") String batchName);

    /**
     * 배치 실행 락 획득 (FOR UPDATE)
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at,
               last_execution_id, created_at, updated_at
        FROM forecast_batch_execution_status
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
    Optional<ForecastBatchExecutionStatus> findByBatchNameForUpdate(@Param("batchName") String batchName);

    /**
     * 배치 상태를 RUNNING으로 업데이트
     */
    @Update("""
        UPDATE forecast_batch_execution_status 
        SET execution_status = 'RUNNING',
            last_execution_id = #{executionId},
            started_at = #{startedAt},
            finished_at = NULL,
            updated_at = #{updatedAt}
        WHERE batch_name = #{batchName}
        """)
    int updateToRunning(@Param("batchName") String batchName,
                        @Param("executionId") String executionId,
                        @Param("startedAt") OffsetDateTime startedAt,
                        @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 배치 상태를 COMPLETED로 업데이트
     */
    @Update("""
        UPDATE forecast_batch_execution_status 
        SET execution_status = 'COMPLETED',
            finished_at = #{finishedAt},
            updated_at = #{updatedAt}
        WHERE batch_name = #{batchName}
        """)
    int updateToCompleted(@Param("batchName") String batchName,
                          @Param("finishedAt") OffsetDateTime finishedAt,
                          @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 배치 상태를 FAILED로 업데이트
     */
    @Update("""
        UPDATE forecast_batch_execution_status 
        SET execution_status = 'FAILED',
            finished_at = #{finishedAt},
            updated_at = #{updatedAt}
        WHERE batch_name = #{batchName}
        """)
    int updateToFailed(@Param("batchName") String batchName,
                       @Param("finishedAt") OffsetDateTime finishedAt,
                       @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 배치 상태를 IDLE로 초기화 (강제 락 해제)
     */
    @Update("""
        UPDATE forecast_batch_execution_status 
        SET execution_status = 'IDLE',
            started_at = NULL,
            finished_at = NULL,
            last_execution_id = NULL,
            updated_at = #{updatedAt}
        WHERE batch_name = #{batchName}
        """)
    int updateToIdle(@Param("batchName") String batchName,
                     @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 타임아웃된 배치 목록 조회
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at,
               last_execution_id, created_at, updated_at
        FROM forecast_batch_execution_status
        WHERE execution_status = 'RUNNING'
        AND started_at < #{timeoutThreshold}
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "batchName", column = "batch_name"),
            @Result(property = "executionStatus", column = "execution_status"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "lastExecutionId", column = "last_execution_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<ForecastBatchExecutionStatus> findTimeoutBatches(@Param("timeoutThreshold") OffsetDateTime timeoutThreshold);

    // ================================
    // 배치 데이터 조회 (ItemReader용)
    // ================================


    /**
     * 수요예측 처리 대상 판매자 목록 조회 (페이징)
     */
    @Select("""
        SELECT 
            s.user_id as seller_id,
            u.name as vendor_name,
            CASE WHEN s.deleted_at IS NULL THEN true ELSE false END as is_active,
            DATE(s.created_at) as join_date,
            NULL as last_order_date,
            (
                SELECT COUNT(*)
                FROM products p
                WHERE p.seller_id = s.user_id
            ) as total_product_count,
            (
                SELECT COUNT(*)
                FROM products p
                WHERE p.seller_id = s.user_id
                AND p.stock > 0
            ) as active_product_count
        FROM sellers s
        INNER JOIN users u ON s.user_id = u.id
        WHERE s.deleted_at IS NULL
        AND (
            SELECT COUNT(*)
            FROM products p
            WHERE p.seller_id = s.user_id
            AND p.stock > 0
        ) > 0
        ORDER BY s.created_at ASC
        LIMIT #{chunkSize} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "seller_id", javaType = String.class),
            @Arg(column = "vendor_name", javaType = String.class),
            @Arg(column = "is_active", javaType = Boolean.class),
            @Arg(column = "join_date", javaType = LocalDate.class),
            @Arg(column = "last_order_date", javaType = LocalDate.class),
            @Arg(column = "total_product_count", javaType = Integer.class),
            @Arg(column = "active_product_count", javaType = Integer.class)
    })
    List<ForecastBatchItem> findSellersForForecastBatch(
            @Param("recentActivityThreshold") LocalDate recentActivityThreshold,
            @Param("chunkSize") int chunkSize,
            @Param("offset") int offset);

    /**
     * 수요예측 처리 대상 판매자 총 개수
     */
    @Select("""
        SELECT COUNT(*)
        FROM sellers s
        WHERE s.deleted_at IS NULL
        AND (
            SELECT COUNT(*)
            FROM products p
            WHERE p.seller_id = s.user_id
            AND p.stock > 0
        ) > 0
        """)
    long countSellersForForecastBatch();

    /**
     * 일별 집계 처리 대상 날짜별 주문 건수 조회
     */
    @Select("""
        SELECT COUNT(DISTINCT o.id) as order_count
        FROM orders o
        WHERE DATE(o.created_at) = #{targetDate}
        AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
        AND o.is_hidden = false
        """)
    long countOrdersForAggregation(@Param("targetDate") LocalDate targetDate);

    /**
     * 배치 실행 상태 초기화 (애플리케이션 시작시)
     */
    @Insert("""
        INSERT INTO forecast_batch_execution_status 
        (id, batch_name, execution_status, created_at, updated_at)
        VALUES (#{id}, #{batchName}, 'IDLE', #{now}, #{now})
        ON CONFLICT (batch_name) DO NOTHING
        """)
    void initializeBatchStatus(@Param("id") String id,
                               @Param("batchName") String batchName,
                               @Param("now") OffsetDateTime now);

    /**
     * 모든 배치 상태 조회
     */
    @Select("""
        SELECT id, batch_name, execution_status, started_at, finished_at,
               last_execution_id, created_at, updated_at
        FROM forecast_batch_execution_status
        ORDER BY batch_name
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "batchName", column = "batch_name"),
            @Result(property = "executionStatus", column = "execution_status"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "lastExecutionId", column = "last_execution_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<ForecastBatchExecutionStatus> findAllBatchStatus();
}