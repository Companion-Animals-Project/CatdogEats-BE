package com.team5.catdogeats.support.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ReportStatsResponseDto(
        List<Object[]> statusStats,     // 상태별 통계
        List<Object[]> typeStats,       // 타입별 통계
        List<Object[]> weeklyStats,     // 주간 통계
        long pendingCount               // 처리 대기 건수
) {}