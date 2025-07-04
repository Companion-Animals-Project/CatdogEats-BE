package com.team5.catdogeats.admins.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Redis 인증코드 서비스 테스트")
class RedisVerificationCodeServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisVerificationCodeService redisVerificationCodeService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_CODE = "123456";
    private static final String VERIFICATION_CODE_PREFIX = "admin:verification:";
    private static final int EXPIRATION_HOURS = 1;

    @BeforeEach
    void setUp() {
        // 필드 값 설정
        ReflectionTestUtils.setField(redisVerificationCodeService, "expirationHours", EXPIRATION_HOURS);
        ReflectionTestUtils.setField(redisVerificationCodeService, "verificationCodePrefix", VERIFICATION_CODE_PREFIX);

        // RedisTemplate의 ValueOperations 모킹
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("인증코드 저장 성공")
    void saveVerificationCode_Success() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        Duration expectedDuration = Duration.ofHours(EXPIRATION_HOURS);

        // when
        redisVerificationCodeService.saveVerificationCode(TEST_EMAIL, TEST_CODE);

        // then
        verify(valueOperations).set(expectedKey, TEST_CODE, expectedDuration);
    }

    @Test
    @DisplayName("인증코드 검증 및 삭제 성공")
    void verifyAndDeleteCode_Success() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        given(valueOperations.get(expectedKey)).willReturn(TEST_CODE);

        // when
        boolean result = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, TEST_CODE);

        // then
        assertThat(result).isTrue();
        verify(valueOperations).get(expectedKey);
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 존재하지 않는 코드")
    void verifyAndDeleteCode_Fail_CodeNotExists() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        boolean result = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, TEST_CODE);

        // then
        assertThat(result).isFalse();
        verify(valueOperations).get(expectedKey);
        verify(redisTemplate, never()).delete((String) any());
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 코드 불일치")
    void verifyAndDeleteCode_Fail_CodeMismatch() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        String storedCode = "654321";
        String inputCode = "123456";

        given(valueOperations.get(expectedKey)).willReturn(storedCode);

        // when
        boolean result = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, inputCode);

        // then
        assertThat(result).isFalse();
        verify(valueOperations).get(expectedKey);
        verify(redisTemplate, never()).delete((String) any());
    }

    @Test
    @DisplayName("인증코드 존재 여부 확인 - 존재함")
    void hasVerificationCode_True() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        given(redisTemplate.hasKey(expectedKey)).willReturn(true);

        // when
        boolean result = redisVerificationCodeService.hasVerificationCode(TEST_EMAIL);

        // then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("인증코드 존재 여부 확인 - 존재하지 않음")
    void hasVerificationCode_False() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        given(redisTemplate.hasKey(expectedKey)).willReturn(false);

        // when
        boolean result = redisVerificationCodeService.hasVerificationCode(TEST_EMAIL);

        // then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("인증코드 TTL 조회 성공")
    void getVerificationCodeTTL_Success() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        long expectedTTL = 3600L; // 1시간 = 3600초

        given(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).willReturn(expectedTTL);

        // when
        long result = redisVerificationCodeService.getVerificationCodeTTL(TEST_EMAIL);

        // then
        assertThat(result).isEqualTo(expectedTTL);
        verify(redisTemplate).getExpire(expectedKey, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("인증코드 TTL 조회 - 만료된 키")
    void getVerificationCodeTTL_ExpiredKey() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        long expiredTTL = -2L; // Redis에서 존재하지 않는 키는 -2를 반환

        given(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).willReturn(expiredTTL);

        // when
        long result = redisVerificationCodeService.getVerificationCodeTTL(TEST_EMAIL);

        // then
        assertThat(result).isEqualTo(expiredTTL);
        verify(redisTemplate).getExpire(expectedKey, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("만료 시간 계산")
    void calculateExpiryTime_Success() {
        // given
        ZonedDateTime beforeCalculation = ZonedDateTime.now();

        // when
        ZonedDateTime result = redisVerificationCodeService.calculateExpiryTime();

        // then
        ZonedDateTime afterCalculation = ZonedDateTime.now().plusHours(EXPIRATION_HOURS);

        assertThat(result).isAfter(beforeCalculation.plusHours(EXPIRATION_HOURS).minusSeconds(1));
        assertThat(result).isBefore(afterCalculation.plusSeconds(1));
    }

    @Test
    @DisplayName("다양한 이메일 형식 테스트")
    void verificationCode_DifferentEmailFormats() {
        // given
        String[] testEmails = {
                "user@domain.com",
                "test.user+tag@example.org",
                "admin@subdomain.example.com",
                "special-chars_123@test-domain.co.kr"
        };

        for (String email : testEmails) {
            String expectedKey = VERIFICATION_CODE_PREFIX + email;
            given(valueOperations.get(expectedKey)).willReturn(TEST_CODE);

            // when
            boolean result = redisVerificationCodeService.verifyAndDeleteCode(email, TEST_CODE);

            // then
            assertThat(result).isTrue();
            verify(valueOperations).get(expectedKey);
            verify(redisTemplate).delete(expectedKey);
        }
    }

    @Test
    @DisplayName("Redis 키 패턴 검증")
    void redisKeyPattern_Verification() {
        // given
        String testEmail1 = "user1@test.com";
        String testEmail2 = "user2@test.com";
        String expectedKey1 = VERIFICATION_CODE_PREFIX + testEmail1;
        String expectedKey2 = VERIFICATION_CODE_PREFIX + testEmail2;

        // when
        redisVerificationCodeService.saveVerificationCode(testEmail1, "111111");
        redisVerificationCodeService.saveVerificationCode(testEmail2, "222222");

        // then
        verify(valueOperations).set(eq(expectedKey1), eq("111111"), any(Duration.class));
        verify(valueOperations).set(eq(expectedKey2), eq("222222"), any(Duration.class));

        // 키가 올바른 패턴으로 생성되었는지 확인
        assertThat(expectedKey1).startsWith(VERIFICATION_CODE_PREFIX);
        assertThat(expectedKey2).startsWith(VERIFICATION_CODE_PREFIX);
        assertThat(expectedKey1).isNotEqualTo(expectedKey2);
    }

    @Test
    @DisplayName("만료 시간 설정 검증")
    void expirationTime_Verification() {
        // given
        Duration expectedDuration = Duration.ofHours(EXPIRATION_HOURS);
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;

        // when
        redisVerificationCodeService.saveVerificationCode(TEST_EMAIL, TEST_CODE);

        // then
        verify(valueOperations).set(expectedKey, TEST_CODE, expectedDuration);
    }

    @Test
    @DisplayName("다른 만료 시간 설정 테스트")
    void differentExpirationHours_Test() {
        // given
        int customExpirationHours = 2;
        ReflectionTestUtils.setField(redisVerificationCodeService, "expirationHours", customExpirationHours);

        Duration expectedDuration = Duration.ofHours(customExpirationHours);
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;

        // when
        redisVerificationCodeService.saveVerificationCode(TEST_EMAIL, TEST_CODE);

        // then
        verify(valueOperations).set(expectedKey, TEST_CODE, expectedDuration);

        // 만료 시간 계산도 변경된 값으로 동작하는지 확인
        ZonedDateTime beforeCalculation = ZonedDateTime.now();
        ZonedDateTime result = redisVerificationCodeService.calculateExpiryTime();
        ZonedDateTime afterCalculation = ZonedDateTime.now().plusHours(customExpirationHours);

        assertThat(result).isAfter(beforeCalculation.plusHours(customExpirationHours).minusSeconds(1));
        assertThat(result).isBefore(afterCalculation.plusSeconds(1));
    }

    @Test
    @DisplayName("코드 삭제 후 재검증 시도")
    void verifyAfterDeletion_Test() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;

        // 첫 번째 검증 - 성공
        given(valueOperations.get(expectedKey)).willReturn(TEST_CODE).willReturn(null);

        // when & then
        // 첫 번째 검증 성공
        boolean firstResult = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, TEST_CODE);
        assertThat(firstResult).isTrue();

        // 두 번째 검증 실패 (이미 삭제됨)
        boolean secondResult = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, TEST_CODE);
        assertThat(secondResult).isFalse();

        // Redis 호출 검증
        verify(valueOperations, times(2)).get(expectedKey);
        verify(redisTemplate, times(1)).delete(expectedKey); // 삭제는 한 번만
    }

    @Test
    @DisplayName("특수 문자 포함 인증코드 테스트")
    void specialCharacterCodes_Test() {
        // given
        String[] specialCodes = {
                "ABC123",
                "abc123",
                "123456",
                "!@#$%^",
                "가나다라마바"
        };

        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;

        for (String code : specialCodes) {
            given(valueOperations.get(expectedKey)).willReturn(code);

            // when
            boolean result = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, code);

            // then
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("Redis 연결 실패 시나리오")
    void redisConnectionFailure_Test() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        given(valueOperations.get(expectedKey)).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, TEST_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis connection failed");
    }

    @Test
    @DisplayName("동시성 테스트 - 같은 이메일 다른 코드")
    void concurrency_SameEmailDifferentCodes() {
        // given
        String code1 = "111111";
        String code2 = "222222";
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;

        given(valueOperations.get(expectedKey)).willReturn(code1);

        // when & then
        // 올바른 코드로 검증 성공
        boolean result1 = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, code1);
        assertThat(result1).isTrue();

        // 다른 코드로 검증 실패
        boolean result2 = redisVerificationCodeService.verifyAndDeleteCode(TEST_EMAIL, code2);
        assertThat(result2).isFalse();
    }

    @Test
    @DisplayName("TTL 다양한 값 테스트")
    void variousTTLValues_Test() {
        // given
        String expectedKey = VERIFICATION_CODE_PREFIX + TEST_EMAIL;
        long[] ttlValues = {3600L, 1800L, 60L, 0L, -1L, -2L};

        for (long ttl : ttlValues) {
            given(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).willReturn(ttl);

            // when
            long result = redisVerificationCodeService.getVerificationCodeTTL(TEST_EMAIL);

            // then
            assertThat(result).isEqualTo(ttl);
        }
    }
}