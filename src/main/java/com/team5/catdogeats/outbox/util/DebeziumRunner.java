package com.team5.catdogeats.outbox.util;

import com.team5.catdogeats.outbox.service.OutboxRelayerService;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DebeziumRunner {
    private final OutboxRelayerService outboxRelayerService;
    private final Configuration debeziumConfig;

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 단일 poll() 루프로 WAL 읽기 → 파이프라인 처리 → 콜백 전달.
    //동시에 두 번 이상 poll()하면 오프셋·순서가 꼬일 위험이 있어 공식적으로도 1스레드 운용을 권장한다고 합니다!

    @PostConstruct
    public void start() {
        Properties props = debeziumConfig.asProperties();

        // 내부 컨버터는 항상 JSON 컨버터를 사용
        props.setProperty("internal.key.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("internal.key.converter.schemas.enable", "true");
        props.setProperty("internal.value.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("internal.value.converter.schemas.enable", "true");
        engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(props)
                .notifying(outboxRelayerService::handleChangeEvent)
                .build();

        executor.execute(engine);
        log.info("Debezium Engine started.");
    }

    @PreDestroy
    public void stop() throws IOException {
        if (engine != null) engine.close();
        executor.shutdown();
        log.info("Debezium Engine stopped and closed.");
    }
}
