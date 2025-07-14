package com.team5.catdogeats.support.domain.inquiry.service;


// 문의 긴급도 상승 서비스 인터페이스
// 시간 경과에 따른 문의 긴급도 관리를 담당합니다.
public interface InquiryEscalationService {


    // 특정 문의의 긴급도를 실시간으로 검토하고 필요시 업데이트합니다.
    // 문의 조회 시 호출하여 최신 긴급도를 반영할 수 있습니다.
    void checkAndUpdateUrgency(String inquiryId);
}