package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.config.R2Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2ServiceImplTest {
    @Mock
    S3Client s3Client;

    @Mock
    R2Config r2Config;

    @InjectMocks
    R2ServiceImpl r2Service;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(r2Config.getBucket()).thenReturn("test-bucket");
        when(r2Config.getDomain()).thenReturn("https://cdn.example.com");
    }

    @Test
    @DisplayName("이미지 업로드 성공 - 이미지 폴더에 저장")
    void uploadImage_Success() throws IOException {
        // given
        String key = "cat.png";
        byte[] data = "hello".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        long size = data.length;
        String contentType = "image/png";

        // when
        String url = r2Service.uploadImage(key, inputStream, size, contentType);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).isEqualTo("images/" + key);
        assertThat(request.contentType()).isEqualTo(contentType);

        assertThat(url).isEqualTo("https://cdn.example.com/images/" + key);
    }

    @Test
    @DisplayName("파일 업로드 성공 - files 폴더에 저장")
    void uploadFile_Success() throws IOException {
        // given
        String key = "document.pdf";
        byte[] data = "pdf-content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        long size = data.length;
        String contentType = "application/pdf";

        // when
        String url = r2Service.uploadFile(key, inputStream, size, contentType);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).isEqualTo("files/" + key);
        assertThat(request.contentType()).isEqualTo(contentType);

        assertThat(url).isEqualTo("https://cdn.example.com/files/" + key);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void deleteFile_Success() {
        String key = "document.pdf";

        r2Service.deleteFile(key);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass((Class) Consumer.class);

        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        captor.getValue().accept(builder);
        DeleteObjectRequest request = builder.build();

        assertEquals("test-bucket", request.bucket());
        assertEquals("files/"+key, request.key());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void deleteImage_Success() {
        String key = "document.pdf";

        r2Service.deleteImage(key);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass((Class) Consumer.class);

        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        captor.getValue().accept(builder);
        DeleteObjectRequest request = builder.build();

        assertEquals("test-bucket", request.bucket());
        assertEquals("images/"+key, request.key());
    }

}