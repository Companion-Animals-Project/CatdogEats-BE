package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
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
    private final RedissonClient redissonClient;
    private static final int MAX_TOKENS_PER_USER = 3;

    private static final String LUA_SCRIPT = """
            local userId = KEYS[1]
            local tokenId = ARGV[1]
            local provider = ARGV[2]
            local providerId = ARGV[3]
            local createdAt = tonumber(ARGV[4])
            local expiresAt = tonumber(ARGV[5])
            local maxTokens = tonumber(ARGV[6])

            if not createdAt or not expiresAt or not maxTokens then
                return redis.error_reply("Invalid numeric argument")
            end

            local tokenKey = "refreshToken:" .. tokenId
            local userTokensSetKey = "userTokens:" .. userId

            redis.call("HMSET", tokenKey,
                "id", tokenId,
                "provider", provider,
                "providerId", providerId,
                "userId", userId,
                "used", "false",
                "createdAt", createdAt,
                "expiresAt", expiresAt
            )

            redis.call("EXPIRE", tokenKey, 86400)

            redis.call("ZADD", userTokensSetKey, createdAt, tokenId)

            redis.call("EXPIRE", userTokensSetKey, 86400)

            local currentTokenCount = redis.call("ZCARD", userTokensSetKey)

            if currentTokenCount > maxTokens then
                local oldestTokenId = redis.call("ZPOPMIN", userTokensSetKey, 1)
                if oldestTokenId and oldestTokenId[1] then
                    redis.call("DEL", "refreshToken:" .. oldestTokenId[1])
                end
            end

            return tokenId
    """;

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

        String result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                LUA_SCRIPT,
                RScript.ReturnType.VALUE,
                Collections.singletonList(user.getId()),
                tokenId,
                principal.provider(),
                principal.providerId(),
                now,
                expiresAt,
                maxTokens
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