package com.team5.catdogeats.support.domain.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


// 1:1 문의 첨부 이미지 정보 DTO
// 1:1 문의 상세 조회 시, 첨부된 이미지 파일들의 정보를 담는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryAttachmentDTO {

    private String imageId;
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
            // 허용된 이미지 확장자만 반환
            if (ext.matches("^(jpg|jpeg|png|webp)$")) {
                return ext;
            }
        }

        return "jpg"; // 기본값
    }

    /**
     // 다운로드 URL 생성
     * @param baseUrl 기본 URL (예: "/v1/users/inquiries/123/images" 또는 "/v1/admin/inquiries/123/images")
     * @return 완성된 다운로드 URL
     */
    public String getDownloadUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }

        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return cleanBaseUrl + "/" + this.imageId;
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


    //  안전한 파일명 반환 (다운로드 시 사용)
    //  XSS 방지 및 특수문자 제거
    public String getSafeFileName() {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "image_" + imageId + ".jpg";
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

        return safeName;
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
        private String imageId;
        private String originalFileName;
        private String uploadedAt;
        private Long fileSize;

        public InquiryAttachmentDTOBuilder imageId(String imageId) {
            this.imageId = imageId;
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
            return new InquiryAttachmentDTO(imageId, originalFileName, uploadedAt, fileSize);
        }
    }
}