package com.team5.catdogeats.storage.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 문서 파일 검증을 위한 공용 유틸리티 클래스
 * 관리자가 업로드하는 문서 파일(PDF, Excel, Word, HWP 등)에 대한 검증 로직
 */
@Slf4j
@Component
public class DocumentValidationUtil {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 통합 문서 파일 검증
     * - 기본 속성 검사
     * - 파일 크기 검증
     * - MIME Type 검증
     * - 실제 파일 내용 검증 (Magic Number)
     * - 보안 검증
     *
     * @param documentFile 검증할 문서 파일
     * @throws IllegalArgumentException 검증 실패 시
     */
    public void validateDocumentFile(MultipartFile documentFile) {
        // 기본 검사
        validateBasicProperties(documentFile);

        // 파일 크기 검증
        validateFileSize(documentFile);

        // MIME Type 검증
        validateMimeType(documentFile);

        // 파일명 보안 검증
        validateFileName(documentFile.getOriginalFilename());

        // 스크립트 공격 방지
        validateNoScriptContent(documentFile);

        // 실제 파일 내용 검증
        validateFileSignature(documentFile);

        log.debug("문서 파일 검증 완료 - fileName: {}, size: {}",
                documentFile.getOriginalFilename(), documentFile.getSize());
    }

    /**
     * 기본 속성 검증
     */
    private void validateBasicProperties(MultipartFile documentFile) {
        if (documentFile == null || documentFile.isEmpty()) {
            throw new IllegalArgumentException("문서 파일이 비어있습니다.");
        }
    }

    /**
     * 파일 크기 검증 (10MB 제한)
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("파일 크기가 너무 큽니다. (최대 %dMB)", MAX_FILE_SIZE / 1024 / 1024));
        }
    }

    /**
     * MIME Type 검증
     */
    private void validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("파일의 Content-Type을 확인할 수 없습니다.");
        }

        // 허용되는 MIME 타입들
        String[] allowedMimeTypes = {
                "application/pdf",                                           // PDF
                "application/msword",                                        // DOC
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
                "application/vnd.ms-excel",                                  // XLS
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // XLSX
                "application/x-hwp",                                         // HWP
                "application/haansofthwp"                                    // HWP (alternative)
        };

        boolean isAllowed = false;
        for (String allowedType : allowedMimeTypes) {
            if (contentType.equals(allowedType)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IllegalArgumentException(
                    String.format("허용되지 않은 문서 파일 형식입니다: %s", contentType));
        }
    }

    /**
     * 파일명 보안 검증
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 비어있습니다.");
        }

        if (fileName.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException("파일명이 너무 깁니다. (최대 255자)");
        }

        // 경로 순회 공격 방지
        if (fileName.contains("..") || fileName.contains("./") || fileName.contains(".\\")) {
            throw new IllegalArgumentException("파일명에 상대경로가 포함될 수 없습니다.");
        }

        // 허용되는 확장자 검증
        String extension = getFileExtension(fileName).toLowerCase();
        String[] allowedExtensions = {"pdf", "doc", "docx", "xls", "xlsx", "hwp"};

        boolean isValidExtension = false;
        for (String allowedExt : allowedExtensions) {
            if (extension.equals(allowedExt)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new IllegalArgumentException(
                    "허용되지 않은 파일 확장자입니다. (허용: pdf, doc, docx, xls, xlsx, hwp)");
        }

        // 위험한 확장자 차단
        if (fileName.toLowerCase().matches(".*\\.(js|html|htm|php|jsp|asp|exe|bat|cmd).*")) {
            throw new IllegalArgumentException("실행 가능한 파일 확장자는 업로드할 수 없습니다.");
        }
    }

    /**
     * 스크립트 공격 방지
     */
    private void validateNoScriptContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[2048];
            int bytesRead = is.read(buffer);

            if (bytesRead > 0) {
                String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).toLowerCase();

                String[] dangerousPatterns = {
                        "<script", "javascript:", "onload=", "onerror=",
                        "onclick=", "eval(", "document.", "alert("
                };

                for (String pattern : dangerousPatterns) {
                    if (content.contains(pattern)) {
                        throw new IllegalArgumentException(
                                "보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다.");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("파일 내용 검증 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 시그니처 검증 (Magic Number)
     * PDF, DOC, DOCX, XLS, XLSX, HWP 실제 파일 형식 확인
     */
    private void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[20]; // 충분한 바이트 수 확보
            int bytesRead = is.read(header);

            if (bytesRead < 4) {
                throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다.");
            }

            // PDF: %PDF (25 50 44 46)
            if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
                return;
            }

            // DOC: D0 CF 11 E0 (Microsoft Office 구 버전)
            // HWP: HWP Document File (D0 CF 11 E0 또는 특별한 시그니처)
            // HWP 3.0+: D0 CF 11 E0 (복합문서 형태)
            if (header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF &&
                    header[2] == 0x11 && header[3] == (byte) 0xE0) {
                return;
            }

            // DOCX, XLSX: PK (50 4B) - ZIP 기반 파일
            if (header[0] == 0x50 && header[1] == 0x4B) {
                return;
            }


            // HWP 5.0+: 특별한 시그니처 확인 (추가 검증 필요시)
            // 일반적으로 ZIP 형태이므로 PK로 시작할 수 있음

            throw new IllegalArgumentException("지원하지 않는 문서 파일 형식입니다.");

        } catch (IOException e) {
            throw new IllegalArgumentException("파일 형식 검증 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 안전한 파일 확장자 추출
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }

        String safeName = fileName.trim();
        int lastDotIndex = safeName.lastIndexOf('.');

        if (lastDotIndex != -1 && lastDotIndex < safeName.length() - 1) {
            return safeName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 안전한 파일명 생성 (다운로드 시 사용)
     */
    public String getSafeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "document_" + System.currentTimeMillis();
        }

        // 위험한 문자 제거 및 공백을 언더스코어로 변경
        String safeName = originalFileName
                .replaceAll("[<>:\"/\\\\|?*]", "") // 파일시스템에서 금지된 문자 제거
                .replaceAll("\\s+", "_") // 공백을 언더스코어로 변경
                .trim();

        // 파일명이 너무 길면 자르기 (확장자 포함 100자 제한)
        if (safeName.length() > 100) {
            String extension = getFileExtension(safeName);
            int maxNameLength = 100 - extension.length() - 1; // .확장자 길이 고려
            safeName = safeName.substring(0, Math.max(1, maxNameLength)) + "." + extension;
        }

        return safeName;
    }
}