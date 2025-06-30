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
     * 이름 또는 이메일로 검색
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findByNameOrEmailContainingIgnoreCase(@Param("search") String search, Pageable pageable);

    /**
     * ACTIVE: 활성화 + 첫 로그인 완료된 사용자
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = true AND a.isFirstLogin = false ORDER BY a.createdAt DESC")
    Page<Admins> findActiveAdmins(Pageable pageable);

    /**
     * PENDING: 비활성화 + 인증코드 있음 (인증 대기중)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = false AND a.verificationCode IS NOT NULL ORDER BY a.createdAt DESC")
    Page<Admins> findPendingAdmins(Pageable pageable);

    /**
     * INACTIVE: 비활성화된 사용자
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(a.isActive = false AND a.verificationCode IS NULL) OR " +
            "(a.isActive = true AND a.isFirstLogin = true) " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findInactiveAdmins(Pageable pageable);


    /**
     * 부서별 필터링
     */
    @Query("SELECT a FROM Admins a WHERE a.department = :department ORDER BY a.createdAt DESC")
    Page<Admins> findByDepartmentWithPagination(@Param("department") Department department, Pageable pageable);


    // ===== 복합 검색 메서드들 (검색어 + 상태 필터) =====

    /**
     * 검색어 + ACTIVE 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = true AND a.isFirstLogin = false " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findActiveAdminsBySearch(@Param("search") String search, Pageable pageable);

    /**
     * 검색어 + PENDING 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = false AND a.verificationCode IS NOT NULL " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findPendingAdminsBySearch(@Param("search") String search, Pageable pageable);

    /**
     * 검색어 + INACTIVE 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "((a.isActive = false AND a.verificationCode IS NULL) OR " +
            "(a.isActive = true AND a.isFirstLogin = true)) " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findInactiveAdminsBySearch(@Param("search") String search, Pageable pageable);

    /**
     * 검색어 + 부서 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.department = :department " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findByDepartmentAndSearch(@Param("department") Department department,
                                           @Param("search") String search,
                                           Pageable pageable);

    // ===== 통계용 카운트 메서드들  =====

    /**
     * 활성 사용자 수 (활성화 + 첫 로그인 완료)
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE a.isActive = true AND a.isFirstLogin = false")
    long countActiveAdmins();


    /**
     * 대기중 사용자 수 (비활성화 + 인증코드 있음) = 승인대기
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE a.isActive = false AND a.verificationCode IS NOT NULL")
    long countPendingAdmins();

    /**
     * 비활성 사용자 수 (비활성화 또는 첫 로그인 안함) = 촛 ㄹ호
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE " +
            "(a.isActive = false AND a.verificationCode IS NULL) OR " +
            "(a.isActive = true AND a.isFirstLogin = true)")
    long countInactiveAdmins();

}

