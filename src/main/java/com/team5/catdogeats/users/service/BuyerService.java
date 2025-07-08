package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.NameMaskingStatusResponseDto;

public interface BuyerService {
    // 마스킹 상태여부 조회
    NameMaskingStatusResponseDto getNameMaskingStatus(UserPrincipal userPrincipal);
    // 마스킹 상태 변경
    NameMaskingStatusResponseDto changeNameMaskingStatus(UserPrincipal userPrincipal);
}