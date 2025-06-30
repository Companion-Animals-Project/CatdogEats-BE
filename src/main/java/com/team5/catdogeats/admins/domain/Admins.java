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

    /**
     * 계정 활성화 상태 (인증코드 확인 완료 여부)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * 최초 로그인 여부 (비밀번호 변경 필요 체크)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFirstLogin = true;


    /**
     * 첫 로그인 완료 처리
     */
    public void completeFirstLogin() {
        this.isFirstLogin = false;
    }


    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

}