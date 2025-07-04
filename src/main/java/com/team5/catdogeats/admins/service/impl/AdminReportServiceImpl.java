package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.dto.ReportDetailResponseDto;
import com.team5.catdogeats.admins.domain.dto.ReportSearchDto;
import com.team5.catdogeats.admins.domain.dto.ReportStatsResponseDto;
import com.team5.catdogeats.admins.domain.dto.ReportStatusUpdateDto;
import com.team5.catdogeats.admins.service.AdminReportService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.support.domain.Reports;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;
import com.team5.catdogeats.support.exception.ReportNotFoundException;
import com.team5.catdogeats.support.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public PageResponseDto<ReportListResponseDto> getReports(ReportSearchDto searchDto) {
        ZonedDateTime startDate = searchDto.startDate() != null ?
                searchDto.startDate().atStartOfDay(java.time.ZoneId.systemDefault()) : null;
        ZonedDateTime endDate = searchDto.endDate() != null ?
                searchDto.endDate().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()) : null;

        Pageable pageable = PageRequest.of(searchDto.page(), searchDto.size());

        Page<Reports> reportPage = reportRepository.findReportsWithFilters(
                searchDto.reportType(),
                searchDto.status(),
                searchDto.keyword(),
                startDate,
                endDate,
                pageable
        );

        List<ReportListResponseDto> content = reportPage.getContent().stream()
                .map(this::mapToListResponseDto)
                .toList();

        return PageResponseDto.of(content, searchDto.page(), searchDto.size(), reportPage.getTotalElements());
    }

    @Override
    public ReportDetailResponseDto getReportDetail(String reportId) {
        Reports report = reportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다."));

        return mapToDetailResponseDto(report);
    }

    @Override
    @Transactional
    public void updateReportStatus(String reportId, ReportStatusUpdateDto updateDto, String adminId) {
        log.info("신고 상태 변경: ID={}, 상태={}, 관리자={}", reportId, updateDto.reportStatus(), adminId);

        Reports report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다."));

        report.updateStatus(updateDto.reportStatus(), adminId, updateDto.adminNote());

        log.info("신고 상태 변경 완료: ID={}, 새 상태={}", reportId, updateDto.reportStatus());
    }

    @Override
    public ReportStatsResponseDto getReportStats() {
        // 상태별 통계
        List<Object[]> statusStats = reportRepository.countReportsByStatus();

        // 타입별 통계
        List<Object[]> typeStats = reportRepository.countReportsByType();

        // 최근 7일 통계
        ZonedDateTime weekAgo = ZonedDateTime.now().minusDays(7);
        List<Object[]> weeklyStats = reportRepository.countReportsLast7Days(weekAgo);

        // 처리 대기 건수
        long pendingCount = reportRepository.countByReportStatus(ReportStatus.PENDING);

        return ReportStatsResponseDto.builder()
                .statusStats(statusStats)
                .typeStats(typeStats)
                .weeklyStats(weeklyStats)
                .pendingCount(pendingCount)
                .build();
    }

    // === 헬퍼 메서드 ===

    // Reports 엔티티를 ReportListResponseDto로 변환
    private ReportListResponseDto mapToListResponseDto(Reports report) {
        String targetName = getTargetName(report.getReportType(), report.getTargetId());

        return ReportListResponseDto.builder()
                .reportId(report.getId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .reportStatus(report.getReportStatus())
                .reporterName(report.getReporter().getUser().getName())
                .reporterId(report.getReporter().getUserId())
                .targetId(report.getTargetId())
                .targetName(targetName)
                .createdAt(report.getCreatedAt())
                .build();
    }

    // Reports 엔티티를 ReportDetailResponseDto로 변환
    private ReportDetailResponseDto mapToDetailResponseDto(Reports report) {
        String targetName = getTargetName(report.getReportType(), report.getTargetId());
        String targetDescription = getTargetDescription(report.getReportType(), report.getTargetId());

        return ReportDetailResponseDto.builder()
                .reportId(report.getId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .content(report.getContent())
                .reportStatus(report.getReportStatus())
                .reporterName(report.getReporter().getUser().getName())
                .reporterId(report.getReporter().getUserId())
                .targetId(report.getTargetId())
                .targetName(targetName)
                .targetDescription(targetDescription)
                .attachmentUrl(report.getAttachmentUrl())
                .adminNote(report.getAdminNote())
                .processedByAdminId(report.getProcessedByAdminId())
                .createdAt(report.getCreatedAt())
                .processedAt(report.getProcessedAt())
                .build();
    }

    // 신고 대상의 이름 조회
    private String getTargetName(ReportType reportType, String targetId) {
        if (reportType == ReportType.PRODUCT) {
            return productRepository.findById(targetId)
                    .map(Products::getTitle)
                    .orElse("삭제된 상품");
        } else if (reportType == ReportType.REVIEW) {
            return "리뷰 #" + targetId.substring(0, 8);
        }
        return "알 수 없음";
    }

    // 신고 대상의 상세 설명 조회
    private String getTargetDescription(ReportType reportType, String targetId) {
        if (reportType == ReportType.PRODUCT) {
            return productRepository.findById(targetId)
                    .map(Products::getTitle)
                    .orElse("상세 정보 없음");
        } else if (reportType == ReportType.REVIEW) {
            return reviewRepository.findById(targetId)
                    .map(Reviews::getContents)
                    .orElse("상세 정보 없음");
        }
        return "상세 정보 없음";
    }
}