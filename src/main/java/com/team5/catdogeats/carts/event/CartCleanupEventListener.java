//package com.team5.catdogeats.carts.event;
//
//import com.team5.catdogeats.carts.service.CartService;
//import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//// 결제 완료 후 구매 상품만 장바구니에서 삭제하는 이벤트 리스너
//// PaymentCompletedEvent를 수신 -> 비동기, 구매상품 장바구니에서 삭제
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class CartCleanupEventListener {
//
//    private final CartService cartService;
//
//    // 결제 완료 이벤트 처리
//    @EventListener
//    @Async("taskExecutor")  // AsyncConfig에서 설정한 기존 스레드풀 사용
//    public void handlePaymentCompleted(PaymentCompletedEvent event) {
//        log.info("결제 완료 이벤트 수신 - 구매 상품들만 장바구니에서 삭제 시작: {}", event);
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            // 입력값 검증
//            if (event.getPurchasedProductIds() == null || event.getPurchasedProductIds().isEmpty()) {
//                log.warn("구매 상품 목록이 비어있음 - paymentId: {}, userId: {}",
//                        event.getPaymentId(), event.getUserId());
//                return;
//            }
//
//            // 🎯 핵심: 구매한 상품들만 선별적으로 삭제
//            cartService.clearPurchasedItemsFromCart(
//                    event.getUserId(),
//                    event.getPurchasedProductIds()
//            );
//
//            long processingTime = System.currentTimeMillis() - startTime;
//            log.info("구매 상품들 장바구니 삭제 완료 - userId: {}, paymentId: {}, 상품수: {}, 처리시간: {}ms",
//                    event.getUserId(), event.getPaymentId(),
//                    event.getPurchasedProductIds().size(), processingTime);
//
//        } catch (Exception e) {
//            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("구매 상품들 장바구니 삭제 실패 - userId: {}, paymentId: {}, 상품수: {}, 처리시간: {}ms, error: {}",
//                    event.getUserId(), event.getPaymentId(),
//                    event.getPurchasedProductIds() != null ? event.getPurchasedProductIds().size() : 0,
//                    processingTime, e.getMessage(), e);
//
//            // 실패 시 별도 처리 (모니터링, 재시도 등)
//            handleCleanupFailure(event, e);
//        }
//    }
//
//    /**
//     * 장바구니 정리 실패 시 처리
//     * 추후 필요에 따라 재시도 로직, 알림 발송 등 구현 가능
//     *
//     * @param event 실패한 이벤트
//     * @param exception 발생한 예외
//     */
//    private void handleCleanupFailure(PaymentCompletedEvent event, Exception exception) {
//        // TODO: 필요시 다음과 같은 처리 추가 가능
//        // 1. 재시도 큐에 추가
//        // 2. 관리자 알림 발송
//        // 3. 모니터링 시스템 연동
//        // 4. 수동 처리를 위한 별도 테이블에 기록
//
//        log.warn("장바구니 정리 실패에 대한 수동 확인 필요 - paymentId: {}, userId: {}, error: {}",
//                event.getPaymentId(), event.getUserId(), exception.getMessage());
//
//        // 현재는 로깅만 수행하고, 실패 정보를 관리자가 확인할 수 있도록 함
//    }
//}