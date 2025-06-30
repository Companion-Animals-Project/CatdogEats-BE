package com.team5.catdogeats.support.domain.inquiry.repository;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquires, String> {

    // 특정 사용자의 문의 목록 조회 (페이징)
    // 최상위 문의만 조회 (parent가 null인 것)
    // 최신순 정렬
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +         // 사용자 정보를 한 번에 가져옴
            "LEFT JOIN FETCH i.orders " +        // 주문 정보를 한 번에 가져옴
            "WHERE i.users.id = :userId AND i.parent IS NULL " +
            "ORDER BY i.createdAt DESC")
    Page<Inquires> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);

    // 관리자: 모든 문의 목록 조회 (페이징)
    // 최상위 문의만 조회
    // 최신순 정렬
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +           // 사용자 정보 한 번에 조회
            "LEFT JOIN FETCH i.orders " +          // 주문 정보 한 번에 조회
            "WHERE i.parent IS NULL " +
            "ORDER BY i.createdAt DESC")
    Page<Inquires> findAllInquiriesOrderByCreatedAtDesc(Pageable pageable);

    // 관리자: 답변 상태별 문의 조회
    @Query("SELECT i FROM Inquires i WHERE i.parent IS NULL AND i.inquiryStatus = :status ORDER BY i.createdAt DESC")
    Page<Inquires> findByInquiryStatusOrderByCreatedAtDesc(@Param("status") InquiryStatus status, Pageable pageable);

    // 관리자: 제목 + 내용 검색
    @Query("SELECT i FROM Inquires i WHERE i.parent IS NULL AND " +
            "(LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY i.createdAt DESC")
    Page<Inquires> findByTitleOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
            @Param("keyword") String keyword, Pageable pageable);

    // 관리자: 기간별 문의 조회
    @Query("SELECT i FROM Inquires i WHERE i.parent IS NULL AND " +
            "i.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY i.createdAt DESC")
    Page<Inquires> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable);

    // 수정: 단일 답변 조회
    Optional<Inquires> findByParent_Id(String parentId);
}