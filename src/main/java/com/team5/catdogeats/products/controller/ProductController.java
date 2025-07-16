package com.team5.catdogeats.products.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.dto.PageResponseDto;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.*;
import com.team5.catdogeats.products.domain.enums.BuyerProductSortType;
import com.team5.catdogeats.products.domain.enums.MainProductSortType;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.service.InventoryAdjustmentService;
import com.team5.catdogeats.products.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "Product", description = "상품 정보 관련 API")
public class ProductController {
    private final ProductService productService;
    private final InventoryAdjustmentService inventoryAdjustmentService;

    @GetMapping("/sellers/products/inventory/record")
    public ResponseEntity<APIResponse<Page<InventoryAdjustmentProjection>>> updateList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(ResponseCode.UNAUTHORIZED));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE));
        }
        try {
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS,inventoryAdjustmentService.adjustment(userPrincipal, page, size)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping("/sellers/products/inventory/record")
    public ResponseEntity<APIResponse<Void>> updateList(
            @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody @Valid AdjustmentRequestDTO dto) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {
            inventoryAdjustmentService.updateAdjustment(userPrincipal, dto);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/sellers/products/inventory/List")
    public ResponseEntity<APIResponse<Page<ProductInventoryProjection>>> list(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(ResponseCode.UNAUTHORIZED));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE));
        }
        try {
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS,inventoryAdjustmentService.productInventoryList(userPrincipal, page, size, title)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(
            summary = "상품 등록",
            description = "판매자가 새로운 상품을 등록합니다. 등록 성공 시 생성된 상품 ID를 Location 헤더로 반환합니다."
    )
    @PostMapping("/sellers/products")
    public ResponseEntity<APIResponse<Void>> registerProduct(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody @Valid @Parameter(description = "등록할 상품 정보", required = true)ProductCreateRequestDto dto) {
        try {
            String productId = productService.registerProduct(userPrincipal, dto);
            return ResponseEntity
                    .created(URI.create("/v1/sellers/products/" + productId))
                    .body(APIResponse.success(ResponseCode.CREATED));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "판매자 상품 목록 조회",
            description = "판매자가 본인의 상품 목록을 조회합니다."
    )
    @GetMapping("/sellers/products")
    public ResponseEntity<APIResponse<Page<SellerProductListProjection>>> getSellerProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(ResponseCode.UNAUTHORIZED));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE));
        }
        try {
            Page<SellerProductListProjection> result = productService.getSellerProductList(userPrincipal, page, size);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, result));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
            log.error("Exception in getSellerProducts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }


    @Operation(
            summary = "상품 수정",
            description = "판매자가 기존 상품 정보를 수정합니다. 수정할 상품 ID와 내용을 요청 바디로 전달합니다."
    )
    @PatchMapping("/sellers/products")
    public ResponseEntity<APIResponse<Void>> updateProduct(@RequestBody @Valid @Parameter(description = "수정할 상품 정보", required = true) ProductUpdateRequestDto dto) {
        log.info("🚩 [PATCH /sellers/products] 요청 도착, dto: {}", dto);
        try {
            productService.updateProduct(dto);
            log.info("✅ [PATCH /sellers/products] 수정 성공, productId: {}", dto.productId());
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            log.error("❌ [PATCH /sellers/products] NoSuchElementException: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [PATCH /sellers/products] Exception: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "상품 삭제",
            description = "판매자가 특정 상품 (상품, 이미지, 매핑 테이블 모두) 삭제합니다. "
    )
    @DeleteMapping("/sellers/products")
    public ResponseEntity<APIResponse<Void>> deleteProduct(@RequestBody @Valid @Parameter(description = "삭제할 상품 id", required = true) ProductDeleteRequestDto dto) {
        log.info("🚩 [DELETE /sellers/products] 요청 도착, dto: {}", dto);
        try {
            productService.deleteProduct(dto);
            log.info("✅ [DELETE /sellers/products] 삭제 성공, productId: {}", dto.productId());
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            log.error("❌ [DELETE /sellers/products] NoSuchElementException: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [DELETE /sellers/products] Exception: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "상품 목록 조회",
            description = "모든 상품을 조회합니다. 이미지, 리뷰 통계 모두 포함 "
    )
    @GetMapping("/buyers/products/list")
    public ResponseEntity<APIResponse<PageResponseDto<ProductListProjection>>> getProducts(
            @RequestParam(required = false) PetCategory petCategory,
            @RequestParam(required = false) ProductCategory productCategory,
            @RequestParam(defaultValue = "CREATED_AT") BuyerProductSortType sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        try{
            Page<ProductListProjection> responses = productService.getProductList(petCategory, productCategory, sortBy, PageRequest.of(page, size));

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, PageResponseDto.from(responses)));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "상품 상세 조회",
            description = "상품 상세정보, 이미지, 판매자 정보, 리뷰 통계까지 모두 조회"
    )
    @GetMapping("/buyers/products/{productNumber}")
    public ResponseEntity<APIResponse<ProductDetailResponseDto>> getProductDetail(
            @PathVariable Long productNumber) {
        try {
            ProductDetailResponseDto response = productService.getProductDetail(productNumber);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "메인페이지 상품 조회",
            description = "신상품, 베스트셀러, 할인상품 상위8개 품목 보여주기"
    )
    @GetMapping("/buyers/products/main")
    public ResponseEntity<APIResponse<List<MainProductResponseDto>>> getMainProducts(
            @RequestParam(defaultValue = "NEW") MainProductSortType filterType
    ) {
        try {
            List<MainProductResponseDto> products = productService.getMainProducts(filterType);

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, products));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }



}
