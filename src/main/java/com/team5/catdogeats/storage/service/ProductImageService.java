package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.storage.domain.dto.ProductImageUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface ProductImageService {
    List<ProductImageUploadResponseDto> uploadProductImage(UserPrincipal userPrincipal, String productId, List<MultipartFile> images) throws IOException, ExecutionException, InterruptedException;
    void deleteProductImage(String productId, String imageId);
    List<ProductImageUploadResponseDto> updateProductImage(UserPrincipal userPrincipal, String productId, List<String> oldImageIds, List<MultipartFile> images) throws IOException, ExecutionException, InterruptedException;

}
