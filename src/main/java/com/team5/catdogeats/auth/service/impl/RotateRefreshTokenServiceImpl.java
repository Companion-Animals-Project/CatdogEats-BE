package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.redis.RefreshTokens;
import com.team5.catdogeats.auth.repository.RefreshTokensRedisRepository;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.auth.service.RotateRefreshTokenService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.exception.ExpiredTokenException;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RotateRefreshTokenServiceImpl implements RotateRefreshTokenService {
    private final RefreshTokensRedisRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @JpaTransactional
    public RotateTokenDTO RotateRefreshToken(String refreshTokenId) {
        try {
            RefreshTokens token = refreshTokenRepository.findById(refreshTokenId)
                    .orElseThrow(() -> new NoSuchElementException("Refresh token not found"));
            log.debug("Refresh token found: {}, {}, {}, {}, {}", token.getId(), token.getUserId(), token.getExpiresAt(), token.isUsed(), token.getCreatedAt());


            validateToken(refreshTokenId, token);
            token.markUsed();
            RefreshTokens newToken = refreshTokenRepository.save(token);
            log.debug("Refresh token 두번째 테스트: {}", newToken.getId());
            return buildRefreshTokens(newToken);
        } catch (ExpiredTokenException e) {
            log.warn("Expired token: {}", e.getMessage());
            throw e;
        } catch (InvalidTokenException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw e;
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error rotating refresh token: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private void validateToken(String refreshTokenId, RefreshTokens token) {
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Expired or invalid refresh token: {}", refreshTokenId);
            List<RefreshTokens> tokens = refreshTokenRepository.findByUserId(token.getUserId());
            refreshTokenRepository.deleteAll(tokens);
            throw new ExpiredTokenException();
        }

        if (token.isUsed()) {
            log.warn("Token reuse detected: {}", refreshTokenId);
            List<RefreshTokens> tokens = refreshTokenRepository.findByUserId(token.getUserId());
            refreshTokenRepository.deleteAll(tokens);
            throw new InvalidTokenException();
        }
    }

    private RotateTokenDTO buildRefreshTokens(RefreshTokens token) {
        // 새 토큰 발급
        UserPrincipal principal = new UserPrincipal(token.getProvider(), token.getProviderId());

        Authentication authentication = jwtService.getAuthentication(principal);
        String newAccessToken = jwtService.createAccessToken(authentication);
        String newRefreshToken = refreshTokenService.createRefreshToken(authentication);

        return new RotateTokenDTO(newAccessToken, newRefreshToken, "Cookie", 60 * 60 * 24);
    }
}
