package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 1:1 문의 검색 및 필터링 요청 DTO
 * 관리자 페이지에서 사용
 */
public record InquirySearchRequestDTO(
        @Schema(description = "검색 키워드 (제목 + 내용)", example = "결제 관련")
        String keyword,

        @Schema(description = "답변 상태",
                allowableValues = {"PENDING", "ANSWERED", "FOLLOWUP", "CLOSED", "FORCE_CLOSED"})
        InquiryStatus status,

        @Schema(description = "문의 유형",
                allowableValues = {"PRODUCT", "ORDER", "PAYMENT", "DELIVERY", "RETURN", "ACCOUNT", "ETC"})
        InquiryType type,

        @Schema(description = "긴급도",
                allowableValues = {"HIGH", "MIDDLE", "LOW"})
        InquiryUrgentLevel urgentLevel,

        @Schema(description = "검색 시작일 (yyyy-MM-dd)", example = "2025-01-01")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(description = "검색 종료일 (yyyy-MM-dd)", example = "2025-01-31")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate
) {
    /**
     * 검색 조건이 있는지 확인
     */
    public boolean hasSearchConditions() {
        return hasKeyword() || hasStatus() || hasType() || hasUrgentLevel() || hasDateRange();
    }

    /**
     * 키워드 검색 조건 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 상태 필터 조건 확인
     */
    public boolean hasStatus() {
        return status != null;
    }

    /**
     * 유형 필터 조건 확인
     */
    public boolean hasType() {
        return type != null;
    }

    /**
     * 긴급도 필터 조건 확인
     */
    public boolean hasUrgentLevel() {
        return urgentLevel != null;
    }

    /**
     * 날짜 범위 필터 조건 확인
     */
    public boolean hasDateRange() {
        return startDate != null || endDate != null;
    }

    /**
     * 정리된 키워드 반환 (trim + 소문자)
     */
    public String getCleanKeyword() {
        return hasKeyword() ? keyword.trim().toLowerCase() : null;
    }

    /**
     * 검색 조건 요약 (로깅용)
     */
    public String getSearchSummary() {
        StringBuilder summary = new StringBuilder();

        if (hasKeyword()) summary.append("키워드: ").append(keyword).append(", ");
        if (hasStatus()) summary.append("상태: ").append(status.getDisplayName()).append(", ");
        if (hasType()) summary.append("유형: ").append(type.getDisplayName()).append(", ");
        if (hasUrgentLevel()) summary.append("긴급도: ").append(urgentLevel.getDisplayName()).append(", ");
        if (hasDateRange()) {
            summary.append("기간: ");
            if (startDate != null) summary.append(startDate);
            summary.append(" ~ ");
            if (endDate != null) summary.append(endDate);
            summary.append(", ");
        }

        return summary.length() > 0
                ? summary.substring(0, summary.length() - 2)
                : "조건 없음";
    }
}