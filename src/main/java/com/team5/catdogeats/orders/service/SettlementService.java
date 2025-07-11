package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.dto.MonthlySettlementReceiptDto;
import com.team5.catdogeats.orders.domain.dto.MonthlySettlementStatusDto;
import com.team5.catdogeats.orders.domain.dto.SettlementListResponseDto;
import com.team5.catdogeats.orders.domain.dto.SettlementPeriodRequestDTO;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;

/**
 * 정산현황 관리 서비스 인터페이스
 * 판매자의 정산 현황 조회, 기간별 조회, 월별 현황 등을 담당
 */
public interface SettlementService {

    /**
     * 전체 정산 리스트 조회 (페이징)
     * 1-1. 주문번호, 상품명, 주문금액, 수수료, 정산금액, 주문일, 상태의 정산 리스트 출력 (페이징)
     * 1-2. 총 정산내역 건수, 총 정산금액, 상태금액(정산완료+대기중/처리중), 평균 정산금액
     *
     * @param userPrincipal 인증된 판매자 정보
     * @param pageable 페이징 정보
     * @return 정산 리스트와 요약 정보
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     */
    SettlementListResponseDto getSettlementList(UserPrincipal userPrincipal, Pageable pageable);

    /**
     * 기간별 정산 리스트 조회 (페이징)
     * 2. 사용자가 선택한 기간동안의 정산 리스트 출력 (페이징)
     * 2-2. 선택한 기간동안의 정산내역 건수, 총 정산금액, 상태금액, 평균 정산금액
     *
     * @param userPrincipal 인증된 판매자 정보
     * @param periodRequest 기간 조회 요청 (시작일, 종료일)
     * @param pageable 페이징 정보
     * @return 기간별 정산 리스트와 요약 정보
     * @throws IllegalArgumentException 판매자 권한이 없거나 날짜 형식이 잘못된 경우
     */
    SettlementListResponseDto getSettlementListByPeriod(
            UserPrincipal userPrincipal,
            SettlementPeriodRequestDTO periodRequest,
            Pageable pageable
    );

    /**
     * 이번달 정산현황 조회
     * 3-1. 이번달 총 정산금액 = (정산확정금액 + 정산예정금액) = (정산완료 + 처리중 + 대기중)
     *      정산확정금액 = (정산완료 + 처리중)
     *      정산예정금액 = 대기중
     *
     * @param userPrincipal 인증된 판매자 정보
     * @return 이번달 정산현황
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     */
    MonthlySettlementStatusDto getMonthlySettlementStatus(UserPrincipal userPrincipal);

    /**
     * 월별 정산내역 영수증 조회 (CSV Export용)
     * 3-2. 월별 정산내역 영수증 (나중에 CSV 파일로 export)
     *
     * @param userPrincipal 인증된 판매자 정보
     * @param targetMonth 대상 년월 (예: 2024-12)
     * @return 월별 정산내역 영수증 데이터
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     */
    MonthlySettlementReceiptDto getMonthlySettlementReceipt(
            UserPrincipal userPrincipal,
            YearMonth targetMonth
    );
}
