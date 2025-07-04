package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Settlements;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.enums.SettlementStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.repository.OrdersRepository;
import com.team5.catdogeats.orders.repository.SettlementsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 정산 배치 처리 서비스
 * 정산 데이터 생성, 갱신 및 상태 변경을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private final OrdersRepository ordersRepository;
    private final SettlementsRepository settlementsRepository;

    // 기본 수수료율 (5%) - 추후 설정 파일이나 DB에서 관리 가능
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("5.00");

    /**
     * 정산 데이터 생성/갱신 메인 메서드
     * 배송완료된 주문들에 대해 정산 데이터를 생성하거나 상태를 갱신
     */
    @Transactional
    public void processSettlements() {
        log.info("정산 데이터 생성/갱신 배치 작업 시작");

        try {
            // 1. 배송완료된 주문들 조회
            List<Orders> deliveredOrders = getDeliveredOrders();
            log.info("배송완료된 주문 {}건 조회", deliveredOrders.size());

            int createdCount = 0;
            int updatedCount = 0;

            // 2. 각 주문에 대해 정산 처리
            for (Orders order : deliveredOrders) {
                for (OrderItems orderItem : order.getOrderItems()) {
                    SettlementProcessResult result = processOrderItemSettlement(orderItem);

                    if (result == SettlementProcessResult.CREATED) {
                        createdCount++;
                    } else if (result == SettlementProcessResult.UPDATED) {
                        updatedCount++;
                    }
                }
            }

            log.info("정산 데이터 처리 완료 - 생성: {}건, 갱신: {}건", createdCount, updatedCount);

        } catch (Exception e) {
            log.error("정산 데이터 생성/갱신 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 처리중 상태의 정산을 완료 상태로 변경
     * 매월 1일 실행
     */
    @Transactional
    public void completeInProgressSettlements() {
        log.info("처리중 정산 완료 처리 시작");

        try {
            List<Settlements> inProgressSettlements = settlementsRepository
                    .findBySettlementStatus(SettlementStatus.IN_PROGRESS);

            log.info("처리중 정산 {}건 조회", inProgressSettlements.size());

            for (Settlements settlement : inProgressSettlements) {
                settlement.updateStatus(SettlementStatus.COMPLETED);
                settlement.setSettledAt(ZonedDateTime.now());
            }

            settlementsRepository.saveAll(inProgressSettlements);
            log.info("처리중 정산 {}건을 완료 상태로 변경", inProgressSettlements.size());

        } catch (Exception e) {
            log.error("처리중 정산 완료 처리 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 배송완료된 주문들 조회
     */
    private List<Orders> getDeliveredOrders() {
        return ordersRepository.findByOrderStatusAndIsHidden(
                OrderStatus.DELIVERED, false);
    }

    /**
     * 개별 주문 상품에 대한 정산 처리
     */
    private SettlementProcessResult processOrderItemSettlement(OrderItems orderItem) {
        try {
            // 기존 정산 데이터 확인
            Optional<Settlements> existingSettlement = settlementsRepository
                    .findByOrderItems(orderItem);

            if (existingSettlement.isPresent()) {
                // 기존 정산 데이터 상태 갱신
                return updateExistingSettlement(existingSettlement.get(), orderItem);
            } else {
                // 새로운 정산 데이터 생성
                return createNewSettlement(orderItem);
            }

        } catch (Exception e) {
            log.error("주문 상품 ID: {} 정산 처리 중 오류", orderItem.getId(), e);
            return SettlementProcessResult.ERROR;
        }
    }

    /**
     * 새로운 정산 데이터 생성
     */
    private SettlementProcessResult createNewSettlement(OrderItems orderItem) {
        try {
            // 정산 상태 계산
            SettlementStatus status = calculateSettlementStatus(orderItem);

            // 정산 금액 계산
            Long itemPrice = orderItem.getPrice() * orderItem.getQuantity();
            Long commissionAmount = calculateCommission(itemPrice, DEFAULT_COMMISSION_RATE);
            Long settlementAmount = itemPrice - commissionAmount;

            // 정산 데이터 생성
            Settlements settlement = Settlements.builder()
                    .seller(orderItem.getProducts().getSeller())
                    .orderItems(orderItem)
                    .itemPrice(itemPrice)
                    .commissionRate(DEFAULT_COMMISSION_RATE)
                    .commissionAmount(commissionAmount)
                    .settlementAmount(settlementAmount)
                    .settlementStatus(status)
                    .build();

            // 완료 상태인 경우 정산 완료 시각 설정
            if (status == SettlementStatus.COMPLETED) {
                settlement.setSettledAt(ZonedDateTime.now());
            }

            settlementsRepository.save(settlement);

            log.debug("새 정산 데이터 생성 - 주문상품 ID: {}, 상태: {}, 정산금액: {}",
                    orderItem.getId(), status, settlementAmount);

            return SettlementProcessResult.CREATED;

        } catch (Exception e) {
            log.error("정산 데이터 생성 실패 - 주문상품 ID: {}", orderItem.getId(), e);
            return SettlementProcessResult.ERROR;
        }
    }

    /**
     * 기존 정산 데이터 상태 갱신
     */
    private SettlementProcessResult updateExistingSettlement(Settlements settlement, OrderItems orderItem) {
        try {
            SettlementStatus currentStatus = settlement.getSettlementStatus();
            SettlementStatus newStatus = calculateSettlementStatus(orderItem);

            // 상태 변경이 필요한 경우만 갱신
            if (currentStatus != newStatus) {
                settlement.updateStatus(newStatus);

                // 완료 상태로 변경되는 경우 정산 완료 시각 설정
                if (newStatus == SettlementStatus.COMPLETED && settlement.getSettledAt() == null) {
                    settlement.setSettledAt(ZonedDateTime.now());
                }

                settlementsRepository.save(settlement);

                log.debug("정산 상태 갱신 - 정산 ID: {}, {} -> {}",
                        settlement.getId(), currentStatus, newStatus);

                return SettlementProcessResult.UPDATED;
            }

            return SettlementProcessResult.NO_CHANGE;

        } catch (Exception e) {
            log.error("정산 데이터 갱신 실패 - 정산 ID: {}", settlement.getId(), e);
            return SettlementProcessResult.ERROR;
        }
    }

    /**
     * 정산 상태 계산
     * - 배송완료 7일 이내: PENDING (대기중)
     * - 배송완료 7일 경과: IN_PROGRESS (처리중)
     */
    private SettlementStatus calculateSettlementStatus(OrderItems orderItem) {
        Orders order = orderItem.getOrders();
        Shipments shipment = order.getShipment();

        if (shipment == null || shipment.getDeliveredAt() == null) {
            return SettlementStatus.PENDING;
        }

        ZonedDateTime deliveredAt = shipment.getDeliveredAt();
        ZonedDateTime now = ZonedDateTime.now();

        // 배송완료 후 7일 경과 여부 확인
        if (deliveredAt.plusDays(7).isBefore(now)) {
            return SettlementStatus.IN_PROGRESS;
        } else {
            return SettlementStatus.PENDING;
        }
    }

    /**
     * 수수료 계산
     */
    private Long calculateCommission(Long itemPrice, BigDecimal commissionRate) {
        BigDecimal itemPriceBigDecimal = BigDecimal.valueOf(itemPrice);
        BigDecimal commission = itemPriceBigDecimal
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 0, BigDecimal.ROUND_HALF_UP);

        return commission.longValue();
    }

    /**
     * 정산 처리 결과 열거형
     */
    private enum SettlementProcessResult {
        CREATED,    // 새로 생성됨
        UPDATED,    // 기존 데이터 갱신됨
        NO_CHANGE,  // 변경 없음
        ERROR       // 오류 발생
    }
}