package com.team5.catdogeats.support.domain.inquiry.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquiryCloseRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquiryReplyRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquiryUrgentLevelRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryDetailResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryListResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/admin/inquiries")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')") // 스웨거 테스트용 주석 처리
@Tag(name = "Inquiry (Admin)", description = "1:1 문의 관리자 API - 관리자만 접근 가능")
public class InquiryAdminController {

    private final InquiryService inquiryService;
    private final AdminControllerUtils controllerUtils;

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
            @Parameter(description = "긴급도 수정 요청")
            @Valid @RequestBody InquiryUrgentLevelRequestDTO request) {

        try {
            InquiryResponseDTO response = inquiryService.updateUrgentLevel(
                    request.inquiryId(), request.urgentLevel());

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

    @PostMapping("/reply")
    @Operation(
            summary = "문의 답변 등록 (관리자)",
            description = "관리자가 문의에 대한 답변을 등록합니다.<br /> "
                    + "최초 답변인 경우 '답변 완료' 상태로, 추가 답변인 경우 '추가 문의' 상태로 변경됩니다.<br/>"
                    + "내용은 최소 5자 이상 작성해야 합니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> createReply(
            HttpSession session,
            @Parameter(description = "답변 등록 정보")
            @Valid @RequestBody InquiryReplyRequestDTO request) {

        try {
            // 세션에서 관리자 정보 가져오기
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);
            String adminId = adminInfo.adminId();

            InquiryResponseDTO response = inquiryService.createAdminReply(
                    request.inquiryId(), adminId, request.content());

            log.info("관리자 답변 등록 완료 - inquiryId: {}, adminId: {}, adminName: {}, replyId: {}",
                    request.inquiryId(), adminId, adminInfo.name(), response.inquiryId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(ResponseCode.CREATED, response)
            );
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalStateException e) {
            log.warn("종료된 문의에 답변 시도 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 답변 등록 실패 - inquiryId: {}, error: {}",
                    request.inquiryId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 답변 등록 중 서버 오류 - inquiryId: {}", request.inquiryId(), e);
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
            HttpSession session,
            @Parameter(description = "강제 종료 요청")
            @Valid @RequestBody InquiryCloseRequestDTO request) {

        try {
            // 세션에서 관리자 정보 가져오기
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);
            String adminId = adminInfo.adminId();

            InquiryResponseDTO response = inquiryService.closeInquiryByAdmin(
                    request.inquiryId(), adminId, request.reason());

            log.info("관리자 문의 강제 종료 완료 - inquiryId: {}, adminId: {}, adminName: {}, reason: {}",
                    request.inquiryId(), adminId, adminInfo.name(), request.reason());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
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
}