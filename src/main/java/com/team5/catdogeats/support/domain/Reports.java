package com.team5.catdogeats.support.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Reports extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 10)
    private ReportType reportType;

    @Column(name = "target_id", length = 36, nullable = false)
    private String targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reports_reporter_id"))
    private Buyers reporter;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.PENDING;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;



    // 신고 상세 내용 추가
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    // 첨부파일 URL 추가
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    // 관리자 메모 추가
    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    // 처리한 관리자 ID 추가
    @Column(name = "processed_by_admin_id", length = 36)
    private String processedByAdminId;

    // === 비즈니스 메서드 ===
    // 신고 상태 변경
    public void updateStatus(ReportStatus newStatus, String adminId, String note) {
        this.reportStatus = newStatus;
        this.processedByAdminId = adminId;
        this.adminNote = note;
        this.processedAt = ZonedDateTime.now();
    }

    // 상품 신고인지 확인
    public boolean isProductReport() {
        return this.reportType == ReportType.PRODUCT;
    }

    // 리뷰 신고인지 확인
    public boolean isReviewReport() {
        return this.reportType == ReportType.REVIEW;
    }

    // 처리 완료 여부 확인
    public boolean isProcessed() {
        return this.reportStatus != ReportStatus.PENDING;
    }
}