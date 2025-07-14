package com.team5.catdogeats.outbox.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record OutboxEventData(String id,
                              @JsonProperty("aggregate_id") String aggregateId,
                              @JsonProperty("aggregate_type") String aggregateType,
                              @JsonProperty("event_type") String eventType,
                              String payload,
                              String status,
                              @JsonProperty("retry_count") Integer retryCount,
                              @JsonProperty("error_message") String errorMessage,
                              @JsonProperty("processed_At") ZonedDateTime processedAt) {
}
