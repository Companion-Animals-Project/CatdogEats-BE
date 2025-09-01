package com.team5.catdogeats.auth.dto;

public record TokenRotationResult(
        int code,
        String provider,
        String providerId,
        String userId,
        String message
) {}
