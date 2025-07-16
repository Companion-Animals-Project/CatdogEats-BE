package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryAttachmentDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 1:1 문의 파일 관리 서비스 인터페이스
public interface InquiryFileService {

    // === 파일 업로드 기능 ===

    // 유저 문의 등록 시 이미지 파일 업로드
    List<InquiryAttachmentDTO> uploadUserImages(String inquiryId, MultipartFile[] imageFiles);

    // 관리자 답변 시 이미지 + 문서 파일 업로드
    List<InquiryAttachmentDTO> uploadAdminFiles(String inquiryId,
                                                MultipartFile[] imageFiles,
                                                MultipartFile[] documentFiles);

    // === 파일 조회 기능 (상세조회용) ===

    // 문의 스레드의 모든 첨부 파일 조회 (상세조회에서 사용)
    List<InquiryAttachmentDTO> getInquiryThreadAttachments(String rootInquiryId);

    // 🆕 추가: 특정 메시지의 첨부 파일만 조회
    List<InquiryAttachmentDTO> getMessageAttachments(String messageId);

    // === 파일 다운로드 기능 ===

    // 🔄 수정된 메서드: 사용자용 파일 다운로드 (provider + providerId 방식)
    Resource downloadUserFileWithValidation(String inquiryId, String fileId, String provider, String providerId);

    // 관리자용 파일 다운로드
    Resource downloadAdminFileWithoutValidation(String inquiryId, String fileId);

    // === 유틸리티 기능 ===

    // 파일의 실제 확장자 반환
    String getFileExtension(String fileId);

    // 관리자용 다운로드 파일명 생성
    String generateAdminDownloadFileName(String fileId);
}