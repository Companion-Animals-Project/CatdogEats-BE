package com.team5.catdogeats.support.repository;

import com.team5.catdogeats.support.domain.Reports;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Reports, String> {

    // 신고 목록 조회 (관리자용) - 페이징

    @Query("SELECT r FROM Reports r " +
            "WHERE (:reportType IS NULL OR r.reportType = :reportType) " +
            "AND (:status IS NULL OR r.reportStatus = :status) " +
            "AND (:keyword IS NULL OR r.content LIKE %:keyword% OR r.reason LIKE %:keyword%) " +
            "AND (:startDate IS NULL OR r.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR r.createdAt <= :endDate) " +
            "ORDER BY r.createdAt DESC")
    Page<Reports> findReportsWithFilters(
            @Param("reportType") ReportType reportType,
            @Param("status") ReportStatus status,
            @Param("keyword") String keyword,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable
    );

    // 특정 사용자의 신고 목록 조회
    @Query("SELECT r FROM Reports r WHERE r.reporter.userId = :reporterId ORDER BY r.createdAt DESC")
    Page<Reports> findByReporterIdOrderByCreatedAtDesc(@Param("reporterId") String reporterId, Pageable pageable);
    // 특정 대상에 대한 신고 목록 조회
    List<Reports> findByTargetIdOrderByCreatedAtDesc(String targetId);

    // 특정 사용자가 특정 대상에 대해 이미 신고했는지 확인
    @Query("SELECT r FROM Reports r " +
            "WHERE r.reporter.id = :reporterId " +
            "AND r.targetId = :targetId")
    Optional<Reports> findExistingReport(
            @Param("reporterId") String reporterId,
            @Param("targetId") String targetId
    );

    // 신고 ID로 상세 조회 (연관 엔티티 포함)
    @Query("SELECT r FROM Reports r " +
            "LEFT JOIN FETCH r.reporter " +
            "WHERE r.id = :reportId")
    Optional<Reports> findByIdWithDetails(@Param("reportId") String reportId);
}