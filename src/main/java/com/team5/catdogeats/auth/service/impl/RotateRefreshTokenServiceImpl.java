package com.team5.catdogeats.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.dto.TokenRotationResult;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.auth.service.RotateRefreshTokenService;
import com.team5.catdogeats.global.exception.ExpiredTokenException;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RotateRefreshTokenServiceImpl implements RotateRefreshTokenService {
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisScript<String> rotateTokenScript;

    @Override
    public RotateTokenDTO RotateRefreshToken(String refreshTokenId) {
        try {
            // Lua 스크립트로 원자적 토큰 회전 처리
            TokenRotationResult result = executeTokenRotationScript(refreshTokenId);

            int status = result.code();

            switch (status) {
                case -1:
                    log.warn("Refresh token not found: {}", refreshTokenId);
                    throw new NoSuchElementException("Refresh token not found");
                case -2:
                    log.warn("Refresh token expired: {}", refreshTokenId);
                    throw new ExpiredTokenException();
                case -3:
                    log.warn("Token reuse detected: {}", refreshTokenId);
                    throw new InvalidTokenException();
                case 1:
                    // 성공 케이스
                    String provider = result.provider();
                    String providerId = result.providerId();

                    log.debug("Token rotation successful for provider: {}, providerId: {}", provider, providerId);
                    return buildNewTokens(provider, providerId);
                default:
                    log.error("Unknown token rotation result code: {}", status);
                    throw new RuntimeException("Unknown token rotation result");
            }

        } catch (ExpiredTokenException | InvalidTokenException | NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error rotating refresh token: {}", e.getMessage(), e);
            throw new RuntimeException("Token rotation failed", e);
        }
    }

    private TokenRotationResult executeTokenRotationScript(String refreshTokenId) {
        String tokenKey = "refreshToken:" + refreshTokenId;
        long currentTime = Instant.now().toEpochMilli();

        String rawResult = stringRedisTemplate.execute(
                    rotateTokenScript,
                    Collections.singletonList(tokenKey),
                    String.valueOf(currentTime));

        try {
            log.debug("Script returned  type {},  JSON: {}", rawResult.getClass().getName(), rawResult);
            return objectMapper.readValue(rawResult, TokenRotationResult.class);

        } catch (JsonProcessingException e) {
            log.error("Error parsing token rotation script result. rawResult class={}, value={}, error={}",
                    rawResult.getClass().getName(), rawResult, e.getMessage(), e);
            throw new RuntimeException("Failed to parse script result", e);
        }
    }


    private RotateTokenDTO buildNewTokens(String provider, String providerId) {
        // 새 토큰 발급
        UserPrincipal principal = new UserPrincipal(provider, providerId);
        Authentication authentication = jwtService.getAuthentication(principal);

        String newAccessToken = jwtService.createAccessToken(authentication);
        String newRefreshToken = refreshTokenService.createRefreshToken(authentication);

        return new RotateTokenDTO(
                newAccessToken,
                newRefreshToken,
                "Cookie",
                60 * 60 * 24
        );
    }
}