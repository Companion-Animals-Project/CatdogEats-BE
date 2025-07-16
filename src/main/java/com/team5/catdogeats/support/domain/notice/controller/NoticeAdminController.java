package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.*;

@RestController
@RequestMapping("/v1/admin/notices/test")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Notice (Admin)", description = "공지사항 관리자 API - 관리자만 접근 가능")
public class NoticeAdminController {

    private final NoticeService noticeService;
    private final AdminControllerUtils controllerUtils;

    // ========== 공지사항 목록 조회 ==========
    @GetMapping
    @Operation(
            summary = "공지사항 목록 조회",
            description = "관리자 페이지에서 공지사항 목록을 조회합니다. " +
                    "sortBy: latest(최신순, 기본값), oldest(오래된순), views(조회순)"
    )
    public ResponseEntity<APIResponse<NoticeListResponseDTO>> getNotices(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "latest") String sortBy) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 목록 조회 - adminId: {}, adminName: {}",
                    adminInfo.adminId(), adminInfo.name());

            NoticeListResponseDTO response = noticeService.getNotices(page, size, search, sortBy);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - 공지사항 목록 조회 시도, error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (Exception e) {
            log.error("관리자 공지사항 목록 조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // ========== 공지사항 상세 조회 (관리자용 - 조회수 증가 없음) ==========
    @GetMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 상세 조회 (관리자용)",
            description = "관리자 페이지에서 공지사항 상세 내용을 조회합니다. 조회수가 증가하지 않습니다."
    )
    public ResponseEntity<APIResponse<NoticeResponseDTO>> getNotice(
            HttpSession session,
            @PathVariable String noticeId) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            // 🔥 변경: 관리자용 메서드 사용 (조회수 증가 없음)
            NoticeResponseDTO response = noticeService.getNoticeForAdmin(noticeId);

            log.info("관리자 공지사항 상세 조회 완료 (조회수 증가 없음) - noticeId: {}, adminId: {}, adminName: {}",
                    noticeId, adminInfo.adminId(), adminInfo.name());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - noticeId: {}, error: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {
            log.error("Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 공지사항 상세 조회 중 서버 오류 - noticeId: {}", noticeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 공지사항 생성 ==========
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "공지사항 등록",
            description = "관리자가 공지사항을 등록합니다. 파일 첨부 가능"
    )
    public ResponseEntity<APIResponse<NoticeResponseDTO>> createNotice(
            HttpSession session,
            @Valid @RequestPart("notice") NoticeCreateRequestDTO requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 생성 요청 - 제목: {}, 첨부파일 개수: {}, adminId: {}, adminName: {}",
                    requestDto.getTitle(),
                    files != null ? files.size() : 0,
                    adminInfo.adminId(),
                    adminInfo.name());

            // 파일 첨부 여부에 따라 다른 서비스 메서드 호출
            NoticeResponseDTO response;
            if (files != null && !files.isEmpty()) {
                response = noticeService.createNoticeWithFiles(requestDto, files);
            } else {
                response = noticeService.createNotice(requestDto);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(APIResponse.success(ResponseCode.CREATED, response));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - 공지사항 생성 시도, error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (IllegalArgumentException e) {
            log.error("파일 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 공지사항 생성 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // ========== 공지사항 수정 ==========
    @PatchMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 수정",
            description = "관리자가 공지사항을 수정합니다."
    )
    public ResponseEntity<APIResponse<NoticeResponseDTO>> updateNotice(
            HttpSession session,
            @PathVariable String noticeId,
            @Valid @RequestBody NoticeUpdateRequestDTO requestDto) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 수정 요청 - ID: {}, 제목: {}, adminId: {}, adminName: {}",
                    noticeId, requestDto.getTitle(), adminInfo.adminId(), adminInfo.name());

            NoticeResponseDTO response = noticeService.updateNotice(noticeId, requestDto);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - noticeId: {}, error: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (Exception e) {
            log.error("관리자 공지사항 수정 중 서버 오류 - noticeId: {}", noticeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // ========== 공지사항 삭제 ==========
    @DeleteMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 삭제",
            description = "관리자가 공지사항을 삭제합니다."
    )
    public ResponseEntity<APIResponse<Void>> deleteNotice(
            HttpSession session,
            @PathVariable String noticeId) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 삭제 요청 - ID: {}, adminId: {}, adminName: {}",
                    noticeId, adminInfo.adminId(), adminInfo.name());

            noticeService.deleteNotice(noticeId);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - noticeId: {}, error: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {
            log.error("Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 공지사항 삭제 중 서버 오류 - noticeId: {}", noticeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 파일 업로드 ==========
    @PostMapping(value = "/{noticeId}/files")
    @Operation(
            summary = "공지사항 첨부파일 업로드",
            description = "관리자가 공지사항의 첨부파일을 업로드합니다."
    )
    public ResponseEntity<APIResponse<NoticeResponseDTO>> uploadFile(
            HttpSession session,
            @PathVariable String noticeId,
            @RequestParam("file") MultipartFile file) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            // 기본적인 파일 존재 여부만 Controller에서 체크
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, "업로드할 파일을 선택해주세요."));
            }

            // 파일 검증과 비즈니스 로직은 서비스에서 처리
            NoticeResponseDTO response = noticeService.uploadFile(noticeId, file);

            log.info("관리자 파일 업로드 완료 - noticeId: {}, fileName: {}, adminId: {}, adminName: {}",
                    noticeId, file.getOriginalFilename(), adminInfo.adminId(), adminInfo.name());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - 파일 업로드 시도, noticeId: {}, error: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (IllegalArgumentException e) {
            log.error("관리자 파일 업로드 검증 실패 - ID: {}, 오류: {}", noticeId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 파일 업로드 실패 - ID: {}, 오류: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 파일 다운로드 ==========
    @GetMapping("/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 다운로드",
            description = "관리자 페이지에서 공지사항의 첨부파일을 다운로드합니다."
    )
    public ResponseEntity<Resource> downloadFile(
            HttpSession session,
            @PathVariable String noticeId,
            @PathVariable String fileId) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            // 서비스에서 DTO로 받아오기
            NoticeFileDownloadResponseDTO downloadResponse = noticeService.downloadFile(fileId);

            log.info("관리자 파일 다운로드 완료 - noticeId: {}, fileId: {}, fileName: {}, adminId: {}, adminName: {}",
                    noticeId, fileId, downloadResponse.getFilename(), adminInfo.adminId(), adminInfo.name());

            // DTO에서 필요한 정보 추출해서 ResponseEntity 구성
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(downloadResponse.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadResponse.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(downloadResponse.getResource());

        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - 파일 다운로드 시도, noticeId: {}, fileId: {}", noticeId, fileId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("관리자 파일 다운로드 실패 - 파일 ID: {}, 오류: {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ========== 파일 삭제 ==========
    @DeleteMapping("/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 삭제",
            description = "관리자가 공지사항의 첨부파일을 삭제합니다."
    )
    public ResponseEntity<APIResponse<Void>> deleteFile(
            HttpSession session,
            @PathVariable String noticeId,
            @PathVariable String fileId) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            noticeService.deleteFile(noticeId, fileId);

            log.info("관리자 파일 삭제 완료 - noticeId: {}, fileId: {}, adminId: {}, adminName: {}",
                    noticeId, fileId, adminInfo.adminId(), adminInfo.name());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - 파일 삭제 시도, noticeId: {}, fileId: {}, error: {}",
                    noticeId, fileId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {
            log.error("File or Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid file deletion request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 파일 삭제 중 서버 오류 - noticeId: {}, fileId: {}", noticeId, fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 파일 수정(교체) ==========
    @PutMapping(value = "/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 수정(교체)",
            description = "관리자가 공지사항의 첨부파일을 새 파일로 수정(교체)합니다."
    )
    public ResponseEntity<APIResponse<NoticeResponseDTO>> replaceFile(
            HttpSession session,
            @PathVariable String noticeId,
            @PathVariable String fileId,
            @RequestParam("file") MultipartFile newFile) {

        try {
            // 세션 인증 추가
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            // 기본적인 파일 존재 여부만 Controller에서 체크
            if (newFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, "수정(교체)할 파일을 선택해주세요."));
            }

            // 파일 검증과 비즈니스 로직은 서비스에서 처리
            NoticeResponseDTO response = noticeService.replaceFile(noticeId, fileId, newFile);

            log.info("관리자 파일 교체 완료 - noticeId: {}, fileId: {}, newFileName: {}, adminId: {}, adminName: {}",
                    noticeId, fileId, newFile.getOriginalFilename(), adminInfo.adminId(), adminInfo.name());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - 파일 교체 시도, noticeId: {}, fileId: {}, error: {}",
                    noticeId, fileId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {
            log.error("File or Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid file replacement request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 파일 교체 중 서버 오류 - noticeId: {}, fileId: {}", noticeId, fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }
}