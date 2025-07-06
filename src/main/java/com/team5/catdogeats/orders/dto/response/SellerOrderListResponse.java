package com.team5.catdogeats.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 판매자용 주문 목록 조회 응답 DTO
 * 판매자가 본인이 판매한 상품이 포함된 주문 목록을 페이징으로 조회할 수 있도록 설계된 응답 구조
 * 구매자의 민감정보는 제외하고 배송 관리에 필요한 정보만 포함
 */
@Builder
public record SellerOrderListResponse(
        List<SellerOrderSummary> orders,
        PageInfo pageInfo
) {

    /**
     * 판매자용 주문 요약 정보
     * 목록에서 표시할 주요 정보만 포함
     */
    @Builder
    public record SellerOrderSummary(
            String orderNumber,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            ZonedDateTime orderDate,

            OrderStatus orderStatus,
            String recipientName,        // 수령인 이름
            String recipientPhone,       // 수령인 연락처 (뒷자리 마스킹 처리됨)
            String shippingAddress,      // 배송지 (상세주소 제외)
            List<OrderProductSummary> products,  // 해당 판매자의 상품 목록
            Long totalAmount,            // 해당 판매자 상품들의 총 금액
            String trackingNumber,       // 운송장 번호 (있는 경우)
            String courierCompany,       // 택배사 (있는 경우)
            boolean isHiddenBySeller     // 판매자가 목록에서 숨김 처리했는지 여부
    ) {}

    /**
     * 주문 상품 요약 정보
     * 목록에서 표시할 상품 정보
     */
    @Builder
    public record OrderProductSummary(
            String productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long itemTotalPrice
    ) {}

    /**
     * 페이징 정보
     */
    @Builder
    public record PageInfo(
            int currentPage,        // 현재 페이지 (0-based)
            int totalPages,         // 전체 페이지 수
            long totalElements,     // 전체 요소 수
            int pageSize,           // 페이지 크기
            boolean hasNext,        // 다음 페이지 존재 여부
            boolean hasPrevious,    // 이전 페이지 존재 여부
            boolean isFirst,        // 첫 번째 페이지 여부
            boolean isLast          // 마지막 페이지 여부
    ) {
        /**
         * Spring Data Page 객체로부터 PageInfo 생성
         */
        public static PageInfo from(Page<?> page) {
            return PageInfo.builder()
                    .currentPage(page.getNumber())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .pageSize(page.getSize())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .isFirst(page.isFirst())
                    .isLast(page.isLast())
                    .build();
        }
    }

    /**
     * 성공 응답 생성
     * @param orders 판매자 주문 요약 목록
     * @param pageInfo 페이징 정보
     * @return 판매자용 주문 목록 응답 DTO
     */
    public static SellerOrderListResponse success(
            List<SellerOrderSummary> orders,
            PageInfo pageInfo
    ) {
        return SellerOrderListResponse.builder()
                .orders(orders)
                .pageInfo(pageInfo)
                .build();
    }

    /**
     * Page 객체로부터 응답 생성
     * @param orders 주문 요약 목록
     * @param page Spring Data Page 객체
     * @return 판매자용 주문 목록 응답 DTO
     */
    public static SellerOrderListResponse from(
            List<SellerOrderSummary> orders,
            Page<?> page
    ) {
        return SellerOrderListResponse.builder()
                .orders(orders)
                .pageInfo(PageInfo.from(page))
                .build();
    }

    /**
     * 전화번호 마스킹 처리 유틸리티 메서드
     * 예: 010-1234-5678 → 010-1234-****
     * @param phoneNumber 원본 전화번호
     * @return 마스킹 처리된 전화번호
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }

        // 하이픈이 있는 경우: 010-1234-5678 → 010-1234-****
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length >= 3) {
                return parts[0] + "-" + parts[1] + "-****";
            }
        }

        // 하이픈이 없는 경우: 01012345678 → 0101234****
        if (phoneNumber.length() >= 8) {
            return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
        }

        return phoneNumber;
    }
}