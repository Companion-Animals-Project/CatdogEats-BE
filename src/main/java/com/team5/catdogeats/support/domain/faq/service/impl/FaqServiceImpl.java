package com.team5.catdogeats.support.domain.faq.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.support.domain.Faqs;
import com.team5.catdogeats.support.domain.enums.FaqCategory;
import com.team5.catdogeats.support.domain.faq.dto.request.FaqCreateRequestDTO;
import com.team5.catdogeats.support.domain.faq.dto.request.FaqUpdateRequestDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqAdminDetailResponseDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqAdminListResponseDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqResponseDTO;
import com.team5.catdogeats.support.domain.faq.repository.FaqRepository;
import com.team5.catdogeats.support.domain.faq.service.FaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;

    // ========== 퍼블릭 기능 ==========

    @Override
    public Page<FaqResponseDTO> searchFaqs(FaqCategory category, String keyword, int page, int size) {
        log.info("FAQ 검색 요청 - category: {}, keyword: {}, page: {}, size: {}",
                category, keyword, page, size);

        // 키워드 전처리
        String processedKeyword = preprocessKeyword(keyword);

        // 카테고리 처리 (ALL인 경우 null로 변환하여 전체 검색)
        FaqCategory searchCategory = (category == FaqCategory.ALL) ? null : category;

        // 페이징 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());

        // 통합 메서드 사용
        Page<Faqs> faqsPage = faqRepository.findFaqsWithKeywords(
                searchCategory, processedKeyword, pageable);

        log.info("FAQ 검색 결과 - category: {}, keyword: '{}' → '{}', 총 {}개, 현재 페이지 {}개, 페이지: {}/{}",
                category, keyword, processedKeyword, faqsPage.getTotalElements(),
                faqsPage.getNumberOfElements(), page + 1, faqsPage.getTotalPages());

        return faqsPage.map(this::convertToFaqResponseDTO);
    }

    @Override
    public List<String> getPopularKeywords() {
        log.info("인기 검색어 조회 요청");
        return Arrays.asList("배송", "환불", "제품", "알러지");
    }

    // ========== 관리자 기능 ==========

    @Override
    public Page<FaqAdminListResponseDTO> getAdminFaqList(FaqCategory category, String keyword, int page, int size) {
        log.info("관리자 FAQ 목록 조회 - category: {}, keyword: {}, page: {}, size: {}",
                category, keyword, page, size);

        // 키워드 전처리
        String processedKeyword = preprocessKeyword(keyword);

        // 카테고리 처리
        FaqCategory searchCategory = (category == FaqCategory.ALL) ? null : category;

        // 페이징 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());

        // 동일한 통합 메서드 사용
        Page<Faqs> faqsPage = faqRepository.findFaqsWithKeywords(
                searchCategory, processedKeyword, pageable);

        log.info("관리자 FAQ 목록 조회 결과 - category: {}, keyword: '{}' → '{}', 총 {}개, 현재 페이지 {}개, 페이지: {}/{}",
                category, keyword, processedKeyword, faqsPage.getTotalElements(),
                faqsPage.getNumberOfElements(), page + 1, faqsPage.getTotalPages());

        return faqsPage.map(this::convertToFaqAdminListResponseDTO);

    }

    @Override
    public FaqAdminDetailResponseDTO getFaqDetail(String faqId) {
        log.info("FAQ 상세 조회 요청 - faqId: {}", faqId);

        Faqs faqs = validateFaqExists(faqId);
        return convertToFaqAdminDetailResponseDTO(faqs);
    }

    @Override
    @JpaTransactional
    public String createFaq(FaqCreateRequestDTO requestDTO) {
        // 개선된 로깅 - 더 자세한 정보
        log.info("FAQ 등록 요청 - question: '{}', category: {}, displayOrder: {}, keywordCount: {}",
                requestDTO.getQuestion(), requestDTO.getFaqCategory(),
                requestDTO.getDisplayOrder(),
                requestDTO.getKeywords() != null ? requestDTO.getKeywords().size() : 0);

        int targetOrder = requestDTO.getDisplayOrder();

        // 벌크 업데이트로 성능 개선 - 한 번의 쿼리로 모든 순서 조정
        faqRepository.bulkUpdateDisplayOrderForInsert(targetOrder);

        // FAQ 엔티티 생성
        Faqs faqs = Faqs.builder()
                .question(requestDTO.getQuestion())
                .answer(requestDTO.getAnswer())
                .faqCategory(requestDTO.getFaqCategory())
                .displayOrder(targetOrder)
                .keywords(requestDTO.getKeywords())
                .build();

        Faqs savedFaqs = faqRepository.save(faqs);

        log.info("FAQ 등록 완료 - faqId: {}, question: '{}', category: {}, displayOrder: {}",
                savedFaqs.getId(), savedFaqs.getQuestion(),
                savedFaqs.getFaqCategory(), savedFaqs.getDisplayOrder());

        return savedFaqs.getId();
    }

    @Override
    @JpaTransactional
    public void updateFaq(String faqId, FaqUpdateRequestDTO requestDTO) {
        // 개선된 로깅 - 수정 내용 요약
        log.info("FAQ 수정 요청 - faqId: {}, 수정필드: [{}]", faqId, getUpdateFields(requestDTO));

        Faqs faqs = validateFaqExists(faqId);

        // 질문 검증 및 수정
        if (requestDTO.getQuestion() != null) {
            if (requestDTO.getQuestion().trim().isEmpty()) {
                throw new IllegalArgumentException("질문은 빈 값일 수 없습니다");
            }
            faqs.setQuestion(requestDTO.getQuestion());
        }

        // 답변 검증 및 수정
        if (requestDTO.getAnswer() != null) {
            if (requestDTO.getAnswer().trim().isEmpty()) {
                throw new IllegalArgumentException("답변은 빈 값일 수 없습니다");
            }
            faqs.setAnswer(requestDTO.getAnswer());
        }

        if (requestDTO.getFaqCategory() != null) {
            faqs.setFaqCategory(requestDTO.getFaqCategory());
        }

        if (requestDTO.getDisplayOrder() != null) {
            int newOrder = requestDTO.getDisplayOrder();
            int oldOrder = faqs.getDisplayOrder();

            if (newOrder != oldOrder) {
                // 벌크 업데이트로 순서 재정렬
                faqRepository.bulkUpdateDisplayOrderForUpdate(newOrder, faqId);
            }
            faqs.setDisplayOrder(newOrder);
        }

        if (requestDTO.getKeywords() != null) {
            faqs.setKeywords(requestDTO.getKeywords());
        }

        // 개선된 완료 로깅
        log.info("FAQ 수정 완료 - faqId: {}, question: '{}', category: {}, displayOrder: {}",
                faqId, faqs.getQuestion(), faqs.getFaqCategory(), faqs.getDisplayOrder());
    }

    @Override
    @JpaTransactional
    public void deleteFaq(String faqId) {
        log.info("FAQ 삭제 요청 - faqId: {}", faqId);

        // FAQ 존재 검증
        Faqs faqs = validateFaqExists(faqId);

        // 삭제 전 정보 로깅 (복구 참고용)
        log.info("FAQ 삭제 정보 - faqId: {}, question: '{}', category: {}, displayOrder: {}",
                faqId, faqs.getQuestion(), faqs.getFaqCategory(), faqs.getDisplayOrder());

        // 삭제
        faqRepository.delete(faqs);

        log.info("FAQ 삭제 완료 - faqId: {}", faqId);
    }

    // 헬퍼 메서드 추가
    private String getUpdateFields(FaqUpdateRequestDTO requestDTO) {
        List<String> fields = new ArrayList<>();
        if (requestDTO.getQuestion() != null) fields.add("question");
        if (requestDTO.getAnswer() != null) fields.add("answer");
        if (requestDTO.getFaqCategory() != null) fields.add("category");
        if (requestDTO.getDisplayOrder() != null) fields.add("displayOrder");
        if (requestDTO.getKeywords() != null) fields.add("keywords");
        return String.join(", ", fields);
    }

    // ========== 내부 검증 메서드들 ==========

    /**
     * 키워드 전처리 - # 제거 및 공백 처리
     */
    private String preprocessKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;  // null 반환으로 Repository 에서 키워드 검색 안함
        }
        String processed = keyword.trim();
        // "#배송" → "배송" 변환
        return processed.startsWith("#") ? processed.substring(1) : processed;
    }

    /**
     * FAQ 존재 여부 검증
     */
    private Faqs validateFaqExists(String faqId) {
        return faqRepository.findByIdWithKeywords(faqId)
                .orElseThrow(() -> {
                    log.warn("FAQ 조회 실패 - 존재하지 않는 faqId: {}", faqId);
                    return new NoSuchElementException("존재하지 않는 FAQ 입니다. ID: " + faqId);  // 🔥 NoSuchElementException 사용
                });
    }


    // ========== DTO 변환 메서드들 ==========

    /**
     * Entity → 퍼블릭 응답 DTO 변환
     */
    private FaqResponseDTO convertToFaqResponseDTO(Faqs faqs) {
        return FaqResponseDTO.builder()
                .id(faqs.getId())
                .question(faqs.getQuestion())
                .answer(faqs.getAnswer())
                .faqCategory(faqs.getFaqCategory())
                .categoryDisplayName(faqs.getFaqCategory().getDisplayName())
                .keywords(faqs.getKeywords())
                .build();
    }

    /**
     * Entity → 관리자 목록 응답 DTO 변환
     */
    private FaqAdminListResponseDTO convertToFaqAdminListResponseDTO(Faqs faqs) {
        return FaqAdminListResponseDTO.builder()
                .id(faqs.getId())
                .question(faqs.getQuestion())
                .faqCategory(faqs.getFaqCategory())
                .categoryDisplayName(faqs.getFaqCategory().getDisplayName())
                .displayOrder(faqs.getDisplayOrder())
                .createdAt(faqs.getCreatedAt())
                .keywords(faqs.getKeywords())
                .build();
    }

    /**
     * Entity → 관리자 상세 응답 DTO 변환
     */
    private FaqAdminDetailResponseDTO convertToFaqAdminDetailResponseDTO(Faqs faqs) {
        return FaqAdminDetailResponseDTO.builder()
                .id(faqs.getId())
                .question(faqs.getQuestion())
                .answer(faqs.getAnswer())
                .faqCategory(faqs.getFaqCategory())
                .categoryDisplayName(faqs.getFaqCategory().getDisplayName())
                .displayOrder(faqs.getDisplayOrder())
                .keywords(faqs.getKeywords())
                .createdAt(faqs.getCreatedAt())
                .updatedAt(faqs.getUpdatedAt())
                .build();
    }
}