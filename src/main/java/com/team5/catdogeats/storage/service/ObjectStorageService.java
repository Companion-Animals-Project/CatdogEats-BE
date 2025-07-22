package com.team5.catdogeats.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface ObjectStorageService {
    String uploadImage(String key, MultipartFile file) throws IOException, ExecutionException, InterruptedException;
    String uploadFile(String key, MultipartFile file) throws IOException, ExecutionException, InterruptedException;
    void deleteFile(String key);
    void deleteImage(String key);
}
