package com.team5.catdogeats.admins.repository;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.enums.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admins, String> {

    /**
     * 이메일로 관리자 조회
     */
    Optional<Admins> findByEmail(String email);

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
     * ACTIVE: 활성화된 사용자 (이메일 인증 완료)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = true ORDER BY a.createdAt DESC")
    Page<Admins> findActiveAdmins(Pageable pageable);


    /**
     * INACTIVE: 비활성화된 사용자 (이메일 인증 완료)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = false ORDER BY a.createdAt DESC")
    Page<Admins> findInactiveAdmins(Pageable pageable);

    // ===== 복합 검색 메서드들 (검색어 + 상태 필터) =====

    /**
     * 검색어 + ACTIVE 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = true " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findActiveAdminsBySearch(@Param("search") String search, Pageable pageable);


    /**
     * 검색어 + INACTIVE 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = false " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findInactiveAdminsBySearch(@Param("search") String search, Pageable pageable);



    // ===== 통계용 카운트 메서드들  =====

    /**
     * 활성 사용자 수 (isActive = true인 모든 사용자)
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE a.isActive = true")
    long countActiveAdmins();


    /**
     * 전체 비활성 사용자 목록 조회 (isActive = false)
     */
    @Query("SELECT a FROM Admins a WHERE a.isActive = false")
    List<Admins> findAllInactiveAdmins();
}