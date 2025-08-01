package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.dto.dashboard.*;
import com.team5.catdogeats.admins.service.AdminDashboardService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderRepository;
//import com.team5.catdogeats.support.domain.repository.ReportsRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.team5.catdogeats.global.util.TimeConstants;


import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@JpaTransactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
//    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;


    @Override
    public DashboardResponseDTO getDashboardData() {
        // 1. 기본 통계 데이터 수집
        DashboardStatsDTO stats = getDashboardStats();

        // 2. 월별 회원 통계 데이터 수집 (최근 6개월)
        List<MonthlyUserStatsDTO> monthlyStats = getMonthlyUserStats();

        // 3. 일일 트렌드 데이터 수집 (최근 7일)
        List<DailyTrendDTO> dailyTrends = getDailyTrends();

        // 4. 응답 DTO 생성
        return DashboardResponseDTO.builder()
                .stats(stats)
                .monthlyUserStats(monthlyStats)
                .dailyTrends(dailyTrends)  // 추가
                .build();
    }

    private DashboardStatsDTO getDashboardStats() {
        // 총 회원 수
        long totalUsers = userRepository.count();

        // 이번 달 신규 회원 수
        ZonedDateTime startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(TimeConstants.SEOUL_ZONE_ID); // 수정
        long monthlyNewUsers = userRepository.countByCreatedAtAfter(startOfMonth);

        // 지난달 신규 회원 수 (전월 대비 계산용)
        ZonedDateTime startOfLastMonth = startOfMonth.minusMonths(1);
        ZonedDateTime endOfLastMonth = startOfMonth.minusSeconds(1);
        long lastMonthUsers = userRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);

        // 전월 대비 증가율 계산
        int monthlyUsersPercentage = calculatePercentageChange(lastMonthUsers, monthlyNewUsers);

        // 총 회원 전월 대비 (총 회원수 - 이번달 신규)
        long totalUsersLastMonth = totalUsers - monthlyNewUsers;
        int totalUsersPercentage = calculatePercentageChange(totalUsersLastMonth, totalUsers);

        // 이번 달 주문 수 (PAYMENT_PENDING 제외)
        long monthlyOrders = orderRepository.countByCreatedAtAfterAndOrderStatusNot(
                startOfMonth, OrderStatus.PAYMENT_PENDING);

        // 지난달 주문 수
        // 지난달 주문 수
        long lastMonthOrders = orderRepository.countByCreatedAtBetweenAndOrderStatusNot(
                startOfLastMonth, endOfLastMonth, OrderStatus.PAYMENT_PENDING);
        int ordersPercentage = calculatePercentageChange(lastMonthOrders, monthlyOrders);

        // 대기중인 신고 수
//        long pendingReports = reportRepository.countByReportStatus("PENDING");

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalUsersPercentage(totalUsersPercentage)
                .monthlyNewUsers(monthlyNewUsers)
                .monthlyNewUsersPercentage(monthlyUsersPercentage)
                .monthlyOrders(monthlyOrders)
                .monthlyOrdersPercentage(ordersPercentage)
//                .pendingReports(pendingReports)
                .build();
    }

    private List<MonthlyUserStatsDTO> getMonthlyUserStats() {
        List<MonthlyUserStatsDTO> monthlyStats = new ArrayList<>();

        LocalDate now = LocalDate.now();

        // 최근 6개월 데이터
        for (int i = 5; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);
            ZonedDateTime startOfMonth = targetMonth
                    .withDayOfMonth(1)
                    .atStartOfDay(TimeConstants.SEOUL_ZONE_ID); // 수정
            ZonedDateTime endOfMonth = targetMonth
                    .plusMonths(1)
                    .withDayOfMonth(1)
                    .atStartOfDay(TimeConstants.SEOUL_ZONE_ID)// 수정
                    .minusSeconds(1);

            long userCount = userRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

            String monthName = (targetMonth.getMonthValue()) + "월";

            monthlyStats.add(MonthlyUserStatsDTO.builder()
                    .month(monthName)
                    .userCount(userCount)
                    .build());
        }

        return monthlyStats;
    }

    private int calculatePercentageChange(long oldValue, long newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100 : 0;
        }
        return (int) Math.round(((double) (newValue - oldValue) / oldValue) * 100);
    }


    private List<DailyTrendDTO> getDailyTrends() {
        // 7일 전부터 오늘까지
        ZonedDateTime sevenDaysAgo = LocalDate.now()
                .minusDays(6)
                .atStartOfDay(TimeConstants.SEOUL_ZONE_ID);

        // ✅ DTO를 직접 받아서 처리 - 타입 캐스팅 불필요
        List<DailyUserStatsDTO> dailyUsers = userRepository.getDailyNewUsers(sevenDaysAgo);
        Map<LocalDate, Long> userMap = dailyUsers.stream()
                .collect(Collectors.toMap(
                        DailyUserStatsDTO::getJoinDate,
                        DailyUserStatsDTO::getUserCount,
                        (existing, replacement) -> existing // 중복 키 처리
                ));

        // 일별 주문 통계도 같은 방식으로 개선
        List<DailyOrderStatsDTO> dailyOrders = orderRepository.getDailyOrderStats(
                sevenDaysAgo,
                OrderStatus.PAYMENT_PENDING
        );
        Map<LocalDate, DailyOrderStatsDTO> orderMap = dailyOrders.stream()
                .collect(Collectors.toMap(
                        DailyOrderStatsDTO::getOrderDate,
                        Function.identity()
                ));

        // 7일간의 데이터 생성 (데이터 없는 날은 0으로)
        List<DailyTrendDTO> trends = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Long newUsers = userMap.getOrDefault(date, 0L);
            DailyOrderStatsDTO orderData = orderMap.get(date);

            trends.add(DailyTrendDTO.builder()
                    .date(date)
                    .newUsers(newUsers)
                    .orderCount(orderData != null ? orderData.getOrderCount() : 0L)
                    .orderCustomers(orderData != null ? orderData.getCustomerCount() : 0L)
                    .build());
        }

        return trends;
    }
}