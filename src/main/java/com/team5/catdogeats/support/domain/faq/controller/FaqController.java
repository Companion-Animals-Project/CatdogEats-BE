package com.team5.catdogeats.support.domain.faq.controller;

import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.enums.FaqCategory;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqResponseDTO;
import com.team5.catdogeats.support.domain.faq.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/faqs")
@RequiredArgsConstructor
@Tag(name = "FAQ (Public)", description = "FAQ 퍼블릭 API - 사용자 조회 기능")
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(
            summary = "FAQ 목록 조회 및 검색",
            description = "사용자가 FAQ 목록을 조회합니다. 카테고리 필터링과 키워드 검색을 지원합니다.<br/><br/>" +
                    "**검색 조건:**<br/>" +
                    "- category: 카테고리 필터 (ALL, PRODUCT, ORDER, DELIVERY, RETURN, ACCOUNT, ETC)<br/>" +
                    "- keyword: 검색 키워드 (제목 + 내용 + 키워드 태그 통합 검색)<br/>" +
                    "**정렬:** displayOrder 오름차순으로 고정<br/>" +
                    "**페이징:** 기본값 page=0, size=10"
    )
    public ResponseEntity<APIResponse<Page<FaqResponseDTO>>> getFaqs(
            @Parameter(description = "카테고리 필터", example = "ALL")
            @RequestParam(defaultValue = "ALL") FaqCategory category,

            @Parameter(description = "검색 키워드 (제목 + 내용 + 키워드 태그 통합 검색)")
            @RequestParam(required = false) String keyword,


            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("FAQ 목록 조회 요청 - category: {}, keyword: {}, page: {}, size: {}",
                    category, keyword, page, size);

            Page<FaqResponseDTO> faqs = faqService.searchFaqs(category, keyword, page, size);

            log.info("FAQ 목록 조회 성공 - 총 {}개, 현재 페이지 {}개",
                    faqs.getTotalElements(), faqs.getNumberOfElements());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, faqs));
        } catch (IllegalArgumentException e) {
            log.warn("FAQ 목록 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage())
            );
        } catch (Exception e) {
            log.error("FAQ 목록 조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @GetMapping("/popular-keywords")
    @Operation(
            summary = "인기 검색어 목록 조회",
            description = "FAQ 검색에서 자주 사용되는 인기 검색어 목록을 조회합니다.<br/>" +
                    "현재는 하드코딩된 키워드를 반환하며, 사용자가 클릭 시 해당 키워드로 검색이 실행됩니다."
    )
    public ResponseEntity<APIResponse<List<String>>> getPopularKeywords() {
        try {
            log.info("인기 검색어 목록 조회 요청");

            List<String> popularKeywords = faqService.getPopularKeywords();

            log.info("인기 검색어 목록 조회 성공 - {}개", popularKeywords.size());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, popularKeywords));
        } catch (Exception e) {
            log.error("인기 검색어 목록 조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR)
            );
        }
    }
}