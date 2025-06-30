package com.team5.catdogeats.pets.domain.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record CursorPetResponseDto(
        List<PetResponseDto> content,
        ZonedDateTime nextCursor,
        boolean hasNext
) {
}
