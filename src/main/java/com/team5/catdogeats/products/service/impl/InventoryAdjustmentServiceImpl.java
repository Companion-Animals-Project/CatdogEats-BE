package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.AdjustmentRequestDTO;
import com.team5.catdogeats.products.domain.dto.InventoryAdjustmentProjection;
import com.team5.catdogeats.products.domain.dto.ProductInventoryProjection;
import com.team5.catdogeats.products.domain.enums.AdjustmentType;
import com.team5.catdogeats.products.repository.InventoryAdjustmentRepository;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.InventoryAdjustmentService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAdjustmentServiceImpl implements InventoryAdjustmentService {
    private final ProductRepository productRepository;
    private final SellersRepository sellersRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final EntityManager entityManager;

    @Override
    @JpaTransactional
    public void updateAdjustment(UserPrincipal userPrincipal, AdjustmentRequestDTO dto) {
      log.debug("재고 조정 관리 db 저장 시작 : productId={}", dto.id());
        try {
            Products products = productRepository.findProductsByIdAndProviderId(dto.id(), userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new IllegalStateException("판매자와 연동된 상품이 아닙니다"));
            int newStock = products.getStock() + (dto.type() == AdjustmentType.OUT ? -dto.quantity() : dto.quantity());
            if (dto.type() == AdjustmentType.IN) {
                productRepository.updateStock(dto.id(), newStock);
            } else if (dto.type() == AdjustmentType.OUT) {
                if (newStock < 0) {
                    throw new IllegalArgumentException("0 이하로 재고조정을 할 수 없습니다.");
                }
                productRepository.updateStock(dto.id(), newStock);
            } else {
                productRepository.updateStock(dto.id(), products.getStock() + dto.quantity());
            }
            entityManager.flush();
            inventoryAdjustmentRepository.save(AdjustmentRequestDTO.toEntity(dto, products, products.getSeller()));
            log.debug("재고 조정 완료 기존 재고: {}, 수정 후 재고: {}", products.getStock(), products.getStock()+dto.quantity());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("재고 조정 중 오류 발생 {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    @JpaTransactional(readOnly = true)
    public Page<InventoryAdjustmentProjection> adjustment(UserPrincipal userPrincipal, int page, int size) {
        try {
            SellerDTO sellerDTO = sellersRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("판매자를 찾을 수 없습니다."));
            Sellers sellers = SellerDTO.toEntity(sellerDTO);
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("products.title").ascending()
            );
            return inventoryAdjustmentRepository.findInventoryAdjustmentsByProducts_Seller(sellers, pageable);
        } catch (Exception e) {
            log.error("재고 현황 조회중 오류발생 {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    @JpaTransactional(readOnly = true)
    public Page<ProductInventoryProjection> productInventoryList(UserPrincipal userPrincipal, int page, int size, String title) {
        try {
            sellersRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("판매자를 찾을 수 없습니다."));
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("title").ascending()
            );
            if (StringUtils.hasText(title)) {
                return productRepository.searchByKeyword(title, pageable);
            } else {
                return productRepository.findProducts(pageable);
            }
        } catch (Exception e) {
            log.error("재고 현황 조회중 오류발생 {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
