package com.team5.catdogeats.support.domain.faq.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.enums.FaqCategory;
import com.team5.catdogeats.support.domain.faq.dto.request.FaqCreateRequestDTO;
import com.team5.catdogeats.support.domain.faq.dto.request.FaqUpdateRequestDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqAdminDetailResponseDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqAdminListResponseDTO;
import com.team5.catdogeats.support.domain.faq.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/v1/admin/faqs")
@RequiredArgsConstructor
@Tag(name = "FAQ (Admin)", description = "FAQ 관리자 API - 관리자만 접근 가능")
public class FaqAdminController {

    private final FaqService faqService;
    private final AdminControllerUtils controllerUtils;

    @GetMapping
    @Operation(
            summary = "관리자 FAQ 목록 조회 및 검색",
            description = "관리자가 모든 FAQ 목록을 페이징으로 조회합니다. " +
                    "검색 및 필터링 조건을 지원합니다.<br/><br/>" +
                    "**검색 조건:**<br/>" +
                    "- category: 카테고리 필터 (ALL, PRODUCT, ORDER, DELIVERY, RETURN, ACCOUNT, ETC)<br/>" +
                    "- keyword: 검색 키워드 (제목 + 내용 검색)<br/><br/>" +
                    "**정렬:** displayOrder 오름차순으로 고정<br/>" +
                    "**페이징:** 기본값 page=0, size=10<br/><br/>" +
                    "**인증:** 관리자 세션 인증 필요"
    )
    public ResponseEntity<APIResponse<Page<FaqAdminListResponseDTO>>> getAdminFaqList(
            HttpSession session,

            @Parameter(description = "카테고리 필터", example = "ALL")
            @RequestParam(defaultValue = "ALL") FaqCategory category,

            @Parameter(description = "검색 키워드")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            // 세션 인증
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 FAQ 목록 조회 요청 - adminId: {}, category: {}, keyword: {}, page: {}, size: {}",
                    adminInfo.adminId(), category, keyword, page, size);

            Page<FaqAdminListResponseDTO> faqs = faqService.getAdminFaqList(category, keyword, page, size);

            log.info("관리자 FAQ 목록 조회 성공 - 총 {}개, 현재 페이지 {}개",
                    faqs.getTotalElements(), faqs.getNumberOfElements());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, faqs));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - FAQ 목록 조회 시도, error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 FAQ 목록 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 FAQ 목록 조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @GetMapping("/{faqId}")
    @Operation(
            summary = "관리자 FAQ 상세 조회",
            description = "관리자가 특정 FAQ의 상세 정보를 조회합니다. " +
                    "관리자 페이지의 FAQ 상세 조회 모달에서 사용됩니다.<br/><br/>" +
                    "**인증:** 관리자 세션 인증 필요"
    )
    public ResponseEntity<APIResponse<FaqAdminDetailResponseDTO>> getFaqDetail(
            HttpSession session,

            @Parameter(description = "FAQ ID")
            @PathVariable String faqId) {

        try {
            // 세션 인증
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 FAQ 상세 조회 요청 - adminId: {}, faqId: {}", adminInfo.adminId(), faqId);

            FaqAdminDetailResponseDTO faq = faqService.getFaqDetail(faqId);

            log.info("관리자 FAQ 상세 조회 성공 - faqId: {}", faqId);

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, faq));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - FAQ 상세 조회 시도, faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {  // 추가
            log.warn("FAQ 조회 실패 - faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 FAQ 상세 조회 실패 - faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 FAQ 상세 조회 중 서버 오류 - faqId: {}", faqId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PostMapping
    @Operation(
            summary = "관리자 FAQ 등록",
            description = "관리자가 새로운 FAQ를 등록합니다. " +
                    "관리자 페이지의 'FAQ 추가' 기능에서 사용됩니다.<br/><br/>" +
                    "카테고리: PRODUCT, ORDER, DELIVERY, RETURN, ACCOUNT, ETC<br />" +
                    "**검증 사항:**<br/>" +
                    "- 질문, 답변, 카테고리는 필수<br/>" +
                    "- 키워드는 선택사항<br/><br/>" +
                    "**인증:** 관리자 세션 인증 필요"
    )
    public ResponseEntity<APIResponse<String>> createFaq(
            HttpSession session,

            @Parameter(description = "FAQ 등록 요청 데이터")
            @Valid @RequestBody FaqCreateRequestDTO requestDTO) {

        try {
            // 세션 인증
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 FAQ 등록 요청 - adminId: {}, question: {}, category: {}",
                    adminInfo.adminId(), requestDTO.getQuestion(), requestDTO.getFaqCategory());

            String faqId = faqService.createFaq(requestDTO);

            log.info("관리자 FAQ 등록 성공 - adminId: {}, faqId: {}", adminInfo.adminId(), faqId);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    APIResponse.success(ResponseCode.CREATED, faqId)
            );
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - FAQ 등록 시도, error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 FAQ 등록 실패 - 검증 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 FAQ 등록 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PatchMapping("/{faqId}")
    @Operation(
            summary = "관리자 FAQ 수정",
            description = "관리자가 기존 FAQ를 수정합니다. " +
                    "관리자 페이지의 FAQ 상세 조회 모달에서 수정 기능으로 사용됩니다.<br/><br/>" +
                    "**검증 사항:**<br/>" +
                    "- FAQ 존재 여부 확인<br/>" +
                    "- displayOrder 중복 불가 (자신 제외)<br/>" +
                    "- 모든 필드 필수<br/><br/>" +
                    "**인증:** 관리자 세션 인증 필요"
    )
    public ResponseEntity<APIResponse<Void>> updateFaq(
            HttpSession session,

            @Parameter(description = "FAQ ID")
            @PathVariable String faqId,

            @Parameter(description = "FAQ 수정 요청 데이터")
            @Valid @RequestBody FaqUpdateRequestDTO requestDTO) {

        try {
            // 세션 인증
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 FAQ 수정 요청 - adminId: {}, faqId: {}, question: {}",
                    adminInfo.adminId(), faqId, requestDTO.getQuestion());

            faqService.updateFaq(faqId, requestDTO);

            log.info("관리자 FAQ 수정 성공 - adminId: {}, faqId: {}", adminInfo.adminId(), faqId);

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - FAQ 수정 시도, faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {  // 추가
            log.warn("FAQ 수정 실패 - faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 FAQ 수정 실패 - faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 FAQ 수정 중 서버 오류 - faqId: {}", faqId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @DeleteMapping("/{faqId}")
    @Operation(
            summary = "관리자 FAQ 삭제",
            description = "관리자가 기존 FAQ를 삭제합니다. " +
                    "관리자 페이지의 FAQ 목록 또는 상세 조회에서 삭제 기능으로 사용됩니다.<br/><br/>" +
                    "**검증 사항:**<br/>" +
                    "- FAQ 존재 여부 확인<br/>" +
                    "- 관련 키워드도 함께 삭제 (CASCADE)<br/><br/>" +
                    "**인증:** 관리자 세션 인증 필요"
    )
    public ResponseEntity<APIResponse<Void>> deleteFaq(
            HttpSession session,

            @Parameter(description = "FAQ ID")
            @PathVariable String faqId) {

        try {
            // 세션 인증
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 FAQ 삭제 요청 - adminId: {}, faqId: {}", adminInfo.adminId(), faqId);

            faqService.deleteFaq(faqId);

            log.info("관리자 FAQ 삭제 성공 - adminId: {}, faqId: {}", adminInfo.adminId(), faqId);

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (BadCredentialsException e) {
            log.warn("관리자 로그인 필요 - FAQ 삭제 시도, faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    APIResponse.error(ResponseCode.UNAUTHORIZED, "관리자 로그인이 필요합니다")
            );
        } catch (NoSuchElementException e) {  // 추가
            log.warn("FAQ 삭제 실패 - faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("관리자 FAQ 삭제 실패 - faqId: {}, error: {}", faqId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("관리자 FAQ 삭제 중 서버 오류 - faqId: {}", faqId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }
}