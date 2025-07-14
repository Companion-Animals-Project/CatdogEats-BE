package com.team5.catdogeats.support.domain.faq.repository;

import com.team5.catdogeats.support.domain.Faqs;
import com.team5.catdogeats.support.domain.enums.FaqCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FaqRepository extends JpaRepository<Faqs, String> {

    /**
     * ID로 FAQ 상세 조회 with keywords
     */
    @Query("SELECT f FROM Faqs f LEFT JOIN FETCH f.keywords WHERE f.id = :id")
    Optional<Faqs> findByIdWithKeywords(@Param("id") String id);


    /**
     * 통합 FAQ 검색 메서드 - 모든 검색 조건을 하나로 처리
     * category가 null 이면 전체 카테고리 검색
     * keyword가 null 이거나 빈 문자열이면 키워드 검색 안함
     */
    @Query("SELECT DISTINCT f FROM Faqs f " +
            "LEFT JOIN FETCH f.keywords " +
            "WHERE (:category IS NULL OR f.faqCategory = :category) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "f.question LIKE %:keyword% OR f.answer LIKE %:keyword% OR " +
            "EXISTS (SELECT 1 FROM f.keywords k WHERE k LIKE %:keyword%)) " +
            "ORDER BY f.displayOrder ASC")
    Page<Faqs> findFaqsWithKeywords(
            @Param("category") FaqCategory category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * displayOrder 벌크 업데이트 - FAQ 삽입 시 기존 순서들을 뒤로 밀기
     */
    @Modifying
    @Query("UPDATE Faqs f SET f.displayOrder = f.displayOrder + 1 " +
            "WHERE f.displayOrder >= :displayOrder")
    void bulkUpdateDisplayOrderForInsert(@Param("displayOrder") int displayOrder);

    /**
     * displayOrder 벌크 업데이트 - FAQ 순서 변경 시 사용
     */
    @Modifying
    @Query("UPDATE Faqs f SET f.displayOrder = f.displayOrder + 1 " +
            "WHERE f.displayOrder >= :newOrder AND f.id != :excludeId")
    void bulkUpdateDisplayOrderForUpdate(@Param("newOrder") int newOrder,
                                         @Param("excludeId") String excludeId);
}