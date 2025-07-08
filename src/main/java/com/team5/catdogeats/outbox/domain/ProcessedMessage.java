package com.team5.catdogeats.outbox.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "processed_messages",
        indexes = {
                @Index(name = "idx_processed_message_id", columnList = "message_id", unique = true)
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
    public class ProcessedMessage {

    @Id
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(name = "processed_at", nullable = false)
    private ZonedDateTime processedAt;

    @Column(name = "consumer_group", length = 100, nullable = false)
    private String consumerGroup;
}
