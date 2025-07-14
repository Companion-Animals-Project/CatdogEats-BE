package com.team5.catdogeats.products.domain.enums;

/**
 * 재고 예약 상태 열거형
 * 재고 예약 시스템에서 사용되는 예약 상태를 정의합니다.
 * RabbitMQ를 통한 예약 만료 처리와 결제 상태에 따른 상태 전환을 지원합니다.
 */
public enum ReservationStatus {

    /**
     * 예약됨 - 초기 상태
     * 주문 생성 시 재고가 예약된 상태
     */
    RESERVED,

    /**
     * 확정됨 - 결제 성공 시
     * 결제가 완료되어 재고가 확정 차감된 상태
     */
    CONFIRMED,

    /**
     * 취소됨 - 수동 취소
     * 사용자가 결제 전에 주문을 취소한 상태
     */
    CANCELLED,

}