package com.team5.catdogeats.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.chats.util.ChatSubscriber;
import com.team5.catdogeats.global.config.RedisScriptConfig;
import com.team5.catdogeats.notifications.util.NotificationSubscriber;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
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

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(classes = {RefreshTokenConcurrencyTest.TestConfig.class, RedisScriptConfig.class})
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

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
            config.setPassword("0000");
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet(); // 초기화
            return factory;        }

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
    RedissonClient redissonClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2ProviderStrategyFactory strategyFactory;

    // 테스트에서는 서비스 인스턴스를 직접 생성(명시적 DI)
    private RefreshTokenServiceImpl refreshTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisScript<String> refreshTokenScript;

    private final String TEST_USER_ID = "test-user-1";

    @BeforeEach
    void setup() {
        // Redis 초기화 (테스트 전)
        redisTemplate.<Object>execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });

        // 사용자 mock 설정
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

        refreshTokenService = new RefreshTokenServiceImpl(
                strategyFactory,
                userRepository,
                refreshTokenScript,
                redisTemplate
        );
    }

    @AfterEach
    void teardown() {
        // Redis 초기화 (테스트 후)
        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("리프레시 토큰 생성")
    void testCreateRefreshToken() {
        Authentication auth = createMockAuthentication(0);
        String tokenId = refreshTokenService.createRefreshToken(auth);
        log.info("tokenId: {}", tokenId);
        assertThat(tokenId).isNotNull();
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
                log.error("Exception: {}", e.getMessage(), e);
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
