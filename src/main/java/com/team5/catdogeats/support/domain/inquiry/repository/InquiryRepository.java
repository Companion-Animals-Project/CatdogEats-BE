package com.team5.catdogeats.support.domain.inquiry.repository;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquires, String> {

    // 특정 사용자의 문의 목록 조회 (페이징)
    @Query(value = "SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "WHERE i.users.id = :userId AND i.parent IS NULL " +
            "ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Inquires i WHERE i.users.id = :userId AND i.parent IS NULL")
    Page<Inquires> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);

    // 관리자: 모든 문의 목록 조회 (페이징)
    @Query(value = "SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "WHERE i.parent IS NULL " +
            "ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Inquires i WHERE i.parent IS NULL")
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

    // 특정 문의의 모든 답글들을 시간순으로 조회
    @Query("SELECT i FROM Inquires i WHERE i.parent.id = :parentId ORDER BY i.createdAt ASC")
    List<Inquires> findByParentIdOrderByCreatedAtAsc(@Param("parentId") String parentId);

    // ✅ 문의 조회 + 사용자 권한 검증을 한 번에
    @Query("SELECT i FROM Inquires i " +
            "JOIN FETCH i.users u " +
            "WHERE i.id = :inquiryId AND u.providerId = :providerId " +
            "AND u.provider IN ('google', 'kakao', 'naver')")
    Optional<Inquires> findByIdAndUserProviderId(@Param("inquiryId") String inquiryId,
                                                 @Param("providerId") String providerId);

    // ✅ 사용자의 문의 목록 조회도 개선
    @Query("SELECT i FROM Inquires i " +
            "JOIN FETCH i.users u " +
            "WHERE u.providerId = :providerId " +
            "AND u.provider IN ('google', 'kakao', 'naver') " +
            "AND i.parent IS NULL " +  // 루트 문의만
            "ORDER BY i.createdAt DESC")
    Page<Inquires> findByUserProviderIdOrderByCreatedAtDesc(@Param("providerId") String providerId,
                                                            Pageable pageable);
}