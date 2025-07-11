package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이름 마스킹 상태 응답 DTO")
public record NameMaskingStatusResponseDto(
        @Schema(description = "이름 마스킹 사용 여부", example = "true")
        Boolean nameMaskingStatus
) {
    public static NameMaskingStatusResponseDto from(Boolean nameMaskingStatus) {
        return new NameMaskingStatusResponseDto(nameMaskingStatus);
    }
}
