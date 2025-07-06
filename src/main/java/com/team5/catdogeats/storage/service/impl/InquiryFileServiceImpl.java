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
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryAttachmentDTO;
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

                String fileName = generateUniqueFileName(imageFile.getOriginalFilename());
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
                        .imageId(savedImage.getId())
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

                String fileName = generateUniqueFileName(documentFile.getOriginalFilename());
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
                        .imageId(savedFile.getId())
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

    @Override
    @JpaTransactional(readOnly = true)
    public Resource downloadUserFile(String inquiryId, String fileId, String providerId) {
        if (!validateFileAccess(inquiryId, fileId, providerId)) {
            throw new AccessDeniedException("해당 파일에 대한 접근 권한이 없습니다.");
        }

        return downloadFileResource(fileId);
    }

    @Override
    @JpaTransactional(readOnly = true)
    public Resource downloadAdminFile(String inquiryId, String fileId) {
        InquiryFiles inquiryFile = inquiryFileRepository.findByInquiryIdAndFileId(inquiryId, fileId)
                .orElseThrow(() -> new EntityNotFoundException("파일을 찾을 수 없습니다."));

        return downloadFileResource(fileId);
    }

    private Resource downloadFileResource(String fileId) {
        // 이미지 파일인지 확인
        Images image = imageRepository.findById(fileId).orElse(null);
        if (image != null) {
            return createResourceFromUrl(image.getImageUrl());
        }

        // 문서 파일인지 확인
        Files file = fileRepository.findById(fileId).orElse(null);
        if (file != null) {
            return createResourceFromUrl(file.getFileUrl());
        }

        throw new EntityNotFoundException("파일을 찾을 수 없습니다: " + fileId);
    }

    private Resource createResourceFromUrl(String fileUrl) {
        try {
            Resource resource = new UrlResource(fileUrl);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("잘못된 파일 URL입니다: " + fileUrl, e);
        }
    }


    @Override
    public String generateSafeDownloadFileName(String originalFileName, String fileId) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "file_" + fileId;
        }

        String safeName = originalFileName
                .replaceAll("[<>:\"/\\\\|?*]", "")
                .replaceAll("\\s+", "_")
                .trim();

        if (safeName.length() > 100) {
            String extension = getFileExtension(originalFileName);
            int maxNameLength = 100 - extension.length() - 1;
            safeName = safeName.substring(0, Math.max(1, maxNameLength)) + "." + extension;
        }

        return safeName.isEmpty() ? "file_" + fileId : safeName;
    }

    @Override
    @JpaTransactional(readOnly = true)
    public boolean validateFileAccess(String inquiryId, String fileId, String providerId) {
        try {
            InquiryFiles inquiryFile = inquiryFileRepository.findByInquiryIdAndFileId(inquiryId, fileId)
                    .orElse(null);
            if (inquiryFile == null) {
                return false;
            }

            Inquires inquiry = inquiryFile.getInquires();
            Inquires rootInquiry = findRootInquiry(inquiry);

            String userId = getUserIdByProviderId(providerId);
            return rootInquiry.getUsers().getId().equals(userId);

        } catch (Exception e) {
            log.error("파일 접근 권한 검증 실패 - inquiryId: {}, fileId: {}, providerId: {}",
                    inquiryId, fileId, providerId, e);
            return false;
        }
    }

    private InquiryAttachmentDTO convertToAttachmentDTO(InquiryFiles inquiryFile) {
        String fileId = null;
        String originalFileName = "첨부파일";

        if (inquiryFile.getImages() != null) {
            fileId = inquiryFile.getImages().getId();
            originalFileName = extractOriginalFileNameFromUrl(inquiryFile.getImages().getImageUrl());
        } else if (inquiryFile.getFiles() != null) {
            fileId = inquiryFile.getFiles().getId();
            originalFileName = extractOriginalFileNameFromUrl(inquiryFile.getFiles().getFileUrl());
        }

        return InquiryAttachmentDTO.builder()
                .imageId(fileId)
                .originalFileName(originalFileName)
                .uploadedAt(inquiryFile.getCreatedAt())
                .fileSize(0L) // 파일 크기는 별도 저장 필요
                .build();
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uniqueId = UUID.randomUUID().toString();
        return uniqueId + "." + extension;
    }

    private String getFileExtension(String fileName) {
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

    private String extractOriginalFileNameFromUrl(String fileUrl) {
        String fileName = extractFileNameFromUrl(fileUrl);

        if (fileName.contains(".")) {
            String extension = getFileExtension(fileName);
            return "attachment." + extension;
        }
        return fileName;
    }

    private Inquires findRootInquiry(Inquires inquiry) {
        Inquires current = inquiry;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    private String getUserIdByProviderId(String providerId) {
        Users user = userRepository.findByProviderAndProviderId("google", providerId)
                .or(() -> userRepository.findByProviderAndProviderId("kakao", providerId))
                .or(() -> userRepository.findByProviderAndProviderId("naver", providerId))
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + providerId));
        return user.getId();
    }
}