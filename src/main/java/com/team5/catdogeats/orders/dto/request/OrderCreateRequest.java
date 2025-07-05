package com.team5.catdogeats.orders.dto.request;

import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO (쿠폰 할인 적용)
 * API: POST /v1/buyers/orders
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrderCreateRequest {

    /**
     * 주문할 상품들의 목록
     */
    @NotEmpty(message = "주문 상품 목록은 비어있을 수 없습니다")
    @Valid
    private List<OrderItemRequest> orderItems;

    /**
     * 배송 주소 정보
     */
    @NotNull(message = "배송 주소 정보는 필수입니다")
    @Valid
    private ShippingAddressRequest shippingAddress;

    /**
     * 결제 관련 정보
     */
    @NotNull(message = "결제 정보는 필수입니다")
    @Valid
    private PaymentInfoRequest paymentInfo;

    /**
     * 주문 요청 아이템 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class OrderItemRequest {

        /**
         * 상품 ID
         */
        @NotBlank(message = "상품 ID는 필수입니다")
        private String productId;

        /**
         * 주문 수량
         */
        @NotNull(message = "주문 수량은 필수입니다")
        private Integer quantity;
    }

    /**
     * 배송 주소 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddressRequest {

        /**
         * 받는 사람 이름
         */
        @NotBlank(message = "받는 사람 이름은 필수입니다")
        private String recipientName;

        /**
         * 받는 사람 연락처
         */
        @NotBlank(message = "받는 사람 연락처는 필수입니다")
        private String recipientPhone;

        /**
         * 우편번호
         */
        @NotBlank(message = "우편번호는 필수입니다")
        private String postalCode;

        /**
         * 기본 주소
         */
        @NotBlank(message = "기본 주소는 필수입니다")
        private String streetAddress;

        /**
         * 상세 주소
         */
        private String detailAddress;

        /**
         * 배송 요청사항
         */
        private String deliveryNote;
    }

    /**
     * 결제 정보 (쿠폰 할인 적용)
     * 토스 페이먼츠 연동을 위한 정보 포함
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class PaymentInfoRequest {

        /**
         * 주문명 (토스 페이먼츠에 표시될 상품명)
         * 예: "강아지 사료 외 2건"
         */
        @NotBlank(message = "주문명은 필수입니다")
        private String orderName;

        // ===== 쿠폰 할인 정보 확장 =====

        /**
         * 쿠폰 할인 타입 (선택사항)
         * PERCENT: 정률 할인 (%), AMOUNT: 정액 할인 (원)
         * null인 경우: 기존 방식으로 couponDiscountRate 사용 (하위 호환성)
         */
        private DiscountType couponType;

        /**
         * 쿠폰 할인률 (%) - couponType이 PERCENT이거나 null일 때 사용
         * 0~100 사이의 값, null이면 할인 없음
         * 예: 10.0 = 10% 할인
         */
        @DecimalMin(value = "0.0", message = "쿠폰 할인률은 0% 이상이어야 합니다")
        @DecimalMax(value = "100.0", message = "쿠폰 할인률은 100% 이하여야 합니다")
        private Double couponDiscountRate;

        /**
         * 쿠폰 할인 금액 (원) - couponType이 AMOUNT일 때 사용
         * 0원 이상의 값, null이면 할인 없음
         * 예: 5000L = 5,000원 할인
         */
        private Long couponDiscountAmount;


        /**
         * 구매자 이메일 (토스 페이먼츠 결제창에 미리 입력)
         */
        private String customerEmail;

        /**
         * 구매자 이름 (토스 페이먼츠 결제창에 미리 입력)
         */
        private String customerName;

        /**
         * 성공 시 리디렉션 URL (선택사항 - 기본값 사용 가능)
         */
        private String successUrl;

        /**
         * 실패 시 리디렉션 URL (선택사항 - 기본값 사용 가능)
         */
        private String failUrl;

        // ===== 쿠폰 검증 메서드 =====

        /**
         * 쿠폰 할인이 적용되었는지 확인
         */
        public boolean isCouponApplied() {
            if (couponType == null) {
                // 기존 방식 (하위 호환성)
                return couponDiscountRate != null && couponDiscountRate > 0;
            }

            return switch (couponType) {
                case PERCENT -> couponDiscountRate != null && couponDiscountRate > 0;
                case AMOUNT -> couponDiscountAmount != null && couponDiscountAmount > 0;
            };
        }

        /**
         * 쿠폰 설정의 일관성 검증
         * @throws IllegalArgumentException 일관성에 문제가 있을 경우
         */
        public void validateCouponConsistency() {
            if (couponType == null) {
                // 기존 방식 - couponDiscountRate만 허용
                if (couponDiscountAmount != null) {
                    throw new IllegalArgumentException("쿠폰 타입이 지정되지 않은 경우 정액 할인 금액을 설정할 수 없습니다.");
                }
                return;
            }

            switch (couponType) {
                case PERCENT:
                    if (couponDiscountRate == null || couponDiscountRate <= 0) {
                        throw new IllegalArgumentException("정률 할인 쿠폰은 할인률이 필수입니다.");
                    }
                    if (couponDiscountAmount != null) {
                        throw new IllegalArgumentException("정률 할인 쿠폰에는 할인 금액을 설정할 수 없습니다.");
                    }
                    break;

                case AMOUNT:
                    if (couponDiscountAmount == null || couponDiscountAmount <= 0) {
                        throw new IllegalArgumentException("정액 할인 쿠폰은 할인 금액이 필수입니다.");
                    }
                    if (couponDiscountRate != null) {
                        throw new IllegalArgumentException("정액 할인 쿠폰에는 할인률을 설정할 수 없습니다.");
                    }
                    break;
            }
        }
    }
}