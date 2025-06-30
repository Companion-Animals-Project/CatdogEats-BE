package com.team5.catdogeats.support.domain.inquiry.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryReplyRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryDetailResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.InquiryListResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 1:1 문의 관리자 전용 Controller
@Slf4j
@RestController
@RequestMapping("/v1/admin/inquiries")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')") // 스웨거 테스트용 주석 처리
@Tag(name = "Inquiry (Admin)", description = "1:1 문의 관리자 API - 관리자만 접근 가능")
public class InquiryAdminController {

    private final InquiryService inquiryService;

    // 모든 문의 목록 조회 (관리자용 - 페이징)
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
            // 수동으로 Pageable 생성
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

    // 문의 상세 조회 (관리자용)
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

    // 문의 답변 등록 (관리자만)
    @PostMapping("/{inquiryId}")
    @Operation(
            summary = "문의 답변 등록 (관리자)",
            description = "관리자가 특정 문의에 대한 답변을 등록합니다. 답변 등록 시 문의 상태가 '답변 완료'로 변경됩니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> createReply(
            @Parameter(description = "문의 ID")
            @PathVariable String inquiryId,
            @Parameter(hidden = true) @AuthenticationPrincipal String adminId,
            @Parameter(description = "답변 등록 정보")
            @Valid @RequestBody InquiryReplyRequestDTO request) {

        try {
            InquiryResponseDTO response = inquiryService.createReply(inquiryId, adminId, request);
            log.info("문의 답변 등록 완료 - inquiryId: {}, adminId: {}, replyId: {}",
                    inquiryId, adminId, response.inquiryId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(ResponseCode.CREATED, response)
            );
        } catch (IllegalArgumentException e) {
            log.warn("문의 답변 등록 실패 - inquiryId: {}, adminId: {}, error: {}",
                    inquiryId, adminId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("문의 답변 등록 중 서버 오류 - inquiryId: {}, adminId: {}", inquiryId, adminId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }
}