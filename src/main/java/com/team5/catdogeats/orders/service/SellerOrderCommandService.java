package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 쓰기 전용 서비스 인터페이스 (CQRS Command)
 * 단일 책임: 주문 변경 관련 기능만 담당
 *
 * 포함 기능:
 * - 주문 상태 변경 (배송 상태 관리)
 * - 운송장 번호 등록 (배송 운송장 등록)
 * - 주문 내역 삭제 (목록 숨김 처리)
 */
public interface SellerOrderCommandService {

    /**
     * 주문 상태 변경 (배송 상태 관리)
     * 판매자가 본인이 판매한 상품이 포함된 주문의 상태를 변경
     *
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 현재 상태에서 요청 상태로의 전환 가능성 검증
     * 4. 상태 전환 규칙 및 제약 조건 확인
     * 5. 주문 상태 업데이트 (트랜잭션)
     * 6. 필요 시 관련 엔티티 동시 업데이트 (Shipments 등)
     * 7. 응답 DTO 생성 및 반환
     *
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
     *
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 주문 상태가 운송장 등록 가능한 상태인지 확인 (READY_FOR_SHIPMENT)
     * 4. 운송장 번호 중복 확인 (같은 택배사 내에서)
     * 5. 스마트택배 API를 통한 운송장 번호 유효성 검증 (선택)
     * 6. Shipments 엔티티에 택배사/운송장번호/발송일시 저장
     * 7. 주문 상태를 IN_DELIVERY로 자동 변경 (요청 시)
     * 8. 응답 DTO 생성 및 반환
     *
     * 지원 택배사:
     * - 우체국택배 (01), CJ대한통운 (04), 한진택배 (05)
     * - 로젠택배 (06), 롯데택배 (08)
     *
     * 운송장 검증:
     * - 택배사별 운송장 번호 형식 검증
     * - 스마트택배 API 연동하여 실제 유효성 확인 (선택)
     * - API 장애 시 형식 검증만으로 등록 가능
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 운송장 번호 등록 요청 정보
     * @return 운송장 등록 처리 결과
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 운송장 번호인 경우
     * @throws IllegalStateException 운송장 등록이 불가능한 주문 상태인 경우
     * @throws RuntimeException 스마트택배 API 호출 실패 시 (검증 활성화된 경우)
     */
    TrackingNumberRegisterResponse registerTrackingNumber(UserPrincipal userPrincipal, TrackingNumberRegisterRequest request);

    /**
     * 주문 내역 삭제 (목록 숨김 처리)
     * 완료된 주문을 판매자 목록에서 숨김 처리하여 관리 편의성 제공
     *
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 삭제 가능한 주문 상태인지 확인 (DELIVERED, CANCELLED)
     * 4. 이미 숨김 처리된 주문인지 확인
     * 5. 숨김 처리 실행 (soft delete)
     * 6. 처리 결과 반환
     *
     * 삭제 가능 조건:
     * - 주문 상태가 DELIVERED (배송 완료) 또는 CANCELLED (주문 취소)
     * - 판매자 본인의 상품이 포함된 주문
     * - 이미 숨김 처리되지 않은 주문
     *
     * 주의사항:
     * - 실제 데이터 삭제가 아닌 숨김 처리 (관리자는 여전히 조회 가능)
     * - 진행 중인 주문 (PREPARING, IN_DELIVERY 등)은 삭제 불가
     * - 삭제 후 복구는 별도 API나 관리자 권한 필요
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 삭제할 주문 번호
     * @return 삭제 처리 성공 여부
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     * @throws IllegalStateException 삭제할 수 없는 주문 상태인 경우
     */
    boolean deleteOrder(UserPrincipal userPrincipal, String orderNumber);
}