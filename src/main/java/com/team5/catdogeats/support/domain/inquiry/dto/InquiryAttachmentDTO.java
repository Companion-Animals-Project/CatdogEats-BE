package com.team5.catdogeats.support.domain.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// 1:1 문의 첨부 파일 정보 DTO (이미지 + 문서 통합)
@Getter
@NoArgsConstructor
@AllArgsConstructor
// @Builder 제거 - 수동 빌더와 충돌 방지
@Slf4j
public class InquiryAttachmentDTO {

    private String fileId;
    private String originalFileName;
    private String uploadedAt;
    private Long fileSize; // 바이트 단위

    // 파일 확장자 추출
    public String getFileExtension() {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "jpg"; // 기본값
        }

        String safeName = originalFileName.trim();
        int lastDotIndex = safeName.lastIndexOf('.');

        if (lastDotIndex != -1 && lastDotIndex < safeName.length() - 1) {
            String ext = safeName.substring(lastDotIndex + 1).toLowerCase();
            // 허용된 파일 확장자만 반환 (이미지 + 문서)
            if (ext.matches("^(jpg|jpeg|png|webp|pdf|doc|docx|xls|xlsx|hwp)$")) {
                return ext;
            }
        }

        return "jpg"; // 기본값
    }

    /**
     * 다운로드 URL 생성
     * @param baseUrl 기본 URL (예: "/v1/users/inquiries/123/files" 또는 "/v1/admin/inquiries/123/files")
     * @return 완성된 다운로드 URL
     */
    public String getDownloadUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }

        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return cleanBaseUrl + "/" + this.fileId;
    }

    // 파일 크기를 사람이 읽기 쉬운 형태로 변환
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize <= 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = fileSize.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * 안전한 파일명 반환 (다운로드 시 사용)
     * XSS 방지 및 특수문자 제거
     */
    public String getSafeFileName() {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "attachment_" + fileId + "." + getFileExtension();
        }

        // 위험한 문자 제거 및 공백을 언더스코어로 변경
        String safeName = originalFileName
                .replaceAll("[<>:\"/\\\\|?*]", "") // 파일시스템에서 금지된 문자 제거
                .replaceAll("\\s+", "_") // 공백을 언더스코어로 변경
                .trim();

        // 파일명이 너무 길면 자르기 (확장자 포함 100자 제한)
        if (safeName.length() > 100) {
            String extension = getFileExtension();
            int maxNameLength = 100 - extension.length() - 1; // .확장자 길이 고려
            safeName = safeName.substring(0, Math.max(1, maxNameLength)) + "." + extension;
        }

        // 확장자가 없으면 추가
        if (!safeName.contains(".")) {
            safeName = safeName + "." + getFileExtension();
        }

        return safeName;
    }

    /**
     * 표시용 파일명 (UI에서 보여줄 때 사용)
     * 원본 파일명을 최대한 보존하면서 안전하게 표시
     */
    public String getDisplayFileName() {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "첨부파일." + getFileExtension();
        }

        return originalFileName;
    }

    // 이미지 타입 여부 확인
    public boolean isImageFile() {
        String extension = getFileExtension();
        return extension.matches("^(jpg|jpeg|png|webp)$");
    }

    // 업로드 시간을 한국 시간대로 포맷
    public String getFormattedUploadTime() {
        if (uploadedAt == null || uploadedAt.trim().isEmpty()) {
            return "";
        }
        return uploadedAt; // 이미 포맷된 문자열이므로 그대로 반환
    }

    // ZonedDateTime 으로부터 DTO 생성 시 사용하는 유틸리티 메서드
    public static String formatUploadTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        return zonedDateTime.withZoneSameInstant(koreaZone).format(formatter);
    }

    // 빌더 패턴으로 DTO 생성 시 업로드 시간 자동 포맷
    public static InquiryAttachmentDTOBuilder builder() {
        return new InquiryAttachmentDTOBuilder();
    }

    public static class InquiryAttachmentDTOBuilder {
        private String fileId;
        private String originalFileName;
        private String uploadedAt;
        private Long fileSize;

        public InquiryAttachmentDTOBuilder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }


        public InquiryAttachmentDTOBuilder originalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
            return this;
        }

        public InquiryAttachmentDTOBuilder uploadedAt(String uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public InquiryAttachmentDTOBuilder uploadedAt(ZonedDateTime zonedDateTime) {
            this.uploadedAt = formatUploadTime(zonedDateTime);
            return this;
        }

        public InquiryAttachmentDTOBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public InquiryAttachmentDTO build() {
            return new InquiryAttachmentDTO(fileId, originalFileName, uploadedAt, fileSize);
        }
    }
}