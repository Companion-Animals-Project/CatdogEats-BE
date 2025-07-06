package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.support.domain.inquiry.dto.InquiryAttachmentDTO;
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


    // === 파일 다운로드 기능 ===


    // 유저용 파일 다운로드 (이미지 + 관리자가 첨부한 문서 모두 가능)
    Resource downloadUserFile(String inquiryId, String fileId, String providerId);


    // 관리자용 파일 다운로드
    Resource downloadAdminFile(String inquiryId, String fileId);



    // === 유틸리티 기능 ===


    // 파일 다운로드를 위한 안전한 파일명 생성
    String generateSafeDownloadFileName(String originalFileName, String fileId);


    // 파일 접근 권한 검증
    boolean validateFileAccess(String inquiryId, String fileId, String providerId);
}