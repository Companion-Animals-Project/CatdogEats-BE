package com.team5.catdogeats.support.domain.inquiry.service;

import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquirySearchRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryDetailResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryListResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquiryCreateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

// 1:1 문의 서비스 인터페이스
public interface InquiryService {

    // === 사용자 기능 (판매자, 구매자) - 간소화된 정보 ===

    // 사용자용 문의 목록 조회
    Page<InquiryListResponseDTO> getUserInquiries(String provider, String providerId, Pageable pageable);

    // 사용자용 문의 상세 조회
    InquiryDetailResponseDTO getUserInquiryDetail(String inquiryId, String provider, String providerId);

    // 최초 문의 등록
    InquiryResponseDTO createInquiry(String provider, String providerId, InquiryCreateRequestDTO request);

    // 사용자용 답글 등록 (스레드 형태)
    InquiryResponseDTO createUserFollowup(String inquiryId, String provider, String providerId, String content);

    // 유저 문의 종료
    InquiryResponseDTO closeInquiryByUser(String inquiryId, String provider, String providerId);



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


    // 파일 포함 메서드
    InquiryResponseDTO createInquiryWithFiles(InquiryCreateRequestDTO request,
                                              MultipartFile[] imageFiles,
                                              String provider,
                                              String providerId);

    InquiryResponseDTO createUserFollowupWithFiles(String inquiryId,
                                                   String content,
                                                   MultipartFile[] imageFiles,
                                                   String provider,
                                                   String providerId);

    InquiryResponseDTO createAdminReplyWithFiles(String inquiryId,
                                                 String content,
                                                 MultipartFile[] imageFiles,
                                                 MultipartFile[] documentFiles,
                                                 String adminId);

    /**
     * 관리자용 문의 검색 및 필터링 (동적 쿼리)
     */
    Page<InquiryListResponseDTO> searchInquiries(InquirySearchRequestDTO searchRequest, Pageable pageable);


    /**
     * 관리자용 문의 검색 및 필터링 (기존 메서드와 통합)
     * - 검색 조건이 없으면 전체 조회
     * - 검색 조건이 있으면 동적 쿼리 실행
     */
    Page<InquiryListResponseDTO> getAllInquiriesWithSearchAndPaging(
            String keyword,
            InquiryStatus status,
            InquiryType type,
            InquiryUrgentLevel urgentLevel,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sort,
            String direction
    );

}