package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.dto.NameMaskingStatusResponseDto;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.service.BuyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BuyerServiceImpl implements BuyerService {

    private final BuyerRepository buyerRepository;

    /*
    마스킹 상태여부 조회
     */
    @Override
    public NameMaskingStatusResponseDto getNameMaskingStatus(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> new NoSuchElementException("해당 구매자 정보를 찾을 수 없습니다."));

        return NameMaskingStatusResponseDto.from(buyerDTO.nameMaskingStatus());
    }

    /*
    마스킹 상태 변경
     */
    @JpaTransactional
    @Override
    public NameMaskingStatusResponseDto changeNameMaskingStatus(UserPrincipal userPrincipal) {

        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> new NoSuchElementException("해당 구매자 정보를 찾을 수 없습니다."));


        buyerRepository.changeNameMaskingStatus(buyerDTO.userId());

        return NameMaskingStatusResponseDto.from(!buyerDTO.nameMaskingStatus());
    }
}