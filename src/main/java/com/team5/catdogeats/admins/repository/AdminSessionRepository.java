package com.team5.catdogeats.admins.repository;

import com.team5.catdogeats.admins.domain.dto.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminSessionRepository extends JpaRepository<AdminSession, String> {

    Optional<AdminSession> findByAdminId(String adminId);

    void deleteByAdminId(String adminId);
}
