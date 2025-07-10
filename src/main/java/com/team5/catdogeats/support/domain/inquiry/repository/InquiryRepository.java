package com.team5.catdogeats.support.domain.inquiry.repository;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquires, String>, JpaSpecificationExecutor<Inquires> {


    // 관리자: 모든 문의 목록 조회 (페이징)
    @Query(value = "SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "WHERE i.parent IS NULL",
            countQuery = "SELECT COUNT(i) FROM Inquires i WHERE i.parent IS NULL")
    Page<Inquires> findAllInquiriesOrderByCreatedAtDesc(Pageable pageable);


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


    // ✅ 추가: 사용자용 문의 상세 조회 (권한 검증 + 답글까지 한번에)
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "LEFT JOIN FETCH i.replies r " +
            "LEFT JOIN FETCH r.users " +
            "WHERE i.id = :inquiryId " +
            "AND i.users.providerId = :providerId " +
            "AND i.users.provider IN ('google', 'kakao', 'naver') " +
            "AND i.parent IS NULL")
    Optional<Inquires> findRootInquiryWithRepliesByIdAndUserProviderId(@Param("inquiryId") String inquiryId,
                                                                       @Param("providerId") String providerId);

    // ✅ 추가: 관리자용 문의 상세 조회 (답글까지 한번에)
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "LEFT JOIN FETCH i.replies r " +
            "LEFT JOIN FETCH r.users " +
            "WHERE i.id = :inquiryId " +
            "AND i.parent IS NULL")
    Optional<Inquires> findRootInquiryWithRepliesById(@Param("inquiryId") String inquiryId);
}