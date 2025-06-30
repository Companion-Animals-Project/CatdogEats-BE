package com.team5.catdogeats.support.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
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

//    @OneToMany(mappedBy = "parent")
//    @ToString.Exclude // 무한 루프 방지
//    @JsonIgnore // 순환 참조 방지
//    private List<Inquires> replies;

    // Todo: 한 번 읽어봐주시고, 의견 주시면 감사하겠습니다.
    // 초기 엔티티 파일 설계에서 1:N 양방향 매핑으로, 하나의 부모(문의)와 여러개의 자식(답변) 리스트를 가질 수 있게 되어있었습니다.
    // 그런데 1:1 문의 프론트 페이지를 확인해보시면, 하나의 문의글에 하나의 답변, 그리고 문의 등록 -> 유저 / 문의 답변 -> 관리자의 형식을 취하고 있습니다.
    // 따라서, 제 생각에는 단방향 관계만 남겨두어, 하나의 문의글에 하나의 답변만을 취하는 형식이 적절하다고 생각하고 있습니다.
    // 왜냐하면 답변은 관리자만이 가능한 기능이므로, 댓글 형식으로 확장하는 방식이 아니라면, 유저가 댓글처럼 답변을 활용할 수 없기 때문입니다.
    // 그래서 동기적으로든, 비동기적으로든 문답의 형식을 가져가는 형태는 수완님이 구현해주신 채팅쪽에서 풀어 나가는 것이 적절해 보이는데,
    // 어떻게 생각하시나요?

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
    @Builder.Default
    private InquiryUrgentLevel inquiryUrgentLevel = InquiryUrgentLevel.MEDIUM;

    // 주문내역 (문의 등록 -> 주문 관련 문의시만 값, 없으면 null 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders orders;

}
