package com.team5.catdogeats.outbox.repository;

import com.team5.catdogeats.outbox.domain.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, String> {
    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxMessage> findPendingMessages(@Param("limit") int limit);

    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'FAILED' AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxMessage> findFailedMessagesForRetry(@Param("maxRetries") int maxRetries, @Param("limit") int limit);

}
