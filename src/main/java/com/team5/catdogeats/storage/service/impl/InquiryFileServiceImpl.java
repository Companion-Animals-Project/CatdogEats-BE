package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.Images;
import com.team5.catdogeats.storage.domain.mapping.InquiryFiles;
import com.team5.catdogeats.storage.repository.FileRepository;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.InquiryFileRepository;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.storage.util.DocumentValidationUtil;
import com.team5.catdogeats.storage.util.ImageValidationUtil;
import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryAttachmentDTO;
import com.team5.catdogeats.support.domain.inquiry.repository.InquiryRepository;
import com.team5.catdogeats.storage.service.InquiryFileService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryFileServiceImpl implements InquiryFileService {

    private final InquiryRepository inquiryRepository;
    private final InquiryFileRepository inquiryFileRepository;
    private final ImageRepository imageRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final ImageValidationUtil imageValidationUtil;
    private final DocumentValidationUtil documentValidationUtil;

    @Override
    @JpaTransactional
    public List<InquiryAttachmentDTO> uploadUserImages(String inquiryId, MultipartFile[] imageFiles) {
        if (imageFiles == null || imageFiles.length == 0) {
            return new ArrayList<>();
        }

        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        List<InquiryAttachmentDTO> uploadedFiles = new ArrayList<>();

        for (MultipartFile imageFile : imageFiles) {
            if (imageFile.isEmpty()) {
                continue;
            }

            try {
                imageValidationUtil.validateImageFile(imageFile);

                String fileName = generateInquiryFileName(imageFile.getOriginalFilename());
                String imageUrl = objectStorageService.uploadImage(
                        fileName,
                        imageFile.getInputStream(),
                        imageFile.getSize(),
                        imageFile.getContentType()
                );

                Images image = Images.builder()
                        .imageUrl(imageUrl)
                        .build();
                Images savedImage = imageRepository.save(image);

                InquiryFiles inquiryFile = InquiryFiles.builder()
                        .inquires(inquiry)
                        .images(savedImage)
                        .build();
                inquiryFileRepository.save(inquiryFile);

                InquiryAttachmentDTO attachment = InquiryAttachmentDTO.builder()
                        .fileId(savedImage.getId())
                        .originalFileName(imageFile.getOriginalFilename())
                        .uploadedAt(inquiryFile.getCreatedAt())
                        .fileSize(imageFile.getSize())
                        .build();

                uploadedFiles.add(attachment);

                log.info("이미지 업로드 완료 - inquiryId: {}, imageId: {}, fileName: {}",
                        inquiryId, savedImage.getId(), imageFile.getOriginalFilename());

            } catch (IOException e) {
                log.error("이미지 업로드 실패 - fileName: {}", imageFile.getOriginalFilename(), e);
                throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + imageFile.getOriginalFilename(), e);
            }
        }

        return uploadedFiles;
    }

    @Override
    @JpaTransactional
    public List<InquiryAttachmentDTO> uploadAdminFiles(String inquiryId,
                                                       MultipartFile[] imageFiles,
                                                       MultipartFile[] documentFiles) {
        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        List<InquiryAttachmentDTO> uploadedFiles = new ArrayList<>();

        // 이미지 파일 업로드
        if (imageFiles != null && imageFiles.length > 0) {
            uploadedFiles.addAll(uploadUserImages(inquiryId, imageFiles));
        }

        // 문서 파일 업로드
        if (documentFiles != null && documentFiles.length > 0) {
            uploadedFiles.addAll(uploadDocumentFiles(inquiry, documentFiles));
        }

        return uploadedFiles;
    }

    private List<InquiryAttachmentDTO> uploadDocumentFiles(Inquires inquiry, MultipartFile[] documentFiles) {
        List<InquiryAttachmentDTO> uploadedFiles = new ArrayList<>();

        for (MultipartFile documentFile : documentFiles) {
            if (documentFile.isEmpty()) {
                continue;
            }

            try {
                documentValidationUtil.validateDocumentFile(documentFile);

                String fileName = generateInquiryFileName(documentFile.getOriginalFilename());
                String fileUrl = objectStorageService.uploadFile(
                        fileName,
                        documentFile.getInputStream(),
                        documentFile.getSize(),
                        documentFile.getContentType()
                );

                Files file = Files.builder()
                        .fileUrl(fileUrl)
                        .build();
                Files savedFile = fileRepository.save(file);

                InquiryFiles inquiryFile = InquiryFiles.builder()
                        .inquires(inquiry)
                        .files(savedFile)
                        .build();
                inquiryFileRepository.save(inquiryFile);

                InquiryAttachmentDTO attachment = InquiryAttachmentDTO.builder()
                        .fileId(savedFile.getId())
                        .originalFileName(documentFile.getOriginalFilename())
                        .uploadedAt(inquiryFile.getCreatedAt())
                        .fileSize(documentFile.getSize())
                        .build();

                uploadedFiles.add(attachment);

                log.info("문서 파일 업로드 완료 - inquiryId: {}, fileId: {}, fileName: {}",
                        inquiry.getId(), savedFile.getId(), documentFile.getOriginalFilename());

            } catch (IOException e) {
                log.error("문서 파일 업로드 실패 - fileName: {}", documentFile.getOriginalFilename(), e);
                throw new RuntimeException("문서 파일 업로드 중 오류가 발생했습니다: " + documentFile.getOriginalFilename(), e);
            }
        }

        return uploadedFiles;
    }

    @Override
    @JpaTransactional(readOnly = true)
    public List<InquiryAttachmentDTO> getInquiryThreadAttachments(String rootInquiryId) {
        List<InquiryFiles> inquiryFiles = inquiryFileRepository.findByInquiryThreadOrderByCreatedAt(rootInquiryId);

        return inquiryFiles.stream()
                .map(this::convertToAttachmentDTO)
                .collect(Collectors.toList());
    }


    // 최적화된 루트 문의 찾기 (이미 fetch된 데이터만 사용)
    private Inquires findRootInquiryOptimized(Inquires inquiry) {
        Inquires current = inquiry;
        // 부모가 이미 fetch되어 있다면 추가 쿼리 없이 순회
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    // providerId로 직접 비교 (DB 조회 없음)
    private boolean checkUserAccessByProviderId(Users user, String providerId) {
        if (user == null || providerId == null) {
            return false;
        }

        // Users 엔티티에 providerId 필드가 있다고 가정
        return providerId.equals(user.getProviderId());
    }


    // 권한 검증 로직을 별도 메서드로 분리
    private boolean hasFileAccess(InquiryFiles inquiryFile, String providerId) {
        try {
            // 이미지 파일인 경우 - 접근 허용
            if (inquiryFile.getImages() != null) {
                return true;
            }

            // 문서 파일인 경우 - 권한 검증
            if (inquiryFile.getFiles() != null) {
                Inquires inquiry = inquiryFile.getInquires();
                Inquires rootInquiry = findRootInquiryOptimized(inquiry);
                return checkUserAccessByProviderId(rootInquiry.getUsers(), providerId);
            }

            return false;
        } catch (Exception e) {
            log.error("파일 접근 권한 검증 실패 - fileId: {}, providerId: {}",
                    inquiryFile.getId(), providerId, e);
            return false;
        }
    }

    // 파일 URL 추출 로직 분리
    private String extractFileUrl(InquiryFiles inquiryFile) {
        if (inquiryFile.getImages() != null) {
            return inquiryFile.getImages().getImageUrl();
        } else if (inquiryFile.getFiles() != null) {
            return inquiryFile.getFiles().getFileUrl();
        }
        return null;
    }


    private Resource createResourceFromUrl(String fileUrl) {
        try {

            // URL에 프로토콜이 없으면 https:// 추가
            String fullUrl = fileUrl;
            if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                fullUrl = "https://" + fileUrl;
            }

            // URL의 파일명 부분만 인코딩 (한글 지원)
            String encodedUrl = encodeFileNameInUrl(fullUrl);

            Resource resource = new UrlResource(encodedUrl);

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.error("파일을 읽을 수 없습니다 - URL: {}", encodedUrl);
                throw new RuntimeException("파일을 읽을 수 없습니다: " + encodedUrl);
            }
        } catch (MalformedURLException e) {
            log.error("잘못된 파일 URL: {}", fileUrl, e);
            throw new RuntimeException("잘못된 파일 URL입니다: " + fileUrl, e);
        } catch (Exception e) {
            log.error("파일 리소스 생성 실패 - URL: {}", fileUrl, e);
            throw new RuntimeException("파일 리소스 생성 실패: " + fileUrl, e);
        }
    }

    /**
     * URL에서 파일명 부분만 인코딩
     * 예: https://domain.com/path/한글파일.jpg -> https://domain.com/path/%ED%95%9C%EA%B8%80%ED%8C%8C%EC%9D%BC.jpg
     */
    private String encodeFileNameInUrl(String url) {
        try {
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex == -1) {
                return url;
            }

            String baseUrl = url.substring(0, lastSlashIndex + 1);
            String fileName = url.substring(lastSlashIndex + 1);

            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return baseUrl + encodedFileName;
        } catch (Exception e) {
            log.warn("URL 인코딩 실패, 원본 반환: {}", url, e);
            return url;
        }
    }


    @Override
    public String getFileExtension(String fileId) {
        try {
            InquiryFiles inquiryFile = inquiryFileRepository.findByFileId(fileId).orElse(null);
            if (inquiryFile == null) {
                return "jpg";
            }

            String fileUrl = extractFileUrl(inquiryFile);
            return extractExtensionFromUrl(fileUrl);

        } catch (Exception e) {
            log.error("파일 확장자 추출 실패 - fileId: {}", fileId, e);
            return "jpg";
        }
    }


    // ========== Private 헬퍼 메서드들 ==========

    /**
     * 문의 전용 파일명 생성 (공지사항 패턴 참고)
     * 형식: inquiry_UUID_타임스탬프_원본파일명
     */
    private String generateInquiryFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = "file.jpg"; // 기본값
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "inquiry_" + uuid + "_" + timestamp + "_" + originalFileName;
    }

    /**
     * URL에서 실제 파일명 추출 (NoticeAttachmentDTO의 extractFileName 로직 참고)
     * inquiry_uuid_YYYYMMDD_HHMMSS_원본파일명 → 원본파일명
     */
    private String extractRealFileNameFromUrl(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                return "첨부파일.jpg";
            }

            // URL에서 파일명 부분만 추출
            String fileName = extractFileNameFromUrl(fileUrl);

            // 문의 파일 패턴 처리: inquiry_uuid_YYYYMMDD_HHMMSS_원본파일명
            if (fileName.startsWith("inquiry_")) {
                // "inquiry_"를 제거
                String withoutPrefix = fileName.substring(8); // "inquiry_" 제거

                // UUID 부분 제거 (첫 번째 _까지)
                int firstUnderscore = withoutPrefix.indexOf('_');
                if (firstUnderscore != -1) {
                    String afterUuid = withoutPrefix.substring(firstUnderscore + 1);

                    // 날짜 부분 제거 (두 번째 _까지) - YYYYMMDD
                    int secondUnderscore = afterUuid.indexOf('_');
                    if (secondUnderscore != -1) {
                        String afterDate = afterUuid.substring(secondUnderscore + 1);

                        // 시간 부분 제거 (세 번째 _까지) - HHMMSS
                        int thirdUnderscore = afterDate.indexOf('_');
                        if (thirdUnderscore != -1) {
                            return afterDate.substring(thirdUnderscore + 1); // 원본 파일명
                        }
                    }
                }
            }

            // 기존 UUID 패턴 (이전 파일들 대응): uuid.확장자
            if (fileName.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\..+")) {
                String extension = extractExtensionFromUrl(fileName);
                return "첨부파일." + extension;
            }

            // 패턴에 맞지 않으면 그대로 반환
            return fileName;

        } catch (Exception e) {
            log.warn("파일명 추출 실패: {}", e.getMessage());
            return "첨부파일.jpg";
        }
    }

    private String extractExtensionFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "jpg";
        }

        // URL에서 파일명 추출 후 확장자 가져오기
        String fileName = extractFileNameFromUrl(url);

        // 파일명에서 직접 확장자 추출
        if (fileName == null || fileName.trim().isEmpty()) {
            return "jpg";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "jpg";
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return "";
        }

        int lastSlashIndex = fileUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        return fileUrl;
    }

    private InquiryAttachmentDTO convertToAttachmentDTO(InquiryFiles inquiryFile) {
        String fileId = null;
        String fileUrl = null;
        ZonedDateTime createdAt = null;

        // 파일 정보 추출
        if (inquiryFile.getImages() != null) {
            fileId = inquiryFile.getImages().getId();
            fileUrl = inquiryFile.getImages().getImageUrl();
            createdAt = inquiryFile.getImages().getCreatedAt();
        } else if (inquiryFile.getFiles() != null) {
            fileId = inquiryFile.getFiles().getId();
            fileUrl = inquiryFile.getFiles().getFileUrl();
            createdAt = inquiryFile.getFiles().getCreatedAt();
        }

        // 실제 파일명 추출
        String realFileName = extractRealFileNameFromUrl(fileUrl);

        return InquiryAttachmentDTO.builder()
                .fileId(fileId)
                .originalFileName(realFileName)
                .fileSize(0L)
                .uploadedAt(createdAt != null ? createdAt : inquiryFile.getCreatedAt()) // 둘 중 하나 사용
                .build();
    }

    /**
     * 🆕 새로운 메서드 1: 사용자용 파일 다운로드 (권한 검증 + 다운로드 통합)
     * 기존 validateFileAccess + downloadUserFile의 중복 쿼리 문제 해결
     */
    @Override
    @JpaTransactional(readOnly = true)
    public Resource downloadUserFileWithValidation(String inquiryId, String fileId, String providerId) {

        // 한 번의 쿼리로 파일 정보와 권한 정보를 모두 조회
        InquiryFiles inquiryFile = inquiryFileRepository.findByFileIdWithUserAndInquiry(fileId)
                .orElseThrow(() -> {
                    log.warn("파일을 찾을 수 없음 - fileId: {}", fileId);
                    return new EntityNotFoundException("파일을 찾을 수 없습니다: " + fileId);
                });

        // 권한 검증 (이미 조회된 데이터 사용)
        if (!hasFileAccess(inquiryFile, providerId)) {
            log.warn("파일 접근 권한 없음 - fileId: {}, providerId: {}", fileId, providerId);
            throw new AccessDeniedException("해당 파일에 대한 접근 권한이 없습니다.");
        }

        // 파일 URL 추출 (이미 조회된 데이터 사용)
        String fileUrl = extractFileUrl(inquiryFile);
        if (fileUrl == null) {
            log.error("파일 URL을 찾을 수 없음 - fileId: {}", fileId);
            throw new EntityNotFoundException("파일 URL을 찾을 수 없습니다: " + fileId);
        }

        log.info("사용자 파일 다운로드 승인 - fileId: {}, providerId: {}", fileId, providerId);
        return createResourceFromUrl(fileUrl);
    }

    /**
     * 🆕 새로운 메서드 2: 관리자용 파일 다운로드 (명확한 네이밍)
     */
    @Override
    @JpaTransactional(readOnly = true)
    public Resource downloadAdminFileWithoutValidation(String inquiryId, String fileId) {

        // 한 번의 조회로 처리
        InquiryFiles inquiryFile = inquiryFileRepository.findByFileId(fileId)
                .orElseThrow(() -> {
                    log.warn("관리자 파일 다운로드 - 파일을 찾을 수 없음: {}", fileId);
                    return new EntityNotFoundException("파일을 찾을 수 없습니다: " + fileId);
                });

        // 이미 조회된 데이터로 리소스 생성
        String fileUrl = extractFileUrl(inquiryFile);
        if (fileUrl == null) {
            log.error("관리자 파일 다운로드 - 파일 URL을 찾을 수 없음: {}", fileId);
            throw new EntityNotFoundException("파일 URL을 찾을 수 없습니다: " + fileId);
        }

        log.info("관리자 파일 다운로드 승인 - fileId: {}", fileId);
        return createResourceFromUrl(fileUrl);
    }

    @Override
    public String generateAdminDownloadFileName(String fileId) {
        try {
            String extension = getFileExtension(fileId);
            return "inquiry_file_" + System.currentTimeMillis() + "." + extension;
        } catch (Exception e) {
            log.warn("파일명 생성 실패 - fileId: {}", fileId, e);
            return "inquiry_file_" + System.currentTimeMillis() + ".jpg";
        }
    }
}