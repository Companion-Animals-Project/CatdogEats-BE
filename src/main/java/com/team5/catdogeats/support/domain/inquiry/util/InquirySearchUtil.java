package com.team5.catdogeats.support.domain.inquiry.util;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.InquirySearchRequestDTO;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 1:1 문의 검색을 위한 유틸리티 클래스
 * JPA Specification을 활용한 동적 쿼리 조건들을 정의
 */
public class InquirySearchUtil {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 루트 문의만 조회 (답글 제외)
     */
    public static Specification<Inquires> isRootInquiry() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("parent"));
    }

    /**
     * 키워드 검색 (제목 + 내용)
     */
    public static Specification<Inquires> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }

            String likePattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likePattern)
            );
        };
    }

    /**
     * 답변 상태 필터
     */
    public static Specification<Inquires> hasStatus(InquiryStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("inquiryStatus"), status);
    }

    /**
     * 문의 유형 필터
     */
    public static Specification<Inquires> hasType(InquiryType type) {
        return (root, query, criteriaBuilder) ->
                type == null ? null : criteriaBuilder.equal(root.get("inquiryType"), type);
    }

    /**
     * 긴급도 필터
     */
    public static Specification<Inquires> hasUrgentLevel(InquiryUrgentLevel urgentLevel) {
        return (root, query, criteriaBuilder) ->
                urgentLevel == null ? null : criteriaBuilder.equal(root.get("inquiryUrgentLevel"), urgentLevel);
    }

    /**
     * 날짜 범위 필터
     */
    public static Specification<Inquires> hasDateRange(LocalDate startDate, LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }

            if (startDate != null && endDate != null) {
                // 시작일 00:00:00 ~ 종료일 23:59:59
                ZonedDateTime startDateTime = startDate.atStartOfDay(KOREA_ZONE);
                ZonedDateTime endDateTime = endDate.plusDays(1).atStartOfDay(KOREA_ZONE);

                return criteriaBuilder.between(
                        root.get("createdAt"),
                        startDateTime,
                        endDateTime
                );
            } else if (startDate != null) {
                // 시작일 이후
                ZonedDateTime startDateTime = startDate.atStartOfDay(KOREA_ZONE);
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDateTime);
            } else {
                // 종료일 이전
                ZonedDateTime endDateTime = endDate.plusDays(1).atStartOfDay(KOREA_ZONE);
                return criteriaBuilder.lessThan(root.get("createdAt"), endDateTime);
            }
        };
    }

    /**
     * N+1 문제 해결을 위한 JOIN FETCH
     */
    public static Specification<Inquires> withUserAndOrderJoinFetch() {
        return (root, query, criteriaBuilder) -> {
            // SELECT 쿼리에만 JOIN FETCH 적용 (COUNT 쿼리에는 적용하지 않음)
            if (query.getResultType() == Long.class || query.getResultType() == long.class) {
                return null;
            }

            // LEFT JOIN FETCH로 연관 엔티티를 한 번에 로딩
            root.fetch("users", JoinType.LEFT);
            root.fetch("orders", JoinType.LEFT);

            return null;
        };
    }

    /**
     * 검색 조건 통합 메서드
     */
    public static Specification<Inquires> searchInquiries(InquirySearchRequestDTO searchRequest) {
        return Specification.where(isRootInquiry())
                .and(hasKeyword(searchRequest.getCleanKeyword()))
                .and(hasStatus(searchRequest.status()))
                .and(hasType(searchRequest.type()))
                .and(hasUrgentLevel(searchRequest.urgentLevel()))
                .and(hasDateRange(searchRequest.startDate(), searchRequest.endDate()))
                .and(withUserAndOrderJoinFetch());
    }
}