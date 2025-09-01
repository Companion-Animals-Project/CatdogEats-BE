package com.team5.catdogeats.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.chats.util.ChatSubscriber;
import com.team5.catdogeats.global.config.RedisScriptConfig;
import com.team5.catdogeats.global.exception.ExpiredTokenException;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import com.team5.catdogeats.notifications.util.NotificationSubscriber;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(classes = {RotateRefreshTokenServiceImplTest.TestConfig.class, RedisScriptConfig.class})
class RotateRefreshTokenServiceImplTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public OAuth2ProviderStrategyFactory strategyFactory() {
            return mock(OAuth2ProviderStrategyFactory.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public ChatSubscriber chatSubscriber() {
            return mock(ChatSubscriber.class);
        }

        @Bean
        public NotificationSubscriber notificationSubscriber() {
            return mock(NotificationSubscriber.class);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
            config.setPassword("0000");
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();
            return factory;
        }

        @Bean
        public StringRedisTemplate redisTemplate(RedisConnectionFactory factory) {
            return new StringRedisTemplate(factory);
        }

        @Bean(destroyMethod = "shutdown")
        public RedissonClient redissonClient() {
            Config config = new Config();
            config.setCodec(new StringCodec());
            config.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setPassword("0000");
            return Redisson.create(config);
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2ProviderStrategyFactory strategyFactory;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisScript<String> refreshTokenScript;

    @Autowired
    private RedisScript<String> rotateTokenScript;

    @Autowired
    private ObjectMapper objectMapper;

    private RefreshTokenServiceImpl refreshTokenService;
    private RotateRefreshTokenServiceImpl rotateRefreshTokenService;

    private final String TEST_USER_ID = "test-user-1";
    private final String PROVIDER = "google";
    private final String PROVIDER_ID = "google-123";

    @BeforeEach
    void setup() {
        // Redis 초기화
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });

        // 사용자 mock 설정
        Users testUser = Users.builder()
                .id(TEST_USER_ID)
                .name("테스트유저")
                .provider(PROVIDER)
                .providerId(PROVIDER_ID)
                .role(Role.ROLE_BUYER)
                .build();

        when(userRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                .thenReturn(Optional.of(testUser));

        OAuth2ProviderStrategy strategy = mock(OAuth2ProviderStrategy.class);
        when(strategyFactory.getStrategy(PROVIDER)).thenReturn(strategy);
        when(strategy.extractProviderId(any())).thenReturn(PROVIDER_ID);

        // JWT 서비스 mock 설정
        Authentication mockAuth = createMockAuthentication();
        when(jwtService.getAuthentication(any())).thenReturn(mockAuth);
        when(jwtService.createAccessToken(any())).thenReturn("new-access-token");

        // 서비스 인스턴스 생성
        refreshTokenService = new RefreshTokenServiceImpl(
                strategyFactory,
                userRepository,
                refreshTokenScript,
                redisTemplate
        );

        rotateRefreshTokenService = new RotateRefreshTokenServiceImpl(
                jwtService,
                refreshTokenService,
                redisTemplate,
                objectMapper,
                rotateTokenScript
        );
    }

    @AfterEach
    void teardown() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("정상적인 리프레시 토큰 회전")
    void testRotateRefreshToken_Success() {
        // given - 유효한 리프레시 토큰 생성
        Authentication auth = createMockAuthentication();
        String refreshTokenId = refreshTokenService.createRefreshToken(auth);

        log.info("Created refresh token: {}", refreshTokenId);

        // when - 토큰 회전 실행
        RotateTokenDTO result = rotateRefreshTokenService.RotateRefreshToken(refreshTokenId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.newAccessToken()).isEqualTo("new-access-token");
        assertThat(result.newRefreshToken()).isNotNull();
        assertThat(result.tokenType()).isEqualTo("Cookie");
        assertThat(result.expiration()).isEqualTo(60 * 60 * 24);

        // 기존 토큰이 사용 처리되었는지 확인 (Redis에서 직접 확인)
        String tokenKey = "refreshToken:" + refreshTokenId;
        String usedStatus = redisTemplate.opsForHash().get(tokenKey, "used").toString();
        assertThat(usedStatus).isEqualTo("true");
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 회전 시 예외 발생")
    void testRotateRefreshToken_TokenNotFound() {
        // given
        String nonExistentTokenId = UUID.randomUUID().toString();

        // when & then
        assertThrows(NoSuchElementException.class, () ->
                rotateRefreshTokenService.RotateRefreshToken(nonExistentTokenId));
    }

    @Test
    @DisplayName("만료된 토큰으로 회전 시 예외 발생")
    void testRotateRefreshToken_ExpiredToken() {
        // given - 만료된 토큰을 Redis에 직접 저장
        String expiredTokenId = UUID.randomUUID().toString();
        String tokenKey = "refreshToken:" + expiredTokenId;
        long expiredTime = Instant.now().minusSeconds(3600).toEpochMilli();

        Map<String, String> tokenData = Map.of(
                "id", expiredTokenId,
                "provider", PROVIDER,
                "providerId", PROVIDER_ID,
                "userId", TEST_USER_ID,
                "used", "false",
                "createdAt", String.valueOf(Instant.now().minusSeconds(7200).toEpochMilli()),
                "expiresAt", String.valueOf(expiredTime)
        );

        redisTemplate.opsForHash().putAll(tokenKey, tokenData);
        redisTemplate.expire(tokenKey, java.time.Duration.ofDays(1));

        // when & then
        assertThrows(ExpiredTokenException.class, () ->
                rotateRefreshTokenService.RotateRefreshToken(expiredTokenId));
    }

    @Test
    @DisplayName("이미 사용된 토큰으로 회전 시 예외 발생 (토큰 재사용 감지)")
    void testRotateRefreshToken_UsedToken() {
        // given - 사용된 토큰을 Redis에 직접 저장
        String usedTokenId = UUID.randomUUID().toString();
        String tokenKey = "refreshToken:" + usedTokenId;
        long futureTime = Instant.now().plusSeconds(3600).toEpochMilli();

        Map<String, String> tokenData = Map.of(
                "id", usedTokenId,
                "provider", PROVIDER,
                "providerId", PROVIDER_ID,
                "userId", TEST_USER_ID,
                "used", "true", // 이미 사용된 토큰
                "createdAt", String.valueOf(Instant.now().toEpochMilli()),
                "expiresAt", String.valueOf(futureTime)
        );

        redisTemplate.opsForHash().putAll(tokenKey, tokenData);
        redisTemplate.expire(tokenKey, java.time.Duration.ofDays(1));

        // when & then
        assertThrows(InvalidTokenException.class, () ->
                rotateRefreshTokenService.RotateRefreshToken(usedTokenId));
    }

    @Test
    @DisplayName("토큰 회전 후 새로운 토큰이 Redis에 저장되는지 확인")
    void testRotateRefreshToken_NewTokenStoredInRedis() {
        // given
        Authentication auth = createMockAuthentication();
        String originalTokenId = refreshTokenService.createRefreshToken(auth);

        // when
        RotateTokenDTO result = rotateRefreshTokenService.RotateRefreshToken(originalTokenId);

        // then
        String newRefreshTokenId = result.newRefreshToken();
        String newTokenKey = "refreshToken:" + newRefreshTokenId;

        // 새 토큰이 Redis에 존재하는지 확인
        Boolean exists = redisTemplate.hasKey(newTokenKey);
        assertThat(exists).isTrue();

        // 새 토큰의 속성 확인
        Map<Object, Object> newTokenData = redisTemplate.opsForHash().entries(newTokenKey);
        assertThat(newTokenData.get("provider")).isEqualTo(PROVIDER);
        assertThat(newTokenData.get("providerId")).isEqualTo(PROVIDER_ID);
        assertThat(newTokenData.get("userId")).isEqualTo(TEST_USER_ID);
        assertThat(newTokenData.get("used")).isEqualTo("false");
    }

    private Authentication createMockAuthentication() {
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_BUYER")),
                Map.of("sub", PROVIDER_ID, "name", "테스트유저"),
                "sub"
        );
        return new OAuth2AuthenticationToken(oauth2User,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_BUYER")),
                PROVIDER);
    }
}