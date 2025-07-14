package com.team5.catdogeats.outbox.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.outbox.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "outbox_messages",
        indexes = {
                @Index(name = "idx_outbox_message_status", columnList = "status"),
                @Index(name = "idx_outbox_message_event_type", columnList = "event_type"),
                @Index(name = "idx_outbox_message_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = ZonedDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = error;
        this.retryCount = (this.retryCount != null) ? this.retryCount + 1 : 1;
    }
}


