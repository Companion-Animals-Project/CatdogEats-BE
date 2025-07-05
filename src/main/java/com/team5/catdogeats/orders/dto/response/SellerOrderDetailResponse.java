package com.team5.catdogeats.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 판매자용 주문 상세 조회 응답 DTO
 * 판매자가 배송지 정보와 해당 판매자의 상품 정보만 조회할 수 있도록 설계된 응답 구조
 * 구매자의 민감정보(결제정보 등)는 제외하고 배송에 필요한 정보만 포함
 */
@Builder
public record SellerOrderDetailResponse(
        String orderNumber,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        ZonedDateTime orderDate,

        OrderStatus orderStatus,
        RecipientInfo recipientInfo,
        List<SellerOrderItem> orderItems,
        Long totalAmount
) {

    /**
     * 수령인 정보 (배송지 정보)
     * 판매자가 상품 발송을 위해 필요한 배송지 관련 정보를 포함
     */
    @Builder
    public record RecipientInfo(
            String recipientName,     // 수령인 이름
            String recipientPhone,    // 수령인 연락처
            String postalCode,        // 우편번호
            String streetAddress,     // 도로명주소
            String detailAddress,     // 상세주소
            String deliveryNote       // 배송 요청사항
    ) {}

    /**
     * 판매자 소유 주문 상품 정보
     * 해당 판매자가 판매한 상품만 필터링된 주문 아이템 정보
     */
    @Builder
    public record SellerOrderItem(
            String productId,         // 상품 ID
            String productName,       // 상품명
            Long unitPrice,           // 단가
            Integer quantity,         // 수량
            Long itemTotalPrice       // 상품별 총 금액 (단가 * 수량)
    ) {}

    /**
     * 성공 응답 생성
     * @param orderNumber 주문번호
     * @param orderDate 주문일시
     * @param orderStatus 주문상태
     * @param recipientInfo 수령인 정보
     * @param orderItems 판매자 소유 상품 목록
     * @param totalAmount 해당 판매자 상품들의 총 금액
     * @return 판매자용 주문 상세 응답 DTO
     */
    public static SellerOrderDetailResponse success(
            String orderNumber,
            ZonedDateTime orderDate,
            OrderStatus orderStatus,
            RecipientInfo recipientInfo,
            List<SellerOrderItem> orderItems,
            Long totalAmount
    ) {
        return SellerOrderDetailResponse.builder()
                .orderNumber(orderNumber)
                .orderDate(orderDate)
                .orderStatus(orderStatus)
                .recipientInfo(recipientInfo)
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .build();
    }
}