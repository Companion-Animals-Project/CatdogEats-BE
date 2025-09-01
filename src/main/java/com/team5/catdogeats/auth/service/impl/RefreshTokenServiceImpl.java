package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final OAuth2ProviderStrategyFactory strategyFactory;
    private final UserRepository userRepository;
    private static final int MAX_TOKENS_PER_USER = 3;

    private final RedisScript<String> refreshTokenScript;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String createRefreshToken(Authentication authentication) {
        UserPrincipal principal = getUserPrincipal(authentication);

        Users user = userRepository.findByProviderAndProviderId(
                        principal.provider(), principal.providerId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String tokenId = UUID.randomUUID().toString();
        String now = String.valueOf(Instant.now().toEpochMilli());
        String expiresAt = String.valueOf(Instant.now().plusSeconds(86400).toEpochMilli());
        String maxTokens = String.valueOf(MAX_TOKENS_PER_USER);

        log.debug("Creating refresh token with userId: {}, tokenId: {}, provider: {}, providerId: {}, createdAt: {}, expiresAt: {}, maxTokens: {}",
                user.getId(), tokenId, principal.provider(), principal.providerId(), now, expiresAt, maxTokens);

        String result = stringRedisTemplate.execute(
                refreshTokenScript,
                Collections.singletonList(user.getId()),
                tokenId,
                principal.provider(),
                principal.providerId(),
                now,
                expiresAt,
                String.valueOf(MAX_TOKENS_PER_USER)
        );

        log.debug("Created refresh token with Lua: {}", result);
        return result;
    }

    private UserPrincipal getUserPrincipal(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        OAuth2ProviderStrategy strategy = strategyFactory.getStrategy(registrationId);
        String providerId = strategy.extractProviderId(oAuth2User);

        return new UserPrincipal(registrationId, providerId);
    }
}