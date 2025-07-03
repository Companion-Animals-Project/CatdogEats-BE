package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.storage.domain.Images;

public interface ProductImageDBHelper {
    Images storeImageToDbWithRetry(String imageUrl);
    void storeProductImageMappingWithRetry(Products product, Images image);
}
