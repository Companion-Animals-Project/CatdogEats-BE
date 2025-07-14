package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.orders.domain.dto.*;
import com.team5.catdogeats.orders.domain.enums.SettlementStatus;
import com.team5.catdogeats.orders.mapper.SettlementMapper;
import com.team5.catdogeats.orders.service.SettlementService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 정산현황 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@MybatisTransactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementMapper settlementMapper;
    private final SellersRepository sellerRepository;

    @Override
    public SettlementListResponseDto getSettlementList(UserPrincipal userPrincipal, Pageable pageable) {
        log.info("전체 정산 리스트 조회 시작 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        // 1. 판매자 권한 검증 및 ID 조회
        String sellerId = validateAndGetSellerId(userPrincipal);

        // 2. 페이징을 위한 offset 계산
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        // 3. 정산 리스트 조회 (Settlement 테이블만)
        List<SettlementItemDTO> settlements = settlementMapper.findSettlementsBySellerId(
                sellerId, offset, limit);

        // 4. 총 건수 조회
        Long totalCount = settlementMapper.countSettlementsBySellerId(sellerId);

        // 5. 정산 요약 정보 조회 (새로운 구조)
        SettlementSummaryDTO summary = settlementMapper.getSettlementSummaryBySellerId(sellerId);

        // 6. Page 객체 생성
        Page<SettlementItemDTO> settlementPage = new PageImpl<>(settlements, pageable, totalCount);

        log.info("전체 정산 리스트 조회 완료 - sellerId: {}, 총건수: {}, 현재페이지: {}",
                sellerId, totalCount, pageable.getPageNumber());

        return new SettlementListResponseDto(settlementPage, summary);
    }

    @Override
    public SettlementListResponseDto getSettlementListByPeriod(
            UserPrincipal userPrincipal,
            SettlementPeriodRequestDTO periodRequest,
            Pageable pageable) {

        log.info("기간별 정산 리스트 조회 시작 - provider: {}, providerId: {}, 기간: {} ~ {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                periodRequest.startDate(), periodRequest.endDate());

        // 1. 판매자 권한 검증 및 ID 조회
        String sellerId = validateAndGetSellerId(userPrincipal);

        // 2. 날짜 유효성 검증
        if (periodRequest.startDate().isAfter(periodRequest.endDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다");
        }

        // 3. 페이징을 위한 offset 계산
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        // 4. 기간별 정산 리스트 조회 (Settlement 테이블만)
        List<SettlementItemDTO> settlements = settlementMapper.findSettlementsBySellerIdAndPeriod(
                sellerId, periodRequest.startDate(), periodRequest.endDate(), offset, limit);

        // 5. 기간별 총 건수 조회
        Long totalCount = settlementMapper.countSettlementsBySellerIdAndPeriod(
                sellerId, periodRequest.startDate(), periodRequest.endDate());

        // 6. 기간별 정산 요약 정보 조회
        SettlementSummaryDTO summary = settlementMapper.getSettlementSummaryBySellerIdAndPeriod(
                sellerId, periodRequest.startDate(), periodRequest.endDate());

        // 7. Page 객체 생성
        Page<SettlementItemDTO> settlementPage = new PageImpl<>(settlements, pageable, totalCount);

        log.info("기간별 정산 리스트 조회 완료 - sellerId: {}, 총건수: {}, 기간: {} ~ {}",
                sellerId, totalCount, periodRequest.startDate(), periodRequest.endDate());

        return new SettlementListResponseDto(settlementPage, summary);
    }

    @Override
    public MonthlySettlementStatusDto getMonthlySettlementStatus(UserPrincipal userPrincipal) {
        log.info("이번달 정산현황 조회 시작 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        // 1. 판매자 권한 검증 및 ID 조회
        String sellerId = validateAndGetSellerId(userPrincipal);

        // 2. 현재 년월
        YearMonth currentMonth = YearMonth.now();

        // 3. 이번달 정산현황 조회
        MonthlySettlementStatusDto summary = settlementMapper.getMonthlySettlementStatus(sellerId, currentMonth);

        // 4. 정산현황 로그 (개수 정보 포함)
        log.info("이번달 정산현황 조회 완료 - sellerId: {}, 총건수: {}, 총금액: {}, 완료: {}건/{}원, 처리중: {}건/{}원",
                sellerId, summary.totalCount(), summary.totalMonthlyAmount(),
                summary.completedCount(), summary.completedAmount(),
                summary.inProgressCount(), summary.inProgressAmount());

        return summary;
    }

    @Override
    public MonthlySettlementReceiptDto getMonthlySettlementReceipt(
            UserPrincipal userPrincipal,
            YearMonth targetMonth,
            Pageable pageable) {

        log.info("월별 정산내역 영수증 조회 시작 - provider: {}, providerId: {}, 대상월: {}, page: {}, size: {}",
                userPrincipal.provider(), userPrincipal.providerId(), targetMonth,
                pageable.getPageNumber(), pageable.getPageSize());

        // 1. 판매자 권한 검증 및 정보 조회
        SellerDTO sellerDTO = validateAndGetSellerInfo(userPrincipal);

        // 2. 페이징을 위한 offset 계산
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        // 3. 월별 정산 아이템 조회 (페이징)
        List<SettlementItemDTO> items = settlementMapper.findMonthlySettlementsWithPaging(
                sellerDTO.userId(), targetMonth, offset, limit);

        // 4. 총 건수 조회
        Long totalCount = settlementMapper.countMonthlySettlements(
                sellerDTO.userId(), targetMonth);

        // 5. 월별 정산 요약 정보 조회
        MonthlySettlementStatusDto summary = settlementMapper.getMonthlySettlementSummary(
                sellerDTO.userId(), targetMonth);

        // 6. Page 객체 생성
        Page<SettlementItemDTO> itemPage = new PageImpl<>(items, pageable, totalCount);

        log.info("월별 정산내역 영수증 조회 완료 - sellerId: {}, 대상월: {}, 총건수: {}, 현재페이지건수: {}",
                sellerDTO.userId(), targetMonth, totalCount, items.size());

        return new MonthlySettlementReceiptDto(
                targetMonth,
                sellerDTO.vendorName(),
                sellerDTO.businessNumber(),
                itemPage,
                summary
        );
    }

    @Override
    public ByteArrayResource generateMonthlyCsv(UserPrincipal userPrincipal, YearMonth targetMonth) {
        log.info("월별 정산내역 CSV 생성 시작 - provider: {}, providerId: {}, 대상월: {}",
                userPrincipal.provider(), userPrincipal.providerId(), targetMonth);

        // 1. 판매자 권한 검증 및 정보 조회
        SellerDTO sellerDTO = validateAndGetSellerInfo(userPrincipal);

        // 2. 모든 정산 데이터 조회 (페이징 없이)
        List<SettlementItemDTO> items = settlementMapper.findMonthlySettlements(
                sellerDTO.userId(), targetMonth);

        // 3. 요약 정보 조회
        MonthlySettlementStatusDto summary = settlementMapper.getMonthlySettlementSummary(
                sellerDTO.userId(), targetMonth);

        // 4. CSV 문자열 생성
        StringBuilder csv = new StringBuilder();

        // CSV 헤더 (업체 정보)
        csv.append("업체명,사업자번호,대상월\n");
        csv.append(String.format("%s,%s,%s\n\n",
                escapeCSV(sellerDTO.vendorName()),
                sellerDTO.businessNumber(),
                targetMonth));

        // 요약 정보
        csv.append("구분,건수,금액\n");
        csv.append(String.format("총 정산,%d,%d\n", summary.totalCount(), summary.totalMonthlyAmount()));
        csv.append(String.format("정산완료,%d,%d\n", summary.completedCount(), summary.completedAmount()));
        csv.append(String.format("처리중,%d,%d\n\n", summary.inProgressCount(), summary.inProgressAmount()));

        // 상세 내역 헤더
        csv.append("주문번호,상품명,주문금액,수수료,정산금액,주문일,배송완료일,정산생성일,상태\n");

        // 상세 데이터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (SettlementItemDTO item : items) {
            csv.append(String.format("%s,%s,%d,%d,%d,%s,%s,%s,%s\n",
                    item.orderNumber(),
                    escapeCSV(item.productName()),
                    item.orderAmount(),
                    item.commission(),
                    item.settlementAmount(),
                    item.orderDate() != null ? item.orderDate().format(formatter) : "",
                    item.deliveryDate() != null ? item.deliveryDate().format(formatter) : "",
                    item.settlementCreatedAt() != null ? item.settlementCreatedAt().format(formatter) : "",
                    getStatusDisplayName(item.status())
            ));
        }

        // 5. ByteArrayResource로 변환
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        // BOM 추가 (Excel에서 한글 깨짐 방지)
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvWithBom = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, csvWithBom, 0, bom.length);
        System.arraycopy(csvBytes, 0, csvWithBom, bom.length, csvBytes.length);

        log.info("월별 정산내역 CSV 생성 완료 - sellerId: {}, 대상월: {}, 아이템수: {}",
                sellerDTO.userId(), targetMonth, items.size());

        return new ByteArrayResource(csvWithBom);
    }

    /**
     * CSV용 문자열 이스케이프 처리
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // 쉼표, 따옴표, 줄바꿈이 있으면 따옴표로 감싸기
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 정산상태 한글 표시명 변환
     */
    private String getStatusDisplayName(SettlementStatus status) {
        return switch (status) {
            case COMPLETED -> "완료";
            case IN_PROGRESS -> "처리중";
            default -> status.name();
        };
    }


    /**
     * UserPrincipal로 판매자 검증 및 ID 조회
     *
     * @param userPrincipal 인증된 사용자 정보
     * @return 판매자 ID
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     */
    private String validateAndGetSellerId(UserPrincipal userPrincipal) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> {
                    log.warn("판매자를 찾을 수 없음 - provider: {}, providerId: {}",
                            userPrincipal.provider(), userPrincipal.providerId());
                    return new NoSuchElementException("판매자 정보를 찾을 수 없습니다");
                });

        return sellerDTO.userId();
    }

    /**
     * UserPrincipal로 판매자 검증 및 상세 정보 조회 (영수증용)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @return 판매자 상세 정보
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     */
    private SellerDTO validateAndGetSellerInfo(UserPrincipal userPrincipal) {
        return sellerRepository.findSellerDtoByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> {
                    log.warn("판매자를 찾을 수 없음 - provider: {}, providerId: {}",
                            userPrincipal.provider(), userPrincipal.providerId());
                    return new NoSuchElementException("판매자 정보를 찾을 수 없습니다");
                });
    }
}