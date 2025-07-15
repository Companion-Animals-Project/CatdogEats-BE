package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 쓰기 전용 서비스 인터페이스
 * 단일 책임: 주문 변경 관련 기능만 담당
 * 포함 기능:
 * - 주문 상태 변경 (배송 상태 관리)
 * - 운송장 번호 등록 (배송 운송장 등록)
 * - 주문 내역 삭제 (목록 숨김 처리)
 * - 배송 상태 동기화 (물류 서버 연동)
 */
public interface SellerOrderCommandService {

    /**
     * 주문 상태 변경 (배송 상태 관리)
     * 판매자가 본인이 판매한 상품이 포함된 주문의 상태를 변경
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 현재 상태에서 요청 상태로의 전환 가능성 검증
     * 4. 상태 전환 규칙 및 제약 조건 확인
     * 5. 주문 상태 업데이트 (트랜잭션)
     * 6. 필요 시 관련 엔티티 동시 업데이트 (Shipments 등)
     * 7. 응답 DTO 생성 및 반환
     * 상태 전환 규칙:
     * - PAYMENT_COMPLETED → PREPARING (상품준비중)
     * - PREPARING → READY_FOR_SHIPMENT (배송준비완료)
     * - PREPARING → CANCELLED (주문취소) - 특별한 경우
     * - READY_FOR_SHIPMENT → IN_DELIVERY (배송중) - 운송장 등록 필요
     * - 역순 전환 및 단계 건너뛰기 금지
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 주문 상태 변경 요청 정보
     * @return 상태 변경 처리 결과
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 상태 전환인 경우
     * @throws IllegalStateException 이미 처리 중인 주문이거나 변경할 수 없는 상태인 경우
     */
    OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request);

    /**
     * 운송장 번호 등록 (배송 운송장 등록)
     * 판매자가 택배사에서 발급받은 운송장 번호를 등록하여 배송을 시작
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 주문 상태가 운송장 등록 가능한 상태인지 확인 (READY_FOR_SHIPMENT)
     * 4. 운송장 번호 중복 확인 (같은 택배사 내에서)
     * 5. Shipments 엔티티에 택배사/운송장번호/발송일시 저장
     * 6. 주문 상태를 IN_DELIVERY로 자동 변경 (요청 시)
     * 7. 응답 DTO 생성 및 반환
     * 지원 택배사:
     * - 우체국택배, CJ대한통운, 한진택배, 로젠택배, 롯데택배
     * 운송장 검증:
     * - 택배사별 운송장 번호 형식 검증
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 운송장 번호 등록 요청 정보
     * @return 운송장 등록 처리 결과
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 운송장 번호인 경우
     * @throws IllegalStateException 운송장 등록이 불가능한 주문 상태인 경우
     */
    TrackingNumberRegisterResponse registerTrackingNumber(UserPrincipal userPrincipal, TrackingNumberRegisterRequest request);

    /**
     * 주문 내역 삭제 (목록 숨김 처리)
     * 완료된 주문을 판매자 목록에서 숨김 처리하여 관리 편의성 제공
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
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
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 삭제할 주문 번호
     * @return 삭제 처리 결과
     * @throws NoSuchElementException 사용자를 찾을 수 없거나 주문이 존재하지 않는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 이미 삭제된 주문인 경우, 삭제 제한 상태인 경우
     */
    boolean deleteOrder(UserPrincipal userPrincipal, String orderNumber);

    /**
     * 전체 배송 상태 동기화
     * 판매자의 모든 배송 중(IN_DELIVERY) 주문에 대해 테스트 물류 서버에서 상태를 조회하고,
     * DELIVERED 상태인 주문들을 자동으로 배송 완료로 업데이트합니다.
     * 프로세스:
     * 1. 판매자 권한 확인
     * 2. 판매자의 IN_DELIVERY 상태 주문 목록 조회
     * 3. 각 주문의 trackingNumber로 물류 서버 API 호출 (GET /api/v1/trackings/{trackingNumber})
     * 4. currentStatus가 'DELIVERED'인 주문들을 DB에서 DELIVERED로 업데이트
     * 5. 동기화 결과 반환
     * 특징:
     * - 수동 새로고침 방식으로 복잡한 스케줄러 없이 단순하고 효율적
     * - 테스트 목적에 부합하는 실용적 접근
     * - 물류 서버 API 호출 실패 시 안전한 오류 처리
     * - 부분적 동기화 실패에도 전체 프로세스 중단되지 않음
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @return 동기화 결과 (총 조회된 주문 수, 업데이트된 주문 수, 업데이트된 주문 목록)
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     */
    ShipmentSyncResponse syncAllShipmentStatus(UserPrincipal userPrincipal);
}