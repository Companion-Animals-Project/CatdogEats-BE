package com.team5.catdogeats.storage.controller;

import com.team5.catdogeats.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/users")
public class UploadTestController {

    private final ObjectStorageService objectStorageService;

    // 이미지 업로드
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadImage(@RequestPart List<MultipartFile> file) {
        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile multipartFile : file) {
                urls.add((objectStorageService.uploadImage(
                        multipartFile.getOriginalFilename(),
                        multipartFile
                )));
            }
            return ResponseEntity.ok(urls);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 파일 업로드
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart MultipartFile file) {
        try {
            String url = objectStorageService.uploadFile(
                    file.getOriginalFilename(),
                    file
            );
            return ResponseEntity.ok(url);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 실패");
        }
    }

    @DeleteMapping(value = "/delete/file")
    public ResponseEntity<Void> deleteFile(@RequestParam String key) {
        objectStorageService.deleteFile(key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/image")
    public ResponseEntity<Void> deleteImage(@RequestParam String key) {
        objectStorageService.deleteImage(key);
        return ResponseEntity.noContent().build();
    }

}

