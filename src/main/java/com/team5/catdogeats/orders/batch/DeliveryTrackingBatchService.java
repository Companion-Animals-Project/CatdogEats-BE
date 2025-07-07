package com.team5.catdogeats.orders.batch;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 배송 추적 배치 서비스
 * 8시간마다 실행되어 배송중인 주문들의 상태를 스마트택배 API로 확인하고 업데이트
 *
 * API 제한 고려사항:
 * - 스마트택배 프리티어: 동일 운송장 일 최대 10건 조회 제한
 * - 8시간 주기 실행 (하루 3회)으로 제한 준수
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingBatchService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryTrackingService deliveryTrackingService; // 수정: 올바른 의존성 주입

    @Value("${delivery.tracking.batch.batch-size:50}")
    private int batchSize;

    @Value("${delivery.tracking.batch.api-call-delay-ms:1000}")
    private long apiCallDelayMs;

    /**
     * 배송 추적 배치 작업 실행
     * 8시간마다 실행 (28800000ms = 8시간)
     * API 제한: 스마트택배 프리티어는 동일 운송장 일 최대 10건 조회 제한
     */
    @Scheduled(fixedRate = 28800000) // 8시간마다 실행
    @Transactional
    public void trackDeliveryStatus() {
        log.info("배송 추적 배치 작업 시작 - 실행 시간: {}", ZonedDateTime.now());

        long startTime = System.currentTimeMillis();

        try {
            // 배송중인 주문 목록 조회
            List<Shipments> inDeliveryShipments = shipmentRepository
                    .findByOrderStatusAndTrackingNumberIsNotNull(OrderStatus.IN_DELIVERY);

            if (inDeliveryShipments.isEmpty()) {
                log.info("배송 추적할 주문이 없습니다");
                return;
            }

            log.info("배송 추적 대상: {}건", inDeliveryShipments.size());

            int processedCount = 0;
            int deliveredCount = 0;
            int errorCount = 0;

            // 배치 크기만큼 나누어 처리
            for (int i = 0; i < inDeliveryShipments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, inDeliveryShipments.size());
                List<Shipments> batchShipments = inDeliveryShipments.subList(i, endIndex);

                for (Shipments shipment : batchShipments) {
                    try {
                        // 개별 배송 상태 확인
                        boolean isDelivered = checkAndUpdateDeliveryStatus(shipment);

                        if (isDelivered) {
                            deliveredCount++;
                        }
                        processedCount++;

                        // API 호출 간 지연 (API 부하 방지)
                        if (apiCallDelayMs > 0) {
                            TimeUnit.MILLISECONDS.sleep(apiCallDelayMs);
                        }

                    } catch (Exception e) {
                        log.error("배송 상태 확인 실패 - shipmentId: {}, trackingNumber: {}, error: {}",
                                shipment.getId(), shipment.getTrackingNumber(), e.getMessage());
                        errorCount++;
                    }
                }

                log.debug("배치 처리 진행률: {}/{}", Math.min(endIndex, inDeliveryShipments.size()),
                        inDeliveryShipments.size());
            }

            long endTime = System.currentTimeMillis();
            long executionTimeSeconds = (endTime - startTime) / 1000;

            log.info("배송 추적 배치 작업 완료 - 처리: {}건, 배송완료: {}건, 오류: {}건, 소요시간: {}초",
                    processedCount, deliveredCount, errorCount, executionTimeSeconds);

        } catch (Exception e) {
            log.error("배송 추적 배치 작업 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 개별 배송 상태 확인 및 업데이트
     * @param shipment 배송 정보
     * @return 배송 완료 여부
     */
    private boolean checkAndUpdateDeliveryStatus(Shipments shipment) {
        try {
            log.debug("배송 상태 확인 - shipmentId: {}, trackingNumber: {}",
                    shipment.getId(), shipment.getTrackingNumber());

            // DeliveryTrackingService를 통해 배송 상태 조회
            boolean isDelivered = deliveryTrackingService.checkDeliveryStatus(
                    shipment.getCourier(),
                    shipment.getTrackingNumber()
            );

            if (isDelivered) {
                // 주문 상태를 배송 완료로 변경
                Orders order = shipment.getOrders();
                order.setOrderStatus(OrderStatus.DELIVERED);
                shipment.setDeliveredAt(ZonedDateTime.now());

                // 저장
                orderRepository.save(order);
                shipmentRepository.save(shipment);

                log.info("배송 완료 처리 - orderNumber: {}, trackingNumber: {}",
                        order.getOrderNumber(), shipment.getTrackingNumber());

                return true;
            } else {
                log.debug("배송 진행 중 - trackingNumber: {}", shipment.getTrackingNumber());
                return false;
            }

        } catch (DeliveryTrackingService.DeliveryTrackingApiException e) {
            // API 제한이나 일시적 오류는 경고로 처리 (배치 중단하지 않음)
            log.warn("배송 상태 확인 API 오류 - shipmentId: {}, trackingNumber: {}, reason: {}",
                    shipment.getId(), shipment.getTrackingNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("배송 상태 확인 중 예상치 못한 오류 - shipmentId: {}, trackingNumber: {}",
                    shipment.getId(), shipment.getTrackingNumber(), e);
            throw e;
        }
    }

    /**
     * 배치 작업 통계 조회
     * @return 배치 작업 현황
     */
    public BatchStatusInfo getBatchStatus() {
        long inDeliveryCount = shipmentRepository.countByOrderStatus(OrderStatus.IN_DELIVERY);
        long deliveredTodayCount = shipmentRepository.countDeliveredToday();

        return BatchStatusInfo.builder()
                .inDeliveryCount(inDeliveryCount)
                .deliveredTodayCount(deliveredTodayCount)
                .nextExecutionTime(getNextExecutionTime())
                .lastExecutionTime(getLastExecutionTime())
                .build();
    }

    /**
     * API 호출 현황 조회
     * @return API 호출 현황
     */
    public DeliveryTrackingService.ApiCallStatus getApiCallStatus() {
        return deliveryTrackingService.getApiCallStatus();
    }

    /**
     * 다음 실행 시간 계산
     */
    private ZonedDateTime getNextExecutionTime() {
        // 현재 시간에서 8시간 후
        return ZonedDateTime.now().plusHours(8);
    }

    /**
     * 마지막 실행 시간 조회 (추정)
     */
    private ZonedDateTime getLastExecutionTime() {
        // TODO: 실제 구현에서는 Redis나 DB에서 관리하거나 스케줄러 메타데이터 활용
        return ZonedDateTime.now().minusHours(8);
    }

    /**
     * 배치 작업 상태 정보
     */
    @lombok.Builder
    public record BatchStatusInfo(
            long inDeliveryCount,           // 배송 중인 주문 수
            long deliveredTodayCount,       // 오늘 배송 완료된 주문 수
            ZonedDateTime nextExecutionTime,    // 다음 실행 시간
            ZonedDateTime lastExecutionTime     // 마지막 실행 시간 (추정)
    ) {}
}