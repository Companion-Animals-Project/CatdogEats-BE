package com.team5.catdogeats.support.service;


import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportCreateRequestDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;


public interface ReportService {

    // 신고 생성
    String createReport(ReportCreateRequestDto request, String reporterId);

    // 사용자별 신고 목록 조회
    PageResponseDto<ReportListResponseDto> getUserReports(String userId, int page, int size);

}