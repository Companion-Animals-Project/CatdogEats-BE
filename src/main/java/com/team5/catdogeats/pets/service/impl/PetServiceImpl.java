package com.team5.catdogeats.pets.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetResponseDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;
import com.team5.catdogeats.pets.repository.PetRepository;
import com.team5.catdogeats.pets.service.PetService;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final BuyerRepository buyerRepository;

    @Override
    public String registerPet(UserPrincipal userPrincipal, PetCreateRequestDto dto) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Pets pet = Pets.fromDto(dto, buyer);
        return petRepository.save(pet).getId();
    }

    @Override
    public Page<PetResponseDto> getMyPets(UserPrincipal userPrincipal, int page, int size) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Pageable pageable = PageRequest.of(page, size);

        return petRepository.findByBuyer(buyer, pageable)
                .map(PetResponseDto::fromEntity);
    }

    @Override
    public Page<PetResponseDto> getMyPetsWithCursor(UserPrincipal userPrincipal, ZonedDateTime cursorUpdatedAt, int size) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        List<Pets> pets;
        if (cursorUpdatedAt == null) {
            pets = petRepository.findByBuyerUserIdOrderByUpdatedAtDesc(buyerDTO.userId(), pageable);
        } else {
            pets = petRepository.findByBuyerUserIdAndUpdatedAtLessThanOrderByUpdatedAtDesc(
                    buyerDTO.userId(), cursorUpdatedAt, pageable
            );
        }
        List<PetResponseDto> dtos = pets.stream().map(PetResponseDto::fromEntity).toList();

        // 프론트에서 사용할 cursorUpdatedAt 값 추출 (응답의 마지막 row의 updatedAt)
        ZonedDateTime nextCursor = dtos.isEmpty() ? null : pets.get(dtos.size() - 1).getUpdatedAt();

        return new PageImpl<>(dtos, pageable, -1);
    }

    @JpaTransactional
    @Override
    public void updatePet(PetUpdateRequestDto dto, UserPrincipal userPrincipal) {
        try {
            Pets pet = petRepository.findByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId(), dto.petId())
                    .orElseThrow(() -> new NoSuchElementException("해당 펫 정보를 찾을 수 없습니다."));
            log.info("{} {}", userPrincipal.provider(), userPrincipal.providerId());
            pet.updateFromDto(dto);
        } catch (Exception e) {
            log.error("펫 정보 수정 중 오류 발생 {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deletePet(PetDeleteRequestDto dto, UserPrincipal userPrincipal) {
        Pets pet = petRepository.findByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId(),dto.petId())
                .orElseThrow(() -> new NoSuchElementException("해당 펫 정보를 찾을 수 없습니다."));

        petRepository.delete(pet);
    }
}
