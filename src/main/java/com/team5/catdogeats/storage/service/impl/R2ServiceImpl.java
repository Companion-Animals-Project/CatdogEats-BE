package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.config.R2Config;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2ServiceImpl implements ObjectStorageService {
    private final S3Client s3Client;
    private final R2Config r2Config;

    private static final int MULTIPART_THRESHOLD = 10 * 1024 * 1024;
    private static final int CHUNK_SIZE = 10 * 1024 * 1024;
    private static final int MAX_THREADS = 3;


    @Override
    public String uploadImage(String key, MultipartFile file) throws IOException, ExecutionException, InterruptedException {
        String imageKey = "images/" + key;
        String bucket = r2Config.getBucket();

        // 파일 크기에 따른 업로드 방식 선택
        if (file.getSize() < MULTIPART_THRESHOLD) {
            return uploadSingle(imageKey, bucket, file);
        } else {
            return uploadMultipart(imageKey, bucket, file);
        }
    }

    @Override
    public String uploadFile(String key, MultipartFile file) throws IOException, ExecutionException, InterruptedException {
        String fileKey = "files/" + key;
        String bucket = r2Config.getBucket();
        if (file.getSize() < MULTIPART_THRESHOLD) {
            return uploadSingle(fileKey, bucket, file);
        } else {
            return uploadMultipart(fileKey, bucket, file);
        }
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


    private String uploadSingle(String key, String bucket, MultipartFile file) throws IOException {
        log.info("Using single upload for file: {} (size: {} bytes)", key, file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromBytes(inputStream.readAllBytes()) // R2에서 가장 안정적
            );
        }

        return r2Config.getDomain() + "/" + key;
    }


    private String uploadMultipart(String key, String bucket, MultipartFile file)
            throws IOException, ExecutionException, InterruptedException {

        log.info("Using multipart upload for file: {} (size: {} bytes)", key, file.getSize());

        CreateMultipartUploadResponse createResponse = initiateMultipartUpload(bucket, key, file.getContentType());
        String uploadId = createResponse.uploadId();

        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        List<CompletedPart> completedParts = Collections.synchronizedList(new ArrayList<>());

        try {
            uploadPartsOptimized(bucket, key, uploadId, file, completedParts, executor);
            return completeMultipartUpload(bucket, key, uploadId, completedParts);
        } catch (Exception e) {
            log.error("Multipart upload failed, aborting: {}", e.getMessage());
            abortMultipartUpload(bucket, key, uploadId);
            throw e;
        } finally {
            executor.shutdown();
        }
    }


    private void uploadPartsOptimized(String bucket, String key, String uploadId,
                                      MultipartFile file, List<CompletedPart> completedParts,
                                      ExecutorService executor) throws IOException, InterruptedException, ExecutionException {

        List<Future<?>> futures = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int partNumber = 1;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) > 0) {
                final int currentPart = partNumber++;

                // 메모리 복사 최소화: 정확한 크기만 복사
                final byte[] chunk = (bytesRead == CHUNK_SIZE) ? buffer.clone() : Arrays.copyOf(buffer, bytesRead);

                futures.add(executor.submit(() -> {
                    try {
                        uploadSinglePart(bucket, key, uploadId, currentPart, chunk, completedParts);
                    } catch (Exception e) {
                        log.error("Failed to upload part {}: {}", currentPart, e.getMessage());
                        throw new RuntimeException(e);
                    }
                }));

                // 메모리 압박 방지: 너무 많은 파트가 대기하지 않도록 제어
                if (futures.size() >= MAX_THREADS * 2) {
                    // 일부 완료 대기
                    futures.get(0).get();
                    futures.remove(0);
                }
            }

            // 모든 파트 완료 대기
            for (Future<?> future : futures) {
                future.get();
            }
        }
    }

    private void uploadSinglePart(String bucket, String key, String uploadId, int partNumber,
                                  byte[] data, List<CompletedPart> completedParts) {

        UploadPartRequest uploadRequest = UploadPartRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength((long) data.length)
                .build();

        UploadPartResponse response = s3Client.uploadPart(uploadRequest, RequestBody.fromBytes(data));

        completedParts.add(CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(response.eTag())
                .build());

        log.debug("Successfully uploaded part {} for key {}", partNumber, key);
    }

    // 기존 메서드들...
    private CreateMultipartUploadResponse initiateMultipartUpload(String bucket, String key, String contentType) {
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        return s3Client.createMultipartUpload(request);
    }

    private String completeMultipartUpload(String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
        completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build();

        s3Client.completeMultipartUpload(completeRequest);
        return r2Config.getDomain() + "/" + key;
    }

    private void abortMultipartUpload(String bucket, String key, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build());
    }
}
