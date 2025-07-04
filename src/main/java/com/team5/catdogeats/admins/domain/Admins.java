package com.team5.catdogeats.admins.domain;

import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.baseEntity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Admins extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole adminRole;

    @Email
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = false;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isFirstLogin = true;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column
    private ZonedDateTime lastLoginAt;


    @Column(length = 100)
    private String deleteReason;

    @Column
    private ZonedDateTime deletedAt;




    /**
     * 첫 로그인 완료 처리
     */
    public void completeFirstLogin() {
        this.isFirstLogin = false;
    }


    /**
     * 로그인 시간 업데이트
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = ZonedDateTime.now();
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }


    /**
     * 퇴사 처리
     */
    public void softDelete(String reason) {
        this.isDeleted = true;
        this.deletedAt = ZonedDateTime.now();
        this.deleteReason = reason;
        this.isActive = false; // 퇴사시 비활성화
    }

    /**
     * 퇴사 취소 (복구)
     */
    public void undoSoftDelete() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deleteReason = null;
    }

}