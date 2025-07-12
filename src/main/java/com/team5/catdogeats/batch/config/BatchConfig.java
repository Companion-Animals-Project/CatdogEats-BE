package com.team5.catdogeats.batch.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final DataSource dataSource;

    @PostConstruct
    public void initializeSchema() {
        try (Connection connection = dataSource.getConnection()) {
            // PostgreSQL용 Spring Batch 스키마 스크립트 실행
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql"));
            log.info("Spring Batch schema initialized successfully!");
        } catch (Exception e) {
            // 테이블이 이미 존재하는 경우 무시
            if (e.getMessage().contains("already exists")) {
                log.error("Spring Batch schema already exists, skipping initialization.");
            } else {
                log.error("Error initializing Spring Batch schema: " + e.getMessage());
            }
        }
    }

    @Bean
    public JobRepository jobRepository(
            DataSource dataSource,
            @Qualifier("batchTransactionManager") PlatformTransactionManager transactionManager // 반드시 MyBatis용!
    )  {
        try {
            JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTransactionManager(transactionManager);
            // 필수 옵션 (Spring Boot 3.x 이상, JDBC 기반 테이블 스키마)
            factory.setIsolationLevelForCreate("ISOLATION_DEFAULT");
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Bean(name = "batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
