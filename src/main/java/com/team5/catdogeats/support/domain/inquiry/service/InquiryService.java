package com.team5.catdogeats.support.domain.inquiry.service;

import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

// 1:1 문의 서비스 인터페이스
public interface InquiryService {

    // === 사용자 기능 (판매자, 구매자) - 간소화된 정보 ===

    // 사용자용 문의 목록 조회 (간소화)
    Page<InquiryListResponseDTO> getUserInquiries(String providerId, Pageable pageable);

    // 사용자용 문의 상세 조회 (간소화)
    InquiryDetailResponseDTO getUserInquiryDetail(String inquiryId, String providerId);

    // 최초 문의 등록 (상세한 정보 반환)
    InquiryResponseDTO createInquiry(String providerId, InquiryCreateRequestDTO request);

    // 사용자용 답글 등록 (스레드 형태)
    InquiryResponseDTO createUserFollowup(String inquiryId, String providerId, String content);

    // 유저 문의 종료 (사유 없음)
    InquiryResponseDTO closeInquiryByUser(String inquiryId, String providerId);



    // === 관리자 전용 기능 - 전체 정보 ===

    // 관리자용 모든 문의 목록 조회 (전체 정보)
    Page<InquiryListResponseDTO> getAllInquiries(Pageable pageable);

    // 관리자용 문의 상세 조회 (전체 정보)
    InquiryDetailResponseDTO getInquiryDetailForAdmin(String inquiryId);

    // 관리자 답변 (최초/추가)
    InquiryResponseDTO createAdminReply(String inquiryId, String adminId, String content);

    // 관리자 강제 종료 (사유 필수)
    InquiryResponseDTO closeInquiryByAdmin(String inquiryId, String adminId, String reason);

    // 긴급도 수정
    InquiryResponseDTO updateUrgentLevel(String inquiryId, InquiryUrgentLevel urgentLevel);

    // 🆕 추가 메서드 (3개)
    InquiryResponseDTO createInquiryWithFiles(InquiryCreateRequestDTO request,
                                              MultipartFile[] imageFiles,
                                              String providerId);

    InquiryResponseDTO createUserFollowupWithFiles(String inquiryId,
                                                   String content,
                                                   MultipartFile[] imageFiles,
                                                   String providerId);

    InquiryResponseDTO createAdminReplyWithFiles(String inquiryId,
                                                 String content,
                                                 MultipartFile[] imageFiles,
                                                 MultipartFile[] documentFiles,
                                                 String adminId);

}