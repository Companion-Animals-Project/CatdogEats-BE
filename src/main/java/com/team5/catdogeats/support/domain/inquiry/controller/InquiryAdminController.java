package com.team5.catdogeats.support.domain.inquiry.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.storage.service.InquiryFileService;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryDetailResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryListResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/admin/inquiries")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')") // 스웨거 테스트용 주석 처리
@Tag(name = "Inquiry (Admin)", description = "1:1 문의 관리자 API - 관리자만 접근 가능")
public class InquiryAdminController {

    private final InquiryService inquiryService;
    private final InquiryFileService inquiryFileService;


    @GetMapping
    @Operation(
            summary = "모든 문의 목록 조회 (관리자)",
            description = "관리자가 모든 사용자의 문의 목록을 페이징으로 조회합니다. 최신순으로 정렬됩니다."
    )
    public ResponseEntity<ApiResponse<Page<InquiryListResponseDTO>>> getAllInquiries(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<InquiryListResponseDTO> inquiries = inquiryService.getAllInquiries(pageable);
            log.info("관리자 문의 목록 조회 완료 - page: {}, size: {}, totalElements: {}",
                    page, size, inquiries.getTotalElements());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, inquiries)
            );
        } catch (Exception e) {
            log.error("관리자 문의 목록 조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @GetMapping("/{inquiryId}")
    @Operation(
            summary = "문의 상세 조회 (관리자)",
            description = "관리자가 특정 문의의 상세 내용을 조회합니다."
    )
    public ResponseEntity<ApiResponse<InquiryDetailResponseDTO>> getInquiryDetailForAdmin(
            @Parameter(description = "문의 ID")
            @PathVariable String inquiryId) {

        try {
            InquiryDetailResponseDTO inquiry = inquiryService.getInquiryDetailForAdmin(inquiryId);
            log.info("관리자 문의 상세 조회 완료 - inquiryId: {}", inquiryId);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, inquiry)
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 문의 상세 조회 실패 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 문의 상세 조회 중 서버 오류 - inquiryId: {}", inquiryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PatchMapping("/urgent-level")
    @Operation(
            summary = "문의 긴급도 수정 (관리자)",
            description = "관리자가 특정 문의의 긴급도를 별도로 수정합니다.<br />"
                    + "긴급도 레벨: HIGH, MIDDLE, LOW"
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> updateUrgentLevel(
            @Parameter(description = "긴급도 수정 요청") @Valid @RequestBody UrgentLevelRequestWrapper request) {

        try {
            InquiryResponseDTO response = inquiryService.updateUrgentLevel(request.inquiryId(), request.urgentLevel());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // 관리자 답변 등록 메서드 (파일 업로드 지원)
    @PostMapping("/reply")
    @Operation(
            summary = "문의 답변 등록 (관리자)",
            description = "관리자가 문의에 대한 답변을 등록합니다.<br /> "
                    + "최초 답변인 경우 '답변 완료' 상태로, 추가 답변인 경우 '추가 문의' 상태로 변경됩니다.<br/>"
                    + "내용은 최소 5자 이상 작성해야 합니다.<br/>"
                    + "이미지 파일 및 문서 파일 첨부 가능<br/>"
                    + "- 이미지: JPG, PNG, WebP (최대 5MB)<br/>"
                    + "- 문서: PDF, DOC, DOCX, XLS, XLSX, HWP (최대 10MB)"
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> createReply(
            @Parameter(hidden = true) @AuthenticationPrincipal Object adminPrincipal,

            @Parameter(description = "문의 ID")
            @RequestParam("inquiryId")
            @NotBlank(message = "문의 ID는 필수입니다") String inquiryId,

            @Parameter(description = "답변 내용")
            @RequestParam("content")
            @NotBlank(message = "답변 내용은 필수입니다")
            @Size(max = 2000, message = "답변 내용은 2,000자를 초과할 수 없습니다") String content,

            @RequestParam(value = "images", required = false)
            @Parameter(description = "첨부 이미지 파일들 (선택사항)")
            MultipartFile[] imageFiles,

            @RequestParam(value = "documents", required = false)
            @Parameter(description = "첨부 문서 파일들 (선택사항)")
            MultipartFile[] documentFiles) {

        try {
            // ✅ 서비스에 모든 처리 위임
            String adminId = adminPrincipal != null ? adminPrincipal.toString() : "SYSTEM";

            // ✅ 매개변수 순서가 바뀐 메서드 호출
            InquiryResponseDTO response = inquiryService.createAdminReplyWithFiles(
                    inquiryId, content, imageFiles, documentFiles, adminId);

            log.info("관리자 답변 등록 완료 - inquiryId: {}, 이미지: {}, 문서: {}",
                    inquiryId,
                    imageFiles != null ? imageFiles.length : 0,
                    documentFiles != null ? documentFiles.length : 0);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(ResponseCode.CREATED, response)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalStateException e) {
            log.warn("종료된 문의에 답변 시도 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 답변 등록 실패 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 답변 등록 중 서버 오류 - inquiryId: {}", inquiryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PatchMapping("/close")
    @Operation(
            summary = "문의 강제 종료 (관리자)",
            description = "관리자가 문의를 강제로 종료합니다.<br />"
                    + "악성 문의, 장기간 미응답, 스팸, 부적절한 내용 등을 차단할 때 사용합니다.<br/>"
                    + "종료 사유는 필수입니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> forceCloseInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal Object adminPrincipal,
            @Parameter(description = "종료 사유 (필수)")
            @Valid @RequestBody CloseRequestWrapper request) {

        try {
            String adminId = adminPrincipal != null ? adminPrincipal.toString() : "SYSTEM";

            // ✅ 컨트롤러에서는 단순히 파라미터만 전달
            InquiryResponseDTO response = inquiryService.closeInquiryByAdmin(
                    request.inquiryId(), adminId, request.reason());

            log.info("관리자 문의 강제 종료 완료 - inquiryId: {}, adminId: {}, reason: {}",
                    request.inquiryId(), adminId, request.reason());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalStateException e) {
            log.warn("문의 강제 종료 실패 - inquiryId: {}, error: {}",
                    request.inquiryId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("문의 강제 종료 중 서버 오류 - inquiryId: {}", request.inquiryId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }


    // 관리자용 파일 다운로드
    @GetMapping("/{inquiryId}/files/{fileId}")
    @Operation(
            summary = "문의 첨부 파일 다운로드 (관리자)",
            description = "관리자가 문의에 첨부된 파일(이미지/문서)을 다운로드합니다."
    )
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String inquiryId,
            @PathVariable String fileId) {

        try {
            Resource resource = inquiryFileService.downloadAdminFileWithoutValidation(inquiryId, fileId);

            // ✅ 파일명 생성을 서비스에 위임
            String fileName = inquiryFileService.generateAdminDownloadFileName(fileId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (EntityNotFoundException e) {
            log.warn("관리자 - 파일을 찾을 수 없음: inquiryId: {}, fileId: {}", inquiryId, fileId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("관리자 파일 다운로드 실패 - inquiryId: {}, fileId: {}", inquiryId, fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // 컨트롤러 전용 래퍼 클래스들
    public record ReplyRequestWrapper(
            @NotBlank(message = "문의 ID는 필수입니다")
            String inquiryId,

            @NotBlank(message = "답변 내용은 필수입니다")
            @Size(max = 2000, message = "답변 내용은 2,000자를 초과할 수 없습니다")
            String content
    ) {}

    public record CloseRequestWrapper(
            @NotBlank(message = "문의 ID는 필수입니다")
            String inquiryId,

            @NotBlank(message = "강제 종료 시, 사유는 필수입니다.")
            @Size(max = 200, message = "종료 사유는 200자 이내로 입력해주세요.")
            String reason
    ) {}

    public record UrgentLevelRequestWrapper(
            @NotBlank(message = "문의 ID는 필수입니다")
            String inquiryId,

            @NotNull(message = "긴급도는 필수입니다")
            InquiryUrgentLevel urgentLevel
    ) {}
}