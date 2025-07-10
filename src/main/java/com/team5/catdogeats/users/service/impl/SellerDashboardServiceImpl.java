package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.users.domain.dto.*;
import com.team5.catdogeats.users.mapper.SellerDashboardMapper;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자 대시보드 서비스 구현체 (수요예측 포함)
 * 실시간 주문 데이터와 수요예측 결과를 기반으로 판매자 대시보드 통계를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@MybatisTransactional(readOnly = true)
public class SellerDashboardServiceImpl implements SellerDashboardService {

    private final SellerDashboardMapper sellerDashboardMapper;
    private final SellersRepository sellersRepository;
    private final DemandForecastService demandForecastService;

    @Override
    public SellerDashboardResponseDTO getDashboardData(UserPrincipal userPrincipal) {
        log.info("판매자 대시보드 데이터 조회 시작 (수요예측 포함) - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        try {
            // 1. 판매자 권한 검증 및 ID 조회
            String sellerId = validateAndGetSellerId(userPrincipal);

            // 2. 병렬로 대시보드 데이터 조회
            TodayStatsDTO todayStats = getTodayStats(sellerId);
            List<WeeklySalesDTO> weeklySales = getWeeklySales(sellerId);
            List<ProductSalesRankingDTO> productRanking = getProductRanking(sellerId);
            List<DemandForecastResultDTO> demandForecasts = getDemandForecasts(sellerId);

            // 3. 응답 데이터 생성 (수요예측 포함)
            SellerDashboardResponseDTO response = SellerDashboardResponseDTO.of(
                    todayStats,
                    weeklySales,
                    productRanking,
                    demandForecasts
            );

            log.info("판매자 대시보드 데이터 조회 완료 - sellerId: {}, 오늘주문: {}건, 오늘매출: {}원, 주간데이터: {}일, 상품순위: {}개, 수요예측: {}개",
                    sellerId,
                    todayStats.todayOrderCount(),
                    todayStats.todayTotalSales(),
                    weeklySales.size(),
                    productRanking.size(),
                    demandForecasts.size());

            return response;

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자 대시보드 데이터 조회 실패 - provider: {}, providerId: {}, reason: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자 대시보드 데이터 조회 중 예상치 못한 오류 발생 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);
            throw new RuntimeException("대시보드 데이터 조회 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public List<DemandForecastResultDTO> getDemandForecastResults(UserPrincipal userPrincipal) {
        log.info("수요예측 결과 조회 시작 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        try {
            String sellerId = validateAndGetSellerId(userPrincipal);
            List<DemandForecastResultDTO> results = getDemandForecasts(sellerId);

            log.info("수요예측 결과 조회 완료 - sellerId: {}, 결과 수: {}", sellerId, results.size());
            return results;

        } catch (Exception e) {
            log.error("수요예측 결과 조회 실패 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);
            throw new RuntimeException("수요예측 결과 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * UserPrincipal로 판매자 검증 및 ID 조회
     * 기존 패턴을 따라 SellersRepository의 findSellerDtoByProviderAndProviderId 활용
     *
     * @param userPrincipal 인증된 사용자 정보
     * @return 판매자 ID (userId)
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     */
    private String validateAndGetSellerId(UserPrincipal userPrincipal) {
        SellerDTO sellerDTO = sellersRepository.findSellerDtoByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> {
                    log.warn("판매자를 찾을 수 없음 - provider: {}, providerId: {}",
                            userPrincipal.provider(), userPrincipal.providerId());
                    return new NoSuchElementException("판매자 정보를 찾을 수 없습니다");
                });

        // 삭제된 판매자 계정 체크
        if (sellerDTO.isDeleted()) {
            log.warn("삭제된 판매자 계정 접근 시도 - userId: {}", sellerDTO.userId());
            throw new IllegalArgumentException("비활성화된 판매자 계정입니다");
        }

        return sellerDTO.userId();
    }

    /**
     * 오늘 주문 통계 조회
     *
     * @param sellerId 판매자 ID
     * @return 오늘 주문 수와 매출액
     */
    private TodayStatsDTO getTodayStats(String sellerId) {
        try {
            TodayStatsDTO todayStats = sellerDashboardMapper.findTodayStats(sellerId);

            // null 안전성 보장
            if (todayStats == null) {
                log.debug("오늘 주문 통계 데이터 없음 - sellerId: {}", sellerId);
                return TodayStatsDTO.empty();
            }

            return todayStats;
        } catch (Exception e) {
            log.error("오늘 주문 통계 조회 실패 - sellerId: {}", sellerId, e);
            return TodayStatsDTO.empty();
        }
    }

    /**
     * 주간 매출 동향 조회
     *
     * @param sellerId 판매자 ID
     * @return 주간 일별 매출 리스트 (7일간)
     */
    private List<WeeklySalesDTO> getWeeklySales(String sellerId) {
        try {
            List<WeeklySalesDTO> weeklySales = sellerDashboardMapper.findWeeklySales(sellerId);

            if (weeklySales == null || weeklySales.isEmpty()) {
                log.debug("주간 매출 데이터 없음 - sellerId: {}", sellerId);
                return List.of();
            }

            return weeklySales;
        } catch (Exception e) {
            log.error("주간 매출 동향 조회 실패 - sellerId: {}", sellerId, e);
            return List.of();
        }
    }

    /**
     * 이번 달 상품 매출 순위 조회
     *
     * @param sellerId 판매자 ID
     * @return 상품 매출 순위 리스트 (TOP 10)
     */
    private List<ProductSalesRankingDTO> getProductRanking(String sellerId) {
        try {
            List<ProductSalesRankingDTO> productRanking =
                    sellerDashboardMapper.findMonthlyProductRanking(sellerId);

            if (productRanking == null || productRanking.isEmpty()) {
                log.debug("상품 매출 순위 데이터 없음 - sellerId: {}", sellerId);
                return List.of();
            }

            return productRanking;
        } catch (Exception e) {
            log.error("상품 매출 순위 조회 실패 - sellerId: {}", sellerId, e);
            return List.of();
        }
    }

    /**
     * 수요예측 결과 조회 (NEW)
     *
     * @param sellerId 판매자 ID
     * @return 수요예측 결과 리스트 (재고 부족량 순으로 정렬)
     */
    private List<DemandForecastResultDTO> getDemandForecasts(String sellerId) {
        try {
            List<DemandForecastResultDTO> forecasts = demandForecastService.getLatestForecastResults(sellerId);

            if (forecasts == null || forecasts.isEmpty()) {
                log.debug("수요예측 데이터 없음 - sellerId: {}", sellerId);
                return List.of();
            }

            log.debug("수요예측 결과 조회 완료 - sellerId: {}, 예측 상품 수: {}, 부족 예상 상품: {}",
                    sellerId,
                    forecasts.size(),
                    forecasts.stream().mapToInt(f -> f.shortageQuantity() > 0 ? 1 : 0).sum());

            return forecasts;
        } catch (Exception e) {
            log.error("수요예측 결과 조회 실패 - sellerId: {}", sellerId, e);
            return List.of();
        }
    }
}