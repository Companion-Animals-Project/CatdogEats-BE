package com.team5.catdogeats.support.domain.inquiry.util;

import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

// 1:1 문의 긴급도, 시간 경과시 상승하는 유틸리티
// 1:1 문의 등록 후 시간 경과에 따라 긴급도를 자동 상승시키는 로직을 담당합니다.
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InquiryUrgencyEscalationUtil {

    // 긴급도 상승 기준 시간 (시간 단위)
    private static final int PRODUCT_ESCALATION_HOURS = 48;   // 제품 문의 상승 기준 (48시간)
    private static final int STANDARD_ESCALATION_HOURS = 72;  // 표준(주문,배송,계정) 문의 상승 기준 (72시간)
    private static final int ETC_FIRST_ESCALATION_HOURS = 72;  // 기타 문의 1단계 상승 (72시간)
    private static final int ETC_FINAL_ESCALATION_HOURS = 120; // 기타 문의 최종 상승 (120시간)


    // 문의의 현재 상태와 경과 시간을 고려하여 적절한 긴급도를 계산합니다.
    public static InquiryUrgentLevel calculateEscalatedUrgency(
            ZonedDateTime createdAt,
            InquiryUrgentLevel currentUrgentLevel,
            InquiryStatus inquiryStatus,
            InquiryType inquiryType) {

        // 이미 답변 완료된 문의는 상승하지 않음
        if (inquiryStatus == InquiryStatus.ANSWERED) {
            return currentUrgentLevel;
        }

        // 현재 시간과의 차이 계산
        ZonedDateTime now = ZonedDateTime.now();
        long hoursElapsed = ChronoUnit.HOURS.between(createdAt, now);

        log.debug("긴급도 상승 검사 - 경과시간: {}시간, 현재긴급도: {}, 문의유형: {}",
                hoursElapsed, currentUrgentLevel, inquiryType);

        // 문의 유형별 상승 정책 적용
        return applyEscalationPolicy(hoursElapsed, currentUrgentLevel, inquiryType);
    }


    // 문의 유형별 긴급도 상승 정책을 적용합니다.
    private static InquiryUrgentLevel applyEscalationPolicy(
            long hoursElapsed,
            InquiryUrgentLevel currentLevel,
            InquiryType inquiryType) {

        // 이미 HIGH인 경우 더 이상 상승하지 않음
        if (currentLevel == InquiryUrgentLevel.HIGH) {
            return currentLevel;
        }

        // 문의 유형별 차별화된 상승 기준
        return switch (inquiryType) {
            case PAYMENT, RETURN -> currentLevel;
            case PRODUCT -> applyHealthRelatedEscalation(hoursElapsed, currentLevel);
            case ORDER, DELIVERY, ACCOUNT -> applyStandardEscalation(hoursElapsed, currentLevel);
            case ETC -> applyLowPriorityEscalation(hoursElapsed, currentLevel);
        };
    }

    // 건강 관련 문의 (제품) 상승 정책
    // 건강 이슈 가능성을 고려한 빠른 상승
    private static InquiryUrgentLevel applyHealthRelatedEscalation(long hoursElapsed, InquiryUrgentLevel currentLevel) {
        if (hoursElapsed >= PRODUCT_ESCALATION_HOURS) { // 48시간 후 HIGH로 상승
            log.info("건강 관련 문의 긴급도 상승: {} -> HIGH ({}시간 경과)", currentLevel, hoursElapsed);
            return InquiryUrgentLevel.HIGH;
        }
        return currentLevel;
    }


    // 표준 문의 (주문/배송/계정) 상승 정책
    private static InquiryUrgentLevel applyStandardEscalation(long hoursElapsed, InquiryUrgentLevel currentLevel) {
        if (hoursElapsed >= STANDARD_ESCALATION_HOURS) { // 72시간(3일) 후 HIGH로 상승
            log.info("표준 문의 긴급도 상승: {} -> HIGH ({}시간 경과)", currentLevel, hoursElapsed);
            return InquiryUrgentLevel.HIGH;
        }
        return currentLevel;
    }


    // 저우선순위 문의 (기타) 상승 정책
    private static InquiryUrgentLevel applyLowPriorityEscalation(long hoursElapsed, InquiryUrgentLevel currentLevel) {
        if (hoursElapsed >= ETC_FINAL_ESCALATION_HOURS) { // 120시간(5일) 후 HIGH로 상승
            log.info("저우선순위 문의 긴급도 상승: {} -> HIGH ({}시간 경과)", currentLevel, hoursElapsed);
            return InquiryUrgentLevel.HIGH;
        } else if (hoursElapsed >= ETC_FIRST_ESCALATION_HOURS) { // 72시간 후 MIDDLE로 상승 (ETC는 LOW -> MIDDLE -> HIGH 단계적 상승)
            if (currentLevel == InquiryUrgentLevel.LOW) {
                log.info("저우선순위 문의 1단계 상승: LOW -> MIDDLE ({}시간 경과)", hoursElapsed);
                return InquiryUrgentLevel.MIDDLE;
            }
        }
        return currentLevel;
    }


    // 문의가 긴급도 상승 대상인지 확인합니다.
        public static boolean needsEscalation(
            ZonedDateTime createdAt,
            InquiryUrgentLevel currentLevel,
            InquiryStatus status,
            InquiryType type) {

        InquiryUrgentLevel escalatedLevel = calculateEscalatedUrgency(createdAt, currentLevel, status, type);
        return !escalatedLevel.equals(currentLevel);
    }
}