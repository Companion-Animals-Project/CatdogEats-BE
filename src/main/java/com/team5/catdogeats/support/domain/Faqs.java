package com.team5.catdogeats.support.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.support.domain.enums.FaqCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Faqs extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaqCategory faqCategory;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ElementCollection
    @CollectionTable(name = "faq_keywords", joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "keyword")
    private List<String> keywords;
}
