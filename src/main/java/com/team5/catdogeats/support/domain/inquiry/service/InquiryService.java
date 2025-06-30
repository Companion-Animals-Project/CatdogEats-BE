package com.team5.catdogeats.support.domain.inquiry.service;

import com.team5.catdogeats.support.domain.inquiry.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 1:1 문의 서비스 인터페이스
public interface InquiryService {

    // === 사용자 기능 (판매자, 구매자) - 간소화된 정보 ===

    // 사용자용 문의 목록 조회 (간소화)
    Page<UserInquiryListResponseDTO> getUserInquiries(String providerId, Pageable pageable);

    // 사용자용 문의 상세 조회 (간소화)
    UserInquiryDetailResponseDTO getUserInquiryDetail(String inquiryId, String providerId);

    // 문의 등록 (상세한 정보 반환)
    InquiryResponseDTO createInquiry(String providerId, InquiryCreateRequestDTO request);


    // === 관리자 전용 기능 - 전체 정보 ===

    // 관리자용 모든 문의 목록 조회 (전체 정보)
    Page<InquiryListResponseDTO> getAllInquiries(Pageable pageable);

    // 관리자용 문의 상세 조회 (전체 정보)
    InquiryDetailResponseDTO getInquiryDetailForAdmin(String inquiryId);

    // 답변 등록 (관리자만)
    InquiryResponseDTO createReply(String inquiryId, String adminId, InquiryReplyRequestDTO request);
}