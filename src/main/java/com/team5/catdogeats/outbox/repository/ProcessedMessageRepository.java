package com.team5.catdogeats.outbox.repository;

import com.team5.catdogeats.outbox.domain.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}
