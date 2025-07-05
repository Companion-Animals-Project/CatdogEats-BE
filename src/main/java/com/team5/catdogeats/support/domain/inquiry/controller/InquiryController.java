package com.team5.catdogeats.support.domain.inquiry.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.inquiry.dto.*;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AccessDeniedException;  // 👈 이거 추가
import org.springframework.web.multipart.MultipartFile;

//  1:1 문의 사용자 Controller
//  로그인한 사용자(판매자, 구매자)가 사용하는 CRUD 기능
@Slf4j
@RestController
@RequestMapping("/v1/users/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry (User)", description = "1:1 문의 사용자 API - 로그인한 사용자만 접근 가능")
public class InquiryController {

    private final InquiryService inquiryService;

    // 사용자별 문의 목록 조회 (페이징)
    @GetMapping
    @Operation(
            summary = "내 문의 목록 조회",
            description = "로그인한 사용자의 문의 목록을 페이징으로 조회합니다. 제목, 내용 미리보기, 상태, 작성일만 표시됩니다."
    )
    public ResponseEntity<ApiResponse<Page<InquiryListResponseDTO>>> getUserInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
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

            Page<InquiryListResponseDTO> inquiries = inquiryService.getUserInquiries(
                    userPrincipal.providerId(), pageable);

            log.info("사용자 문의 목록 조회 완료 - providerId: {}, page: {}, size: {}",
                    userPrincipal.providerId(), page, size);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, inquiries)
            );
        } catch (IllegalArgumentException e) {
            log.warn("사용자 문의 목록 조회 실패 - providerId: {}, error: {}",
                    userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("사용자 문의 목록 조회 중 서버 오류 - providerId: {}",
                    userPrincipal.providerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // 문의 상세 조회
    @GetMapping("/{inquiryId}")
    @Operation(
            summary = "내 문의 상세 조회",
            description = "로그인한 사용자의 특정 문의 상세 내용을 조회합니다. 제목, 전체 내용, 상태, 답변을 확인할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<InquiryDetailResponseDTO>> getInquiryDetail(
            @PathVariable String inquiryId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            InquiryDetailResponseDTO inquiry = inquiryService.getUserInquiryDetail(
                    inquiryId, userPrincipal.providerId());
            log.info("문의 상세 조회 완료 - inquiryId: {}, providerId: {}", inquiryId, userPrincipal.providerId());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, inquiry)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", inquiryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (AccessDeniedException e) {
            log.warn("접근 권한 없음 - inquiryId: {}, providerId: {}, error: {}",
                    inquiryId, userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("문의 상세 조회 실패 - inquiryId: {}, providerId: {}, error: {}",
                    inquiryId, userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("문의 상세 조회 중 서버 오류 - inquiryId: {}, providerId: {}", inquiryId, userPrincipal.providerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // 문의 등록
    @PostMapping
    @Operation(
            summary = "1:1 문의 등록",
            description = "1:1 문의를 등록합니다. 주문 관련 문의인 경우 주문 ID를 포함할 수 있습니다.<br/>"
                    + "제목은 최소 5자 이상, 내용은 최소 10자 이상 작성해야 합니다.<br/>"
                    + "문의 유형: PRODUCT, ORDER, PAYMENT, DELIVERY, RETURN, ACCOUNT, ETC"
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> createInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "문의 등록 정보")
            @Valid @ModelAttribute InquiryCreateRequestDTO request,
            @RequestParam(value = "images", required = false) MultipartFile[] imageFiles) {

        try {
            InquiryResponseDTO response = inquiryService.createInquiry(userPrincipal.providerId(), request);
            log.info("문의 등록 완료 - inquiryId: {}, providerId: {}", response.inquiryId(), userPrincipal.providerId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(ResponseCode.CREATED, response)
            );
        } catch (IllegalArgumentException e) {
            log.warn("문의 등록 실패 - providerId: {}, error: {}", userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("문의 등록 중 서버 오류 - providerId: {}", userPrincipal.providerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PostMapping("/followup")
    @Operation(
            summary = "문의 답글 등록 (유저)",
            description = "사용자가 기존 문의나 관리자 답변에 대한 추가 답글을 등록합니다. "
                    + "답글 등록 시 문의 상태가 '추가 문의'로 변경됩니다.<br/>"
                    + "내용은 최소 5자 이상 작성해야 합니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> createFollowup(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "답글 등록 정보")
            @Valid @RequestBody FollowupRequestWrapper request) {

        try {
            // InquiryRequestDTO 생성
            InquiryRequestDTO inquiryRequest = InquiryRequestDTO.forContent(request.content());

            InquiryResponseDTO response = inquiryService.createUserFollowup(
                    request.inquiryId(), userPrincipal.providerId(), inquiryRequest);

            log.info("사용자 답글 등록 완료 - inquiryId: {}, providerId: {}, followupId: {}",
                    request.inquiryId(), userPrincipal.providerId(), response.inquiryId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(ResponseCode.CREATED, response)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (AccessDeniedException e) {
            log.warn("접근 권한 없음 - inquiryId: {}, providerId: {}, error: {}",
                    request.inquiryId(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage())
            );
        } catch (IllegalStateException e) { // ✅ 추가: 종료된 문의 예외 처리
            log.warn("종료된 문의에 답글 시도 - inquiryId: {}, providerId: {}, error: {}",
                    request.inquiryId(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("사용자 답글 등록 실패 - inquiryId: {}, providerId: {}, error: {}",
                    request.inquiryId(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("사용자 답글 등록 중 서버 오류 - inquiryId: {}, providerId: {}",
                    request.inquiryId(), userPrincipal.providerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PatchMapping("/close")
    @Operation(
            summary = "문의 종료 (유저)",
            description = "사용자가 본인의 문의를 종료합니다.<br />"
                    + "문의가 해결되었거나 더 이상 답변이 필요하지 않을 때 사용합니다.<br/>"
                    + "이미 종료된 문의는 종료할 수 없습니다."
    )
    public ResponseEntity<ApiResponse<InquiryResponseDTO>> closeInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "문의 종료 요청(유저)") @Valid @RequestBody CloseInquiryRequestWrapper request) {

        try {
            InquiryResponseDTO response = inquiryService.closeInquiryByUser(
                    request.inquiryId(), userPrincipal.providerId());

            log.info("사용자 문의 종료 완료 - inquiryId: {}, providerId: {}",
                    request.inquiryId(), userPrincipal.providerId());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );
        } catch (EntityNotFoundException e) {
            log.warn("문의를 찾을 수 없음 - inquiryId: {}, error: {}", request.inquiryId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (AccessDeniedException e) {
            log.warn("접근 권한 없음 - inquiryId: {}, providerId: {}, error: {}",
                    request.inquiryId(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage())
            );
        } catch (IllegalStateException e) {
            log.warn("문의 종료 실패 - inquiryId: {}, providerId: {}, error: {}",
                    request.inquiryId(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("문의 종료 중 서버 오류 - inquiryId: {}, providerId: {}",
                    request.inquiryId(), userPrincipal.providerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }


    // 컨트롤러 전용 래퍼 클래스들 (간단한 요청을 위해)
    public record FollowupRequestWrapper(
            @NotBlank(message = "문의 ID는 필수입니다")
            String inquiryId,

            @NotBlank(message = "내용은 필수입니다")
            @Size(min = 5, max = 2000, message = "내용은 5자 이상 2,000자 이하로 입력해주세요")
            String content
    ) {}

    public record CloseInquiryRequestWrapper(
            @NotBlank(message = "문의 ID는 필수입니다")
            String inquiryId
    ) {}

    // 이미지 다운로드
    @GetMapping("/{inquiryId}/images/{imageId}")
    public ResponseEntity<Resource> downloadImage(
            @PathVariable String inquiryId,
            @PathVariable String imageId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        // 권한 검증 + 이미지 다운로드
        return null;
    }
}