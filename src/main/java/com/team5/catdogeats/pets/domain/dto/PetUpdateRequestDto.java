package com.team5.catdogeats.pets.domain.dto;

import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import jakarta.validation.constraints.NotNull;

public record PetUpdateRequestDto(
        @NotNull(message = "petId는 필수입니다.")
        String petId,
        String name,
        PetCategory petCategory,
        Gender gender,
        String breed,
        Short age,
        Boolean isAllergy,
        String healthState,
        String requestion
) {
}
