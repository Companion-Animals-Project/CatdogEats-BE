package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.repository.InquiryRepository;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryEscalationService;
import com.team5.catdogeats.support.domain.inquiry.util.InquiryUrgencyEscalationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 문의 긴급도 상승 서비스 구현체
// 주기적으로 미답변 문의들의 긴급도를 검토하고 필요시 상승시킵니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryEscalationServiceImpl implements InquiryEscalationService {

    private final InquiryRepository inquiryRepository;


    // 특정 문의의 긴급도 실시간 검토 (문의 조회 시 호출 가능)
    @Override
    @JpaTransactional
    public void checkAndUpdateUrgency(String inquiryId) {
        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다: " + inquiryId));

        InquiryUrgentLevel currentLevel = inquiry.getInquiryUrgentLevel();
        InquiryUrgentLevel escalatedLevel = InquiryUrgencyEscalationUtil.calculateEscalatedUrgency(
                inquiry.getCreatedAt(),
                currentLevel,
                inquiry.getInquiryStatus(),
                inquiry.getInquiryType()
        );

        if (!escalatedLevel.equals(currentLevel)) {
            inquiry.setInquiryUrgentLevel(escalatedLevel);
            log.info("실시간 긴급도 상승 - ID: {}, {} -> {}",
                    inquiryId, currentLevel.getDisplayName(), escalatedLevel.getDisplayName());
        }

    }
}