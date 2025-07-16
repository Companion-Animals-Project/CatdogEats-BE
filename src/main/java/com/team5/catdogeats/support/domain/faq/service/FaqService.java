package com.team5.catdogeats.support.domain.faq.service;

import com.team5.catdogeats.support.domain.enums.FaqCategory;
import com.team5.catdogeats.support.domain.faq.dto.request.FaqCreateRequestDTO;
import com.team5.catdogeats.support.domain.faq.dto.request.FaqUpdateRequestDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqAdminDetailResponseDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqAdminListResponseDTO;
import com.team5.catdogeats.support.domain.faq.dto.response.FaqResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * FAQ 서비스 인터페이스
 * - 퍼블릭과 관리자 기능을 모두 포함
 * - 비즈니스 로직과 검증 로직 처리
 */
public interface FaqService {

    // ========== 퍼블릭 기능 ==========

    /**
     * 퍼블릭 - FAQ 목록 조회 (검색/필터링 포함)
     *
     * @param category 카테고리 (ALL 이면 전체 조회)
     * @param keyword 검색 키워드 (제목+내용+키워드 태그 통합 검색)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 FAQ 목록
     */
    Page<FaqResponseDTO> searchFaqs(FaqCategory category, String keyword, int page, int size);

    /**
     * 퍼블릭 - 인기 검색어 목록 조회
     *
     * @return 하드코딩된 인기 검색어 목록
     */
    List<String> getPopularKeywords();

    // ========== 관리자 기능 ==========

    /**
     * 관리자 - FAQ 목록 조회 (검색/필터링 포함)
     *
     * @param category 카테고리 (ALL 이면 전체 조회)
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 페이징된 FAQ 관리자 목록
     */
    Page<FaqAdminListResponseDTO> getAdminFaqList(FaqCategory category, String keyword, int page, int size);

    /**
     * 관리자 - FAQ 상세 조회
     *
     * @param faqId FAQ ID
     * @return FAQ 상세 정보
     * @throws IllegalArgumentException FAQ가 존재하지 않는 경우
     */
    FaqAdminDetailResponseDTO getFaqDetail(String faqId);

    /**
     * 관리자 - FAQ 등록
     *
     * @param requestDTO FAQ 등록 요청 데이터
     * @return 등록된 FAQ ID
     * @throws IllegalArgumentException displayOrder 중복 등 검증 실패 시
     */
    String createFaq(FaqCreateRequestDTO requestDTO);

    /**
     * 관리자 - FAQ 수정
     *
     * @param faqId FAQ ID
     * @param requestDTO FAQ 수정 요청 데이터
     * @throws IllegalArgumentException FAQ가 존재하지 않거나 검증 실패 시
     */
    void updateFaq(String faqId, FaqUpdateRequestDTO requestDTO);

    /**
     * 관리자 - FAQ 삭제
     *
     * @param faqId FAQ ID
     * @throws IllegalArgumentException FAQ가 존재하지 않는 경우
     */
    void deleteFaq(String faqId);

    // ========== 내부 검증 메서드들 (구현체에서 private 으로 구현) ==========
    // validateDisplayOrder() - displayOrder 중복 검증
    // validateFaqExists() - FAQ 존재 여부 검증
    // reorderDisplayOrders() - displayOrder 재정렬
    // convertToDTO() - Entity → DTO 변환
}