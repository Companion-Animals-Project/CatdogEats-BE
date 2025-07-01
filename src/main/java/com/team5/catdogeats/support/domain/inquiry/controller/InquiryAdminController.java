package com.team5.catdogeats.support.domain.inquiry.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/admin/inquiries")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')") // 스웨거 테스트용 주석 처리
@Tag(name = "Inquiry (Admin)", description = "1:1 문의 관리자 API - 관리자만 접근 가능")
public class InquiryAdminController {

    private final InquiryService inquiryService;

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

    @PatchMapping("/{inquiryId}/urgent-level")
    @Operation(
            summary = "문의 긴급도 수정 (관리자)",
            description = "관리자가 특정 문의의 긴급도를 별도로 수정합니다.<br />"
                    + "긴급도 레벨: HIGH, MIDDLE, LOW"
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> updateUrgentLevel(
            @PathVariable String inquiryId,
            @Valid @RequestBody UrgentLevelRequestWrapper request) {

        try {
            InquiryResponseDTO response = inquiryService.updateUrgentLevel(inquiryId, request.urgentLevel());

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

    @PostMapping("/{inquiryId}/reply")
    @Operation(
            summary = "문의 답변 등록 (관리자)",
            description = "관리자가 문의에 대한 답변을 등록합니다.<br /> "
                    + "최초 답변인 경우 '답변 완료' 상태로, 추가 답변인 경우 '추가 문의' 상태로 변경됩니다.<br/>"
                    + "내용은 최소 5자 이상 작성해야 합니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> createReply(
            @Parameter(description = "문의 ID")
            @PathVariable String inquiryId,
            @Parameter(hidden = true) @AuthenticationPrincipal Object adminPrincipal,
            @Parameter(description = "답변 등록 정보")
            @Valid @RequestBody ReplyRequestWrapper request) {

        try {
            String adminId = adminPrincipal != null ? adminPrincipal.toString() : "SYSTEM";

            // InquiryRequestDTO 생성
            InquiryRequestDTO inquiryRequest = InquiryRequestDTO.forContent(request.content());

            InquiryResponseDTO response = inquiryService.createAdminReply(inquiryId, adminId, inquiryRequest);
            log.info("관리자 답변 등록 완료 - inquiryId: {}, adminId: {}, replyId: {}",
                    inquiryId, adminId, response.inquiryId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(ResponseCode.CREATED, response)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalStateException e) { // ✅ 추가: 종료된 문의 예외 처리
            log.warn("종료된 문의에 답변 시도 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 답변 등록 실패 - inquiryId: {}, error: {}",
                    inquiryId, e.getMessage());
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

    @PatchMapping("/{inquiryId}/close")
    @Operation(
            summary = "문의 강제 종료 (관리자)",
            description = "관리자가 문의를 강제로 종료합니다.<br />"
                    + "악성 문의, 장기간 미응답, 스팸, 부적절한 내용 등을 차단할 때 사용합니다.<br/>"
                    + "종료 사유는 필수입니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> forceCloseInquiry(
            @Parameter(description = "문의 ID")
            @PathVariable String inquiryId,
            @Parameter(hidden = true) @AuthenticationPrincipal Object adminPrincipal,
            @Parameter(description = "종료 사유 (필수)")
            @Valid @RequestBody CloseRequestWrapper request) {

        try {
            String adminId = adminPrincipal != null ? adminPrincipal.toString() : "SYSTEM";

            // InquiryRequestDTO 생성
            InquiryRequestDTO inquiryRequest = InquiryRequestDTO.forClose(request.reason());

            InquiryResponseDTO response = inquiryService.closeInquiryByAdmin(
                    inquiryId, adminId, inquiryRequest);

            log.info("관리자 문의 강제 종료 완료 - inquiryId: {}, adminId: {}, reason: {}",
                    inquiryId, adminId, request.reason());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalStateException e) {
            log.warn("문의 강제 종료 실패 - inquiryId: {}, error: {}",
                    inquiryId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("문의 강제 종료 중 서버 오류 - inquiryId: {}", inquiryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // 컨트롤러 전용 래퍼 클래스들
    public record ReplyRequestWrapper(
            @jakarta.validation.constraints.NotBlank(message = "답변 내용은 필수입니다")
            @jakarta.validation.constraints.Size(max = 2000, message = "답변 내용은 2,000자를 초과할 수 없습니다")
            String content
    ) {}

    public record CloseRequestWrapper(
            @jakarta.validation.constraints.NotBlank(message = "강제 종료 시, 사유는 필수입니다.")
            @jakarta.validation.constraints.Size(max = 200, message = "종료 사유는 200자 이내로 입력해주세요.")
            String reason
    ) {}

    public record UrgentLevelRequestWrapper(
            @jakarta.validation.constraints.NotNull(message = "긴급도는 필수입니다")
            InquiryUrgentLevel urgentLevel
    ) {}
}