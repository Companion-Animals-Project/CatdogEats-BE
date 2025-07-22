package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.config.R2Config;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class R2ServiceImpl implements ObjectStorageService {
    private final S3Client s3Client;
    private final R2Config r2Config;

    @Override
    public String uploadImage(String key, InputStream inputStream, long contentLength, String contentType) throws IOException {
        String imageKey = "images/" + key;
        byte[] fileBytes = inputStream.readAllBytes();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(r2Config.getBucket())
                        .key(imageKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(fileBytes)
        );
        return r2Config.getDomain() +  "/"  + imageKey;
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) throws IOException {
            String fileKey = "files/" + key;
            byte[] fileBytes = inputStream.readAllBytes();
        s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(r2Config.getBucket())
                            .key(fileKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(fileBytes)
            );
        return r2Config.getDomain() +  "/" + fileKey;
    }

    @Override
    public void deleteFile(String key) {
        String fileKey = "files/" + key;
        s3Client.deleteObject(builder -> builder
                .bucket(r2Config.getBucket())
                .key(fileKey)
                .build()
        );
    }

    @Override
    public void deleteImage(String key) {
        String imageKey = "images/" + key;
        s3Client.deleteObject(builder -> builder
                .bucket(r2Config.getBucket())
                .key(imageKey)
                .build()
        );
    }


}
