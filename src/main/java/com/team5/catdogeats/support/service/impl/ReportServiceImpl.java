package com.team5.catdogeats.support.service.impl;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.support.domain.Reports;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportCreateRequestDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.repository.ReportRepository;
import com.team5.catdogeats.support.service.ReportService;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public String createReport(ReportCreateRequestDto request, String reporterId) {
        log.info("신고 생성 요청: 사용자={}, 타입={}, 대상ID={}", reporterId, request.reportType(), request.targetId());

        // 신고자 조회
        Buyers reporter = buyerRepository.findById(reporterId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 중복 신고 체크
        Optional<Reports> existingReport = reportRepository.findExistingReport(reporterId, request.targetId());
        if (existingReport.isPresent()) {
            throw new IllegalStateException("이미 신고한 대상입니다.");
        }

        // 신고 대상 검증
        validateReportTarget(request.reportType(), request.targetId());

        // 신고 엔티티 생성
        Reports report = Reports.builder()
                .reportType(request.reportType())
                .reason(request.reason())
                .content(request.content())
                .targetId(request.targetId())
                .reporter(reporter)
                .attachmentUrl(request.attachmentUrl())
                .reportStatus(ReportStatus.PENDING)
                .build();

        Reports savedReport = reportRepository.save(report);
        log.info("신고 생성 완료: ID={}", savedReport.getId());

        return savedReport.getId();
    }

    @Override
    public PageResponseDto<ReportListResponseDto> getUserReports(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Reports> reportPage = reportRepository.findByReporterIdOrderByCreatedAtDesc(userId, pageable);

        List<ReportListResponseDto> content = reportPage.getContent().stream()
                .map(this::mapToListResponseDto)
                .toList();

        return PageResponseDto.of(content, page, size, reportPage.getTotalElements());
    }

    // === 헬퍼 메서드 ===

    // 신고 대상 검증
    private void validateReportTarget(ReportType reportType, String targetId) {
        if (reportType == ReportType.PRODUCT) {
            productRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("신고 대상 상품을 찾을 수 없습니다."));
        } else if (reportType == ReportType.REVIEW) {
            reviewRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("신고 대상 리뷰를 찾을 수 없습니다."));
        }
    }

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
}