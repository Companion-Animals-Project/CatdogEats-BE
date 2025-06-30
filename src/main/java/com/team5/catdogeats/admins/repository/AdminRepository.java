package com.team5.catdogeats.admins.repository;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.enums.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admins, String> {

    /**
     * 이메일로 관리자 조회
     */
    Optional<Admins> findByEmail(String email);


    /**
     * 부서별 관리자 목록 조회
     */
    List<Admins> findByDepartment(Department department);

    /**
     * 만료된 인증코드를 가진 관리자 목록 조회 (정리용)
     */
    List<Admins> findByVerificationCodeExpiryBefore(ZonedDateTime expiry);

    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);

    /**
     * 이름 또는 이메일로 검색 (페이지네이션 지원)
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Admins> findByNameOrEmailContainingIgnoreCase(@Param("search") String search, Pageable pageable);

    /**
     * 상태별 필터링 (활성화된 사용자)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = true AND a.isFirstLogin = false")
    Page<Admins> findActiveAdmins(Pageable pageable);

    /**
     * 상태별 필터링 (대기중인 사용자 - 비활성화 + 인증코드 있음)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = false AND a.verificationCode IS NOT NULL")
    Page<Admins> findPendingAdmins(Pageable pageable);

    /**
     * 상태별 필터링 (비활성화된 사용자)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = false")
    Page<Admins> findInactiveAdmins(Pageable pageable);

    /**
     * 부서별 필터링 (페이지네이션 지원)
     */
    Page<Admins> findByDepartment(Department department, Pageable pageable);

    /**
     * 복합 검색: 검색어 + 상태
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = :isActive")
    Page<Admins> findBySearchAndActiveStatus(@Param("search") String search,
                                             @Param("isActive") boolean isActive,
                                             Pageable pageable);

    /**
     * 복합 검색: 검색어 + 부서
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.department = :department")
    Page<Admins> findBySearchAndDepartment(@Param("search") String search,
                                           @Param("department") Department department,
                                           Pageable pageable);

    // ===== 통계용 카운트 메서드들 =====
    long countByIsActiveTrue();
    long countByIsActiveFalseAndVerificationCodeIsNotNull();
    long countByIsActiveFalse();
}

