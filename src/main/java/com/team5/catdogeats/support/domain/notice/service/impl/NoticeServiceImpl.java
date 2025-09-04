package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.annotation.Notification;
import com.team5.catdogeats.notifications.domain.dto.NoticeCompletedDTO;
import com.team5.catdogeats.notifications.domain.enums.NotificationType;
import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.service.NoticeFileService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeFilesRepository noticeFilesRepository;
    private final NoticeFileService noticeFileService;

    private Sort createSort(String sortBy) {
        return switch (sortBy) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    // ========== 공지사항 목록 조회 ==========
    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public NoticeListResponseDTO getNotices(int page, int size, String search, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Notices> noticePage;
        if (search != null && !search.trim().isEmpty()) {
            noticePage = noticeRepository.findByTitleOrContentContaining(search.trim(), pageable);
        } else {
            noticePage = noticeRepository.findAll(pageable);
        }

        Page<NoticeResponseDTO> responsePage = noticePage.map(NoticeResponseDTO::from);
        return NoticeListResponseDTO.from(responsePage);
    }

    @PersistenceContext
    private EntityManager em;

    // ========== 공지사항 상세 조회 (일반 사용자용 - 조회수 증가) ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO getNotice(String noticeId) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 원자적 조회수 증가 (동시성 안전)
        noticeRepository.incrementViewCount(noticeId);

        em.flush();      // DB 반영 (JPQL bulk-update 반영)
        em.refresh(notice);  // 엔티티 새로고침 (DB에서 다시 select 해서 엔티티 동기화)

        List<NoticeFiles> attachments = noticeFilesRepository.findByNoticesId(noticeId);

        log.info("일반 사용자 공지사항 상세 조회 완료 (조회수 증가) - ID: {}, 조회수: {}",
                noticeId, notice.getViewCount());

        return NoticeResponseDTO.fromWithAttachments(notice, attachments);
    }

    // ========== 공지사항 상세 조회 (관리자용 - 조회수 증가 없음) ==========
    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public NoticeResponseDTO getNoticeForAdmin(String noticeId) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 조회수 증가 없음
        List<NoticeFiles> attachments = noticeFilesRepository.findByNoticesId(noticeId);

        log.info("관리자 공지사항 상세 조회 완료 (조회수 증가 없음) - ID: {}, 현재 조회수: {}",
                noticeId, notice.getViewCount());

        return NoticeResponseDTO.fromWithAttachments(notice, attachments);
    }

    // ========== 공지사항 생성 ==========
    @Override
    @Notification(type = NotificationType.NOTICE)
    public NoticeCompletedDTO createNotice(NoticeCreateRequestDTO requestDTO) {
        Notices notice = Notices.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .build();

        Notices savedNotice = noticeRepository.save(notice);
        log.info("공지사항 생성 완료 - ID: {}, 제목: {}", savedNotice.getId(), savedNotice.getTitle());
        String content = savedNotice.getContent();
        int endIndex = Math.min(content.length(), 10);

        return new NoticeCompletedDTO(
                savedNotice.getTitle(),
                content.substring(0, endIndex),
                NotificationType.NOTICE
        );
    }

    // ========== 공지사항 수정 ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO updateNotice(String noticeId, NoticeUpdateRequestDTO requestDTO) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        notice.setTitle(requestDTO.getTitle());
        notice.setContent(requestDTO.getContent());

        Notices updatedNotice = noticeRepository.save(notice);
        log.info("공지사항 수정 완료 - ID: {}, 제목: {}", updatedNotice.getId(), updatedNotice.getTitle());

        return NoticeResponseDTO.from(updatedNotice);
    }

    // ========== 공지사항 삭제 ==========
    @Override
    @JpaTransactional
    public void deleteNotice(String noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId);
        }

        List<NoticeFiles> noticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
        log.info("=== 파일 삭제 디버깅 - 조회된 파일 개수: {} ===", noticeFiles.size());

        if (noticeFiles.isEmpty()) {
            log.info("연결된 파일이 없어서 S3 삭제 건너뜀");
        } else {
            for (NoticeFiles noticeFile : noticeFiles) {
                String fileUrl = noticeFile.getFiles().getFileUrl();
                String fileId = noticeFile.getFiles().getId();
                log.info("S3 파일 삭제 시도 - URL: {}", fileUrl);

                try {
                    noticeFileService.deleteNoticeFileCompletely(fileId);
                } catch (Exception e) {
                    log.warn("파일 삭제 실패 (계속 진행) - ID: {}, 오류: {}", fileId, e.getMessage());
                }
            }
        }

        noticeFilesRepository.deleteByNoticesId(noticeId);
        noticeRepository.deleteById(noticeId);
        log.info("공지사항 삭제 완료 - ID: {}", noticeId);
    }

    // ========== 파일 업로드 ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO uploadFile(String noticeId, MultipartFile file) {
        // 파일 검증 (Notice 도메인 책임)
        validateFile(file);

        // 공지사항 존재 확인
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 파일 관리 서비스에 위임
        Files savedFile = noticeFileService.uploadNoticeFile(file);

        // 공지사항과 파일 연결 (Notice 도메인 책임)
        NoticeFiles noticeFile = NoticeFiles.builder()
                .notices(notice)
                .files(savedFile)
                .build();
        noticeFilesRepository.save(noticeFile);

        // 첨부파일 포함된 상세 정보 반환
        List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
        return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);
    }

    // ========== 파일 다운로드 ==========
    @Override
    public NoticeFileDownloadResponseDTO downloadFile(String fileId) {
        return noticeFileService.downloadNoticeFile(fileId);
    }

    // ========== 파일 삭제 ==========
    @Override
    @JpaTransactional
    public void deleteFile(String noticeId, String fileId) {

        NoticeFiles noticeFile = noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항에 연결되지 않은 파일입니다. noticeId: " + noticeId + ", fileId: " + fileId));

        // 매핑 관계 삭제
        noticeFilesRepository.deleteById(noticeFile.getId());

        // 파일 관리 서비스에 위임 (Storage + Files DB 삭제)
        noticeFileService.deleteNoticeFileCompletely(fileId);

        log.info("파일 삭제 완료 - noticeId: {}, fileId: {}", noticeId, fileId);
    }

    // ========== 파일 수정(교체) ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO replaceFile(String noticeId, String fileId, MultipartFile newFile) {
        // 파일 검증 (Notice 도메인 책임)
        validateFile(newFile);

        // 공지사항 존재 확인
        NoticeFiles noticeFile = noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항에 연결되지 않은 파일입니다. noticeId: " + noticeId + ", fileId: " + fileId));

        // 공지사항 정보가 필요하면
        Notices notice = noticeFile.getNotices();

        // 파일 관리 서비스에 위임
        noticeFileService.replaceNoticeFile(fileId, newFile);

        log.info("파일 교체 완료 - noticeId: {}, fileId: {}", noticeId, fileId);

        // 업데이트된 공지사항 정보 반환
        List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
        return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);
    }


    // ========== 새로 추가된 검증 및 유틸리티 메서드들 ==========
    //파일 검증 메서드
    private void validateFile(MultipartFile file) {
        // 파일 크기 제한 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 허용된 파일 확장자 검사
        String fileName = file.getOriginalFilename();
        if (!isAllowedFileType(fileName)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (pdf, doc, docx, xls, xlsx 만 가능)");
        }
    }

    //파일 타입 검증
    private boolean isAllowedFileType(String fileName) {
        if (fileName == null) return false;

        // extractFileExtension 메서드 호출 대신 직접 처리
        if (fileName.isEmpty()) {
            return false;
        }

        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        return List.of("pdf", "doc", "docx", "xls", "xlsx").contains(extension);
    }

    // ========== 공지사항 생성 (파일 포함) ==========
    @Override
    @JpaTransactional
    @Notification(type = NotificationType.NOTICE)
    public NoticeResponseDTO createNoticeWithFiles(NoticeCreateRequestDTO requestDTO, List<MultipartFile> files) {
        // 1. 공지사항 생성
        Notices notice = Notices.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .build();

        Notices savedNotice = noticeRepository.save(notice);
        log.info("공지사항 생성 완료 - ID: {}, 제목: {}", savedNotice.getId(), savedNotice.getTitle());

        // 2. 파일들 업로드 및 연결
        List<NoticeFiles> attachedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 파일 검증
                validateFile(file);

                // 파일 업로드
                Files savedFile = noticeFileService.uploadNoticeFile(file);

                // 공지사항과 파일 연결
                NoticeFiles noticeFile = NoticeFiles.builder()
                        .notices(savedNotice)
                        .files(savedFile)
                        .build();

                NoticeFiles savedNoticeFile = noticeFilesRepository.save(noticeFile);
                attachedFiles.add(savedNoticeFile);

                log.info("파일 업로드 및 연결 완료 - 공지사항 ID: {}, 파일명: {}",
                        savedNotice.getId(), file.getOriginalFilename());

            } catch (Exception e) {
                // 파일 업로드 실패 시 로그만 남기고 계속 진행
                // 또는 전체 롤백이 필요하면 예외를 다시 던짐
                log.error("파일 업로드 실패 - 파일명: {}, 오류: {}",
                        file.getOriginalFilename(), e.getMessage());
                // throw e; // 전체 롤백이 필요한 경우
            }
        }

        // 3. 첨부파일 포함된 응답 반환
        return NoticeResponseDTO.fromWithAttachments(savedNotice, attachedFiles);
    }
}