package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.dto.NameMaskingStatusResponseDto;
import com.team5.catdogeats.users.repository.BuyerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuyerService 단위 테스트")
class BuyerServiceImplTest {

    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private BuyerMaskingServiceImpl buyerMaskingService;

    private UserPrincipal userPrincipal;
    private BuyerDTO buyerDTO;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal("testProvider", "testProviderId");
        buyerDTO = new BuyerDTO(
                "user123",
                true,  // nameMaskingStatus
                false, // isDeleted
                null   // deletedAt
        );
    }

    @Test
    @DisplayName("마스킹 상태 조회 - 성공")
    void getNameMaskingStatus_Success() {
        // given
        given(buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        )).willReturn(Optional.of(buyerDTO));

        // when
        NameMaskingStatusResponseDto result = buyerMaskingService.getNameMaskingStatus(userPrincipal);

        // then
        assertThat(result.nameMaskingStatus()).isTrue();
        then(buyerRepository).should(times(1))
                .findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId());
    }

    @Test
    @DisplayName("마스킹 상태 조회 - 구매자 정보 없음")
    void getNameMaskingStatus_BuyerNotFound() {
        // given
        given(buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        )).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> buyerMaskingService.getNameMaskingStatus(userPrincipal))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당 구매자 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("마스킹 상태 변경 - true에서 false로 변경")
    void changeNameMaskingStatus_TrueToFalse() {
        // given
        BuyerDTO buyerWithTrueMasking = new BuyerDTO("user123", true, false, null);

        given(buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        )).willReturn(Optional.of(buyerWithTrueMasking));

        // when
        NameMaskingStatusResponseDto result = buyerMaskingService.changeNameMaskingStatus(userPrincipal);

        // then
        assertThat(result.nameMaskingStatus()).isFalse(); // true의 반대 = false
        then(buyerRepository).should(times(1))
                .findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId());
        then(buyerRepository).should(times(1))
                .changeNameMaskingStatus("user123");
    }

    @Test
    @DisplayName("마스킹 상태 변경 - false에서 true로 변경")
    void changeNameMaskingStatus_FalseToTrue() {
        // given
        BuyerDTO buyerWithFalseMasking = new BuyerDTO("user123", false, false, null);

        given(buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        )).willReturn(Optional.of(buyerWithFalseMasking));

        // when
        NameMaskingStatusResponseDto result = buyerMaskingService.changeNameMaskingStatus(userPrincipal);

        // then
        assertThat(result.nameMaskingStatus()).isTrue(); // false의 반대 = true
        then(buyerRepository).should(times(1))
                .findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId());
        then(buyerRepository).should(times(1))
                .changeNameMaskingStatus("user123");
    }

    @Test
    @DisplayName("마스킹 상태 변경 - 구매자 정보 없음")
    void changeNameMaskingStatus_BuyerNotFound() {
        // given
        given(buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        )).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> buyerMaskingService.changeNameMaskingStatus(userPrincipal))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당 구매자 정보를 찾을 수 없습니다.");

        // Repository의 changeNameMaskingStatus 메서드는 호출되지 않아야 함
        then(buyerRepository).should(times(0)).changeNameMaskingStatus(anyString());
    }
}