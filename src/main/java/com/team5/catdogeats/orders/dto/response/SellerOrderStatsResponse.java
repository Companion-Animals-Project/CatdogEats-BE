package com.team5.catdogeats.orders.dto.response;

import lombok.Builder;

/**
 * 판매자 주문 통계 응답 DTO
 * API: GET /v1/sellers/orders/stats
 * 판매자의 주문 현황을 통계로 제공하는 응답 구조
 */
@Builder
public record SellerOrderStatsResponse(
        Long totalOrders,           // 전체 주문 수
        StatusStats statusStats,    // 상태별 주문 통계
        RecentStats recentStats,    // 최근 통계
        String lastUpdatedAt        // 마지막 업데이트 시간
) {

    /**
     * 상태별 주문 통계
     */
    @Builder
    public record StatusStats(
            Long paymentCompleted,    // 결제 완료
            Long preparing,           // 상품 준비중
            Long readyForShipment,    // 배송 준비 완료
            Long inDelivery,          // 배송중
            Long delivered,           // 배송 완료
            Long cancelled,           // 취소됨
            Long refunded            // 환불 완료
    ) {
        /**
         * 처리가 필요한 주문 수 (액션 필요)
         * 결제 완료 + 상품 준비중 + 배송 준비 완료
         */
        public Long getActionRequiredCount() {
            return (paymentCompleted != null ? paymentCompleted : 0L) +
                   (preparing != null ? preparing : 0L) +
                   (readyForShipment != null ? readyForShipment : 0L);
        }

        /**
         * 진행중인 주문 수 (배송중)
         */
        public Long getInProgressCount() {
            return inDelivery != null ? inDelivery : 0L;
        }

        /**
         * 완료된 주문 수 (배송 완료 + 취소 + 환불)
         */
        public Long getCompletedCount() {
            return (delivered != null ? delivered : 0L) +
                   (cancelled != null ? cancelled : 0L) +
                   (refunded != null ? refunded : 0L);
        }
    }

    /**
     * 최근 통계 (지난 7일, 30일)
     */
    @Builder
    public record RecentStats(
            WeeklyStats last7Days,     // 지난 7일 통계
            MonthlyStats last30Days    // 지난 30일 통계
    ) {}

    /**
     * 주간 통계 (지난 7일)
     */
    @Builder
    public record WeeklyStats(
            Long newOrders,           // 신규 주문 수
            Long completedDeliveries, // 배송 완료 수
            Long cancelledOrders      // 취소 주문 수
    ) {}

    /**
     * 월간 통계 (지난 30일)
     */
    @Builder
    public record MonthlyStats(
            Long newOrders,           // 신규 주문 수
            Long completedDeliveries, // 배송 완료 수
            Long cancelledOrders,     // 취소 주문 수
            Long totalRevenue         // 총 매출 (배송 완료 기준)
    ) {}

    /**
     * 성공 응답 생성
     * @param totalOrders 전체 주문 수
     * @param statusStats 상태별 통계
     * @param recentStats 최근 통계
     * @param lastUpdatedAt 마지막 업데이트 시간
     * @return 판매자 주문 통계 응답 DTO
     */
    public static SellerOrderStatsResponse success(
            Long totalOrders,
            StatusStats statusStats,
            RecentStats recentStats,
            String lastUpdatedAt
    ) {
        return SellerOrderStatsResponse.builder()
                .totalOrders(totalOrders)
                .statusStats(statusStats)
                .recentStats(recentStats)
                .lastUpdatedAt(lastUpdatedAt)
                .build();
    }

    /**
     * 빈 통계 응답 생성 (신규 판매자)
     * @return 모든 값이 0인 통계 응답
     */
    public static SellerOrderStatsResponse empty() {
        StatusStats emptyStatusStats = StatusStats.builder()
                .paymentCompleted(0L)
                .preparing(0L)
                .readyForShipment(0L)
                .inDelivery(0L)
                .delivered(0L)
                .cancelled(0L)
                .refunded(0L)
                .build();

        WeeklyStats emptyWeeklyStats = WeeklyStats.builder()
                .newOrders(0L)
                .completedDeliveries(0L)
                .cancelledOrders(0L)
                .build();

        MonthlyStats emptyMonthlyStats = MonthlyStats.builder()
                .newOrders(0L)
                .completedDeliveries(0L)
                .cancelledOrders(0L)
                .totalRevenue(0L)
                .build();

        RecentStats emptyRecentStats = RecentStats.builder()
                .last7Days(emptyWeeklyStats)
                .last30Days(emptyMonthlyStats)
                .build();

        return SellerOrderStatsResponse.builder()
                .totalOrders(0L)
                .statusStats(emptyStatusStats)
                .recentStats(emptyRecentStats)
                .lastUpdatedAt(java.time.ZonedDateTime.now().toString())
                .build();
    }

    /**
     * 판매자에게 중요한 알림 정보 생성
     * @return 알림 메시지 배열
     */
    public String[] getImportantNotifications() {
        if (statusStats == null) {
            return new String[0];
        }

        java.util.List<String> notifications = new java.util.ArrayList<>();

        // 처리 대기 중인 주문 알림
        Long actionRequired = statusStats.getActionRequiredCount();
        if (actionRequired > 0) {
            notifications.add(String.format("처리가 필요한 주문이 %d건 있습니다", actionRequired));
        }

        // 배송 중인 주문 알림
        Long inProgress = statusStats.getInProgressCount();
        if (inProgress > 0) {
            notifications.add(String.format("배송 중인 주문이 %d건 있습니다", inProgress));
        }

        // 배송 준비 완료 상태 알림 (운송장 등록 필요)
        if (statusStats.readyForShipment() != null && statusStats.readyForShipment() > 0) {
            notifications.add(String.format("운송장 등록이 필요한 주문이 %d건 있습니다", statusStats.readyForShipment()));
        }

        return notifications.toArray(new String[0]);
    }

    /**
     * 판매자 대시보드용 요약 정보
     * @return 대시보드 요약 정보
     */
    public DashboardSummary getDashboardSummary() {
        if (statusStats == null) {
            return new DashboardSummary(0L, 0L, 0L, "모든 주문이 처리되었습니다");
        }

        Long actionRequired = statusStats.getActionRequiredCount();
        Long inProgress = statusStats.getInProgressCount();
        Long completed = statusStats.getCompletedCount();

        String primaryMessage;
        if (actionRequired > 0) {
            primaryMessage = String.format("처리 대기: %d건", actionRequired);
        } else if (inProgress > 0) {
            primaryMessage = String.format("배송 중: %d건", inProgress);
        } else {
            primaryMessage = "모든 주문이 처리되었습니다";
        }

        return new DashboardSummary(actionRequired, inProgress, completed, primaryMessage);
    }

    /**
     * 대시보드 요약 정보
     */
    public record DashboardSummary(
            Long actionRequired,    // 처리 필요
            Long inProgress,        // 진행 중
            Long completed,         // 완료됨
            String primaryMessage   // 주요 메시지
    ) {}
}