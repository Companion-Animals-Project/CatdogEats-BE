package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;

import java.util.NoSuchElementException;

public interface OderDeleteService {

    /**
     * 주문 내역 삭제 (구매자) - 논리적 삭제 방식 + 상태별 제한
     * UserPrincipal을 통해 인증된 사용자의 주문만 삭제 가능하도록 보안 강화
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 구매자 권한 확인
     * 2. 주문 존재 여부 및 소유권 검증
     * 3. 주문 상태별 삭제 가능 여부 확인 (상태 제한 로직)
     * 4. 이미 숨겨진 주문인지 확인
     * 5. 논리적 삭제 처리 (is_hidden = true, hidden_at = 현재시각)
     * 6. 성공/실패 결과 반환
     * 삭제 제한 상태:
     * - PAYMENT_COMPLETED: 결제 완료 후 상품 준비 진행상황 확인 필요
     * - PREPARING: 판매자가 상품을 준비 중인 상태
     * - READY_FOR_SHIPMENT: 배송 준비 완료로 곧 배송 시작
     * - IN_DELIVERY: 배송 중으로 배송 조회 필요
     * - REFUND_PROCESSING: 환불 처리 진행상황 확인 필요
     * 삭제 허용 상태:
     * - DELIVERED: 배송 완료로 더 이상 추적 불필요
     * - CANCELLED: 주문 취소로 처리 완료
     * - REFUNDED: 환불 완료로 처리 완료
     * 특징:
     * - 물리적 삭제가 아닌 논리적 삭제로 데이터 무결성 보장
     * - 상태별 제한으로 사용자가 진행 중인 주문 상황을 놓치지 않도록 보호
     * - 판매 통계, 정산 데이터 보존으로 비즈니스 연속성 확보
     *
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param orderNumber 삭제할 주문 번호
     * @return 삭제 처리 결과
     * @throws NoSuchElementException 사용자를 찾을 수 없거나 주문이 존재하지 않는 경우
     * @throws IllegalArgumentException 구매자 권한이 없는 경우, 이미 삭제된 주문인 경우, 삭제 제한 상태인 경우
     */
    OrderDeleteResponse deleteOrder(UserPrincipal userPrincipal, String orderNumber);
}
