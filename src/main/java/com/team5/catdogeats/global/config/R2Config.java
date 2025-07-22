package com.team5.catdogeats.global.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Slf4j
@Getter
@Configuration
public class R2Config {

    @Value("${cloud.r2.bucket}")
    private String bucket;
    @Value("${cloud.r2.endpoint}")
    private String endpoint;
    @Value("${cloud.r2.domain}")
    private String domain;

    @Value("${cloud.r2.access-key}")
    private String accessKey;
    @Value("${cloud.r2.secret-key}")
    private String secretKey;


    @Bean
    public S3Client r2Client() {

        return S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }
}

