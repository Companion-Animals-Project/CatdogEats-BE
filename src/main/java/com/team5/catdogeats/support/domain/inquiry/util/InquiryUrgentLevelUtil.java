package com.team5.catdogeats.support.domain.inquiry.util;

import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


// 1:1 문의 긴급도 관련 유틸리티 클래스
// 문의 유형에 따른 기본 긴급도 설정 로직을 담당합니다.
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InquiryUrgentLevelUtil {


    // 문의 유형에 따른 기본 긴급도를 반환합니다.
    // 긴급도 분류 기준
    // HIGH: 결제, 환불/교환 (돈 관련)
    // MIDDLE: 주문, 배송, 제품, 계정 (진행 관련)
    // LOW: 기타 (일반 문의)
    public static InquiryUrgentLevel getDefaultUrgentLevel(InquiryType inquiryType) {
        if (inquiryType == null) {
            throw new IllegalArgumentException("문의 유형은 필수입니다");
        }

        InquiryUrgentLevel urgentLevel = switch (inquiryType) {
            case PAYMENT, RETURN -> InquiryUrgentLevel.HIGH;           // 돈 관련 - 높음
            case ORDER, DELIVERY, PRODUCT, ACCOUNT -> InquiryUrgentLevel.MIDDLE; // 진행 관련 - 중간
            case ETC -> InquiryUrgentLevel.LOW;               // 일반 문의 - 낮음
        };

        log.debug("문의 유형 '{}' -> 긴급도 '{}' 자동 설정",
                inquiryType.getDisplayName(), urgentLevel.getDisplayName());

        return urgentLevel;
    }


    // 특정 문의 유형이 높은 긴급도에 해당하는지 확인합니다.
    public static boolean isHighUrgency(InquiryType inquiryType) {
        return getDefaultUrgentLevel(inquiryType) == InquiryUrgentLevel.HIGH;
    }


    // 특정 문의 유형이 중간 긴급도에 해당하는지 확인합니다.
    public static boolean isMiddleUrgency(InquiryType inquiryType) {
        return getDefaultUrgentLevel(inquiryType) == InquiryUrgentLevel.MIDDLE;
    }


    // 특정 문의 유형이 낮은 긴급도에 해당하는지 확인합니다.
    public static boolean isLowUrgency(InquiryType inquiryType) {
        return getDefaultUrgentLevel(inquiryType) == InquiryUrgentLevel.LOW;
    }
}