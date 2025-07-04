package com.team5.catdogeats.admins.repository;

import com.team5.catdogeats.admins.domain.Admins;
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
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Admins a WHERE a.email = :email AND a.isDeleted = false")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 전체 관리자 조회 (퇴사자 제외)
     */
    @Query("SELECT a FROM Admins a WHERE a.isDeleted = false ORDER BY a.createdAt DESC")
    Page<Admins> findAll(Pageable pageable);

    /**
     * 이름 또는 이메일로 검색
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "a.isDeleted = false AND " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findByNameOrEmailContainingIgnoreCase(@Param("search") String search, Pageable pageable);

    /**
     * ACTIVE: 활성화된 사용자 (이메일 인증 완료)
     */
    @Query("SELECT a FROM Admins a WHERE a.isDeleted = false AND a.isActive = true ORDER BY a.createdAt DESC")
    Page<Admins> findActiveAdmins(Pageable pageable);


    /**
     * INACTIVE: 비활성화된 사용자 (이메일 인증 완료)
     */
    @Query("SELECT a FROM Admins a WHERE a.isDeleted = false AND a.isActive = false ORDER BY a.createdAt DESC")
    Page<Admins> findInactiveAdmins(Pageable pageable);

    // ===== 복합 검색 메서드들 (검색어 + 상태 필터) =====

    /**
     * 검색어 + ACTIVE 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "a.isDeleted = false AND " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = true " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findActiveAdminsBySearch(@Param("search") String search, Pageable pageable);


    /**
     * 검색어 + INACTIVE 상태 조합
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "a.isDeleted = false AND " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "a.isActive = false " +
            "ORDER BY a.createdAt DESC")
    Page<Admins> findInactiveAdminsBySearch(@Param("search") String search, Pageable pageable);


    /**
     * 퇴사자 목록 조회
     */
    @Query("SELECT a FROM Admins a WHERE a.isDeleted = true ORDER BY a.deletedAt DESC")
    Page<Admins> findDeletedAdmins(Pageable pageable);

    /**
     * 퇴사자 검색
     */
    @Query("SELECT a FROM Admins a WHERE " +
            "a.isDeleted = true AND " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY a.deletedAt DESC")
    Page<Admins> findDeletedAdminsBySearch(@Param("search") String search, Pageable pageable);

    /**
     * 퇴사자 수 조회
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE a.isDeleted = true")
    long countDeletedAdmins();


    /**
     * 전체 관리자 수 (퇴사자 포함)
     */
    @Query("SELECT COUNT(a) FROM Admins a")
    long countAllAdmins();


    // ===== 통계용 카운트 메서드들  =====

    /**
     * 전체 관리자 수 (퇴사자 제외)
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE a.isDeleted = false")
    long count();

    /**
     * 활성 사용자 수 (isActive = true인 모든 사용자)
     */
    @Query("SELECT COUNT(a) FROM Admins a WHERE a.isDeleted = false AND a.isActive = true")
    long countActiveAdmins();


    /**
     * 전체 비활성 사용자 목록 조회 (isActive = false)
     */
    @Query("SELECT a FROM Admins a WHERE a.isDeleted = false AND a.isActive = false")
    List<Admins> findAllInactiveAdmins();



}