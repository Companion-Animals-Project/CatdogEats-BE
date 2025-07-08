package com.team5.catdogeats.global.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
public class DebeziumConfig {

    @Value("${debezium.database.hostname}")
    private String dbHost;

    @Value("${debezium.database.port}")
    private int dbPort;

    @Value("${debezium.database.username}")
    private String dbUser;

    @Value("${debezium.database.password}")
    private String dbPassword;

    @Value("${debezium.database.dbname}")
    private String dbName;

    @Value("${debezium.database.server-name}")
    private String dbServerName;

    @Value("${debezium.connector.slot-name}")
    private String slotName;

    @Value("${debezium.connector.publication-name}")
    private String publicationName;

    @Value("${debezium.connector.plugin-name}")
    private String pluginName;

    @Value("${debezium.connector.table-include-list}")
    private String tableIncludeList;

    @Value("${debezium.offset.storage-file}")
    private String offsetFilePath;

    @Bean
    public io.debezium.config.Configuration createConnectorConfiguration() {
        try {
            return io.debezium.config.Configuration.create()
                    .with("name", "outbox-postgres-connector")
                    .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
                    .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                    .with("offset.storage.file.filename", offsetFilePath)

                    .with("database.hostname", dbHost)
                    .with("database.port", String.valueOf(dbPort))
                    .with("database.user", dbUser)
                    .with("database.password", dbPassword)
                    .with("database.dbname", dbName)
                    .with("database.server.name", dbServerName)

                    .with("plugin.name", pluginName)
                    .with("slot.name", slotName)
                    .with("publication.name", publicationName)
                    .with("slot.drop.on.stop", "false")  // 중지 시 슬롯 삭제 방지


                    .with("table.include.list", tableIncludeList)

                    .with("topic.prefix", "outbox-events")
                    .with("transforms.outbox.route.topic.replacement", "outbox.events")
                    .with("transforms.outbox.table.field.event.id", "id")
                    .with("transforms.outbox.table.field.event.key", "aggregate_id")
                    .with("transforms.outbox.route.by.field", "aggregate_type")
                    .with("transforms.outbox.table.field.event.type", "event_type")
                    .with("transforms.outbox.table.field.event.payload", "payload")

                    .with("tombstones.on.delete", "false")
                    .with("provide.transaction.metadata", "false")
                    .with("value.converter.schemas.enable", "true")
                    .with("key.converter.schemas.enable", "true")

                    .build();
        } catch (Exception e) {
            log.error("Failed to create Debezium configuration", e);
            throw new RuntimeException(e);
        }
    }
}

