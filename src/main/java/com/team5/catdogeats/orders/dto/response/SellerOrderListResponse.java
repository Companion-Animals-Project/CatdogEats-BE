package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 판매자용 주문 목록 응답 DTO
 * API: GET /v1/sellers/orders/list
 *
 * 판매자가 본인이 판매한 상품이 포함된 주문 목록을 조회하는 응답 구조
 */
@Builder
public record SellerOrderListResponse(
        List<SellerOrderSummary> orders,    // 주문 목록
        int currentPage,                    // 현재 페이지 (0부터 시작)
        int totalPages,                     // 전체 페이지 수
        long totalElements,                 // 전체 주문 수
        int pageSize,                       // 페이지 크기
        boolean hasNext,                    // 다음 페이지 존재 여부
        boolean hasPrevious,                // 이전 페이지 존재 여부
        String searchType,                  // 검색 타입 (선택)
        String searchKeyword,               // 검색 키워드 (선택)
        OrderStatus filterStatus            // 필터링 상태 (선택)
) {

    /**
     * 판매자용 주문 요약 정보
     */
    @Builder
    public record SellerOrderSummary(
            String orderNumber,             // 주문 번호
            OrderStatus orderStatus,        // 주문 상태
            ZonedDateTime orderDate,        // 주문일시

            // 배송지 정보 (마스킹 처리됨)
            String recipientName,           // 수령인명
            String maskedPhone,             // 마스킹된 전화번호
            String basicAddress,            // 기본 주소 (상세주소 제외)

            // 주문 상품 정보 (해당 판매자 상품만)
            List<SellerOrderItem> orderItems, // 판매자 상품 목록
            Long totalPrice,                // 해당 판매자 상품 총액
            int itemCount,                  // 상품 종류 수

            // 배송 정보
            String courier,                 // 택배사
            String trackingNumber,          // 운송장 번호
            ZonedDateTime shippedAt,        // 발송일시
            ZonedDateTime deliveredAt,      // 배송완료일시

            // 관리 정보
            boolean canChangeStatus,        // 상태 변경 가능 여부
            boolean requiresTracking,       // 운송장 등록 필요 여부
            String nextAction              // 다음 수행 작업 안내
    ) {

        /**
         * 배송 상태 확인
         * @return 배송 상태 요약
         */
        public String getShipmentStatus() {
            if (deliveredAt != null) {
                return "배송완료";
            } else if (shippedAt != null && trackingNumber != null) {
                return "배송중";
            } else if (orderStatus == OrderStatus.READY_FOR_SHIPMENT) {
                return "배송준비완료";
            } else if (orderStatus == OrderStatus.PREPARING) {
                return "상품준비중";
            } else {
                return "주문확인";
            }
        }

        /**
         * 상태 변경 가능 여부 확인
         * @return 상태 변경 가능 여부
         */
        public boolean isStatusChangeable() {
            return canChangeStatus &&
                   orderStatus != OrderStatus.DELIVERED &&
                   orderStatus != OrderStatus.CANCELLED;
        }

        /**
         * 운송장 등록 가능 여부 확인
         * @return 운송장 등록 가능 여부
         */
        public boolean canRegisterTracking() {
            return requiresTracking &&
                   orderStatus == OrderStatus.READY_FOR_SHIPMENT &&
                   (trackingNumber == null || trackingNumber.trim().isEmpty());
        }
    }

    /**
     * 판매자 주문 상품 정보
     */
    @Builder
    public record SellerOrderItem(
            String productId,               // 상품 ID
            String productName,             // 상품명
            String productImageUrl,         // 상품 이미지 URL
            Long unitPrice,                 // 단가
            int quantity,                   // 수량
            Long totalPrice,                // 총 가격 (단가 × 수량)
            String productOptions          // 상품 옵션 정보 (선택)
    ) {}

    /**
     * 빈 응답 생성
     * @return 빈 주문 목록 응답
     */
    public static SellerOrderListResponse empty() {
        return SellerOrderListResponse.builder()
                .orders(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0L)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .searchType(null)
                .searchKeyword(null)
                .filterStatus(null)
                .build();
    }

    /**
     * 기본 응답 생성
     * @param orders 주문 목록
     * @param currentPage 현재 페이지
     * @param totalPages 전체 페이지 수
     * @param totalElements 전체 요소 수
     * @param pageSize 페이지 크기
     * @param hasNext 다음 페이지 존재 여부
     * @param hasPrevious 이전 페이지 존재 여부
     * @return 주문 목록 응답
     */
    public static SellerOrderListResponse of(
            List<SellerOrderSummary> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            boolean hasNext,
            boolean hasPrevious
    ) {
        return SellerOrderListResponse.builder()
                .orders(orders)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .pageSize(pageSize)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .searchType(null)
                .searchKeyword(null)
                .filterStatus(null)
                .build();
    }

    /**
     * 검색 결과 응답 생성
     * @param orders 주문 목록
     * @param currentPage 현재 페이지
     * @param totalPages 전체 페이지 수
     * @param totalElements 전체 요소 수
     * @param pageSize 페이지 크기
     * @param hasNext 다음 페이지 존재 여부
     * @param hasPrevious 이전 페이지 존재 여부
     * @param searchType 검색 타입
     * @param searchKeyword 검색 키워드
     * @return 검색 결과 응답
     */
    public static SellerOrderListResponse searchResult(
            List<SellerOrderSummary> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            boolean hasNext,
            boolean hasPrevious,
            String searchType,
            String searchKeyword
    ) {
        return SellerOrderListResponse.builder()
                .orders(orders)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .pageSize(pageSize)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .searchType(searchType)
                .searchKeyword(searchKeyword)
                .filterStatus(null)
                .build();
    }

    /**
     * 필터링 결과 응답 생성
     * @param orders 주문 목록
     * @param currentPage 현재 페이지
     * @param totalPages 전체 페이지 수
     * @param totalElements 전체 요소 수
     * @param pageSize 페이지 크기
     * @param hasNext 다음 페이지 존재 여부
     * @param hasPrevious 이전 페이지 존재 여부
     * @param filterStatus 필터링 상태
     * @return 필터링 결과 응답
     */
    public static SellerOrderListResponse filteredResult(
            List<SellerOrderSummary> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            boolean hasNext,
            boolean hasPrevious,
            OrderStatus filterStatus
    ) {
        return SellerOrderListResponse.builder()
                .orders(orders)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .pageSize(pageSize)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .searchType(null)
                .searchKeyword(null)
                .filterStatus(filterStatus)
                .build();
    }

    /**
     * 빈 목록인지 확인
     * @return 빈 목록 여부
     */
    public boolean isEmpty() {
        return orders == null || orders.isEmpty();
    }

    /**
     * 검색 결과인지 확인
     * @return 검색 결과 여부
     */
    public boolean isSearchResult() {
        return searchType != null && searchKeyword != null;
    }

    /**
     * 필터링된 결과인지 확인
     * @return 필터링 결과 여부
     */
    public boolean isFilteredResult() {
        return filterStatus != null;
    }

    /**
     * 총 주문 금액 계산
     * @return 전체 주문 금액
     */
    public Long getTotalOrderAmount() {
        if (orders == null || orders.isEmpty()) {
            return 0L;
        }

        return orders.stream()
                .mapToLong(SellerOrderSummary::totalPrice)
                .sum();
    }

    /**
     * 상태별 주문 수 집계
     * @param status 집계할 상태
     * @return 해당 상태의 주문 수
     */
    public long countByStatus(OrderStatus status) {
        if (orders == null || orders.isEmpty()) {
            return 0L;
        }

        return orders.stream()
                .filter(order -> order.orderStatus() == status)
                .count();
    }
}