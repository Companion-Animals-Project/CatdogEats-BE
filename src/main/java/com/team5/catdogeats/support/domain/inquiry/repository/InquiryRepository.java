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

    // =================================
    // 관리자용 메서드들 (변경 없음)
    // =================================

    // 관리자: 모든 문의 목록 조회 (페이징)
    @Query(value = "SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "WHERE i.parent IS NULL",
            countQuery = "SELECT COUNT(i) FROM Inquires i WHERE i.parent IS NULL")
    Page<Inquires> findAllInquiriesOrderByCreatedAtDesc(Pageable pageable);

    // 관리자용 문의 상세 조회 (답글까지 한번에)
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "LEFT JOIN FETCH i.replies r " +
            "LEFT JOIN FETCH r.users " +
            "WHERE i.id = :inquiryId " +
            "AND i.parent IS NULL")
    Optional<Inquires> findRootInquiryWithRepliesById(@Param("inquiryId") String inquiryId);

    // =================================
    // 사용자용 메서드들 (새로 추가/수정)
    // =================================

    // 🆕 사용자 ID로 문의 목록 조회 (개선된 방식)
    @Query(value = "SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "WHERE i.users.id = :userId " +
            "AND i.parent IS NULL " +
            "ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Inquires i WHERE i.users.id = :userId AND i.parent IS NULL")
    Page<Inquires> findByUsersIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);

    // 🆕 사용자 ID로 문의 조회 + 권한 검증
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "WHERE i.id = :inquiryId AND i.users.id = :userId")
    Optional<Inquires> findByIdAndUsersId(@Param("inquiryId") String inquiryId, @Param("userId") String userId);

    // 🆕 사용자용 문의 상세 조회 (권한 검증 + 답글까지 한번에)
    @Query("SELECT i FROM Inquires i " +
            "LEFT JOIN FETCH i.users " +
            "LEFT JOIN FETCH i.orders " +
            "LEFT JOIN FETCH i.replies r " +
            "LEFT JOIN FETCH r.users " +
            "LEFT JOIN FETCH r.admins " +
            "WHERE i.id = :inquiryId " +
            "AND i.users.id = :userId " +
            "AND i.parent IS NULL")
    Optional<Inquires> findRootInquiryWithRepliesByIdAndUserId(@Param("inquiryId") String inquiryId,
                                                               @Param("userId") String userId);

    // =================================
    // 기존 메서드들 (하위 호환성을 위해 유지, 향후 제거 예정)
    // =================================

    // @Deprecated - 사용하지 않음, userId 기반 메서드 사용 권장
    @Query("SELECT i FROM Inquires i " +
            "JOIN FETCH i.users u " +
            "WHERE i.id = :inquiryId AND u.providerId = :providerId " +
            "AND u.provider IN ('google', 'kakao', 'naver')")
    Optional<Inquires> findByIdAndUserProviderId(@Param("inquiryId") String inquiryId,
                                                 @Param("providerId") String providerId);

    // @Deprecated - 사용하지 않음, userId 기반 메서드 사용 권장
    @Query("SELECT i FROM Inquires i " +
            "JOIN FETCH i.users u " +
            "WHERE u.providerId = :providerId " +
            "AND u.provider IN ('google', 'kakao', 'naver') " +
            "AND i.parent IS NULL " +
            "ORDER BY i.createdAt DESC")
    Page<Inquires> findByUserProviderIdOrderByCreatedAtDesc(@Param("providerId") String providerId,
                                                            Pageable pageable);

    // @Deprecated - 사용하지 않음, userId 기반 메서드 사용 권장
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
}