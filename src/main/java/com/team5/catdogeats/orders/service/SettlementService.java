package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.dto.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;

/**
 * 정산현황 관리 서비스 인터페이스
 */
public interface SettlementService {

    /**
     * 전체 정산 리스트 조회 (페이징)
     */
    SettlementListResponseDto getSettlementList(UserPrincipal userPrincipal, Pageable pageable);

    /**
     * 기간별 정산 리스트 조회 (페이징)
     */
    SettlementListResponseDto getSettlementListByPeriod(
            UserPrincipal userPrincipal,
            SettlementPeriodRequestDTO periodRequest,
            Pageable pageable);

    /**
     * 이번달 정산현황 조회
     */
    MonthlySettlementStatusDto getMonthlySettlementStatus(UserPrincipal userPrincipal);

    /**
     * 월별 정산내역 영수증 조회 (JSON - 미리보기용, 페이징)
     */
    MonthlySettlementReceiptDto getMonthlySettlementReceipt(
            UserPrincipal userPrincipal,
            YearMonth targetMonth,
            Pageable pageable);

    /**
     * 월별 정산내역 CSV 파일 생성
     */
    ByteArrayResource generateMonthlyCsv(UserPrincipal userPrincipal, YearMonth targetMonth);
}