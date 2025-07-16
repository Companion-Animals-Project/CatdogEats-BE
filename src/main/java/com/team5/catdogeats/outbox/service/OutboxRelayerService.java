package com.team5.catdogeats.outbox.service;

import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;

public interface OutboxRelayerService {
    void handleChangeEvent(RecordChangeEvent<SourceRecord> event);
}
