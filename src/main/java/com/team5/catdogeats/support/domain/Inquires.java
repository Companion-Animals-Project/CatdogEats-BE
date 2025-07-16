package com.team5.catdogeats.support.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.support.domain.enums.*;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Inquires extends BaseEntity {
    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    // 자기참조 관계, 답글 기능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Inquires parent;

    @OneToMany(mappedBy = "parent")
    @ToString.Exclude // 무한 루프 방지
    @JsonIgnore // 순환 참조 방지
    private List<Inquires> replies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admins admins;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus inquiryStatus = InquiryStatus.PENDING;

    // 문의 유형 (문의 등록)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryType inquiryType;

    // 문의 답변 수신 방법 (문의 등록)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryReceiveMethod inquiryReceiveMethod = InquiryReceiveMethod.WEB;

    // 긴급도 (1:1 문의 관리자 페이지)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryUrgentLevel inquiryUrgentLevel;

    // 답글 타입 구분 (1:1 문의 답글)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryMessageType inquiryMessageType = InquiryMessageType.QUESTION;

    // 주문내역 (문의 등록 -> 주문 관련 문의시만 값, 없으면 null 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders orders;

    // 강제 종료 사유 추가
    @Column(columnDefinition = "TEXT")
    private String reason;
}
