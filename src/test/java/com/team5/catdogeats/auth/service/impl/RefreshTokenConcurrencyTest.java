package com.team5.catdogeats.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.chats.util.ChatSubscriber;
import com.team5.catdogeats.global.config.RedisConfig;
import com.team5.catdogeats.notifications.util.NotificationSubscriber;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(classes = {RefreshTokenServiceImpl.class, RefreshTokenConcurrencyTest.TestConfig.class,
        RedisConfig.class
})
class RefreshTokenConcurrencyTest {

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
    }

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenServiceImpl refreshTokenService;

    @Autowired
    private OAuth2ProviderStrategyFactory strategyFactory;

    private final String TEST_USER_ID = "test-user-1";


    @BeforeEach
    void setup() {
        // Redis 초기화
        redissonClient.getKeys().flushall();

        // 사용자 mock 설정만 남김
        Users testUser = Users.builder()
                .id(TEST_USER_ID)
                .name("테스트유저")
                .provider("google")
                .providerId("google-123")
                .role(Role.ROLE_BUYER)
                .build();

        when(userRepository.findByProviderAndProviderId("google", "google-123"))
                .thenReturn(Optional.of(testUser));

        OAuth2ProviderStrategy strategy = mock(OAuth2ProviderStrategy.class);
        when(strategyFactory.getStrategy("google")).thenReturn(strategy);
        when(strategy.extractProviderId(any())).thenReturn("google-123");

        refreshTokenService = new RefreshTokenServiceImpl(strategyFactory, userRepository, redissonClient);
    }

    @AfterEach
    void teardown() {
        // 테스트 후 Redis 초기화
        redissonClient.getKeys().flushall();
    }

    @Test
    @DisplayName("동시 로그인 시 토큰 개수 제한")
    void testConcurrentRefreshTokenLimit() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<String> tokenIds = Collections.synchronizedList(new ArrayList<>());

        IntStream.range(0, threadCount).forEach(i -> executor.submit(() -> {
            try {
                Authentication auth = createMockAuthentication(i);
                log.info("Thread {}: {}", Thread.currentThread().getId(), auth.getName());
                String tokenId = refreshTokenService.createRefreshToken(auth);
                log.info("Thread {}: tokenId: {}", Thread.currentThread().getId(), tokenId);
                tokenIds.add(tokenId);
            } catch (Exception e) {
                log.error("Exception: {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        }));

        latch.await();
        executor.shutdown();

        // Redis에서 실제 토큰 개수 확인
        long tokenCount = redissonClient.getScoredSortedSet("userTokens:" + TEST_USER_ID).size();
        assertTrue(tokenCount <= 3, "Redis에 저장된 토큰이 3개를 초과했습니다!");
        log.info("Redis userTokens ZSET count: " + tokenCount);
        log.info("token size: " + tokenIds.size());
        // 실제 Lua 스크립트 원자적 처리 확인
        RScoredSortedSet<String> tokenSet = redissonClient.getScoredSortedSet("userTokens:" + TEST_USER_ID);
        List<String> tokensInRedis = tokenSet.stream().toList();
        assertThat(tokensInRedis).hasSize(3);
    }

    private Authentication createMockAuthentication(int i) {
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_BUYER")),
                Map.of("sub", "google-123", "name", "테스트유저" + i),
                "sub"
        );
        return new OAuth2AuthenticationToken(oauth2User,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_BUYER")),
                "google");
    }
}
