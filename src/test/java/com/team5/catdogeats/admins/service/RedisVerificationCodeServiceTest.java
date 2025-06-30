package com.team5.catdogeats.admins.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 인증코드 서비스 테스트")
class RedisVerificationCodeServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisVerificationCodeService redisVerificationCodeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(redisVerificationCodeService, "expirationHours", 1);
    }

    @Test
    @DisplayName("인증코드 저장 성공")
    void saveVerificationCode_Success() {
        // given
        String email = "test@example.com";
        String code = "123456";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        redisVerificationCodeService.saveVerificationCode(email, code);

        // then
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(expectedKey), eq(code), durationCaptor.capture());

        Duration capturedDuration = durationCaptor.getValue();
        assertThat(capturedDuration.toHours()).isEqualTo(1);
    }

    @Test
    @DisplayName("인증코드 검증 및 삭제 성공")
    void verifyAndDeleteCode_Success() {
        // given
        String email = "test@example.com";
        String inputCode = "123456";
        String storedCode = "123456";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(storedCode);

        // when
        boolean result = redisVerificationCodeService.verifyAndDeleteCode(email, inputCode);

        // then
        assertThat(result).isTrue();
        verify(valueOperations).get(expectedKey);
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 코드가 존재하지 않음")
    void verifyAndDeleteCode_Fail_CodeNotExists() {
        // given
        String email = "test@example.com";
        String inputCode = "123456";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // when
        boolean result = redisVerificationCodeService.verifyAndDeleteCode(email, inputCode);

        // then
        assertThat(result).isFalse();
        verify(valueOperations).get(expectedKey);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 코드 불일치")
    void verifyAndDeleteCode_Fail_CodeMismatch() {
        // given
        String email = "test@example.com";
        String inputCode = "123456";
        String storedCode = "654321";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(storedCode);

        // when
        boolean result = redisVerificationCodeService.verifyAndDeleteCode(email, inputCode);

        // then
        assertThat(result).isFalse();
        verify(valueOperations).get(expectedKey);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("인증코드 존재 여부 확인 - 존재함")
    void hasVerificationCode_True() {
        // given
        String email = "test@example.com";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // when
        boolean result = redisVerificationCodeService.hasVerificationCode(email);

        // then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("인증코드 존재 여부 확인 - 존재하지 않음")
    void hasVerificationCode_False() {
        // given
        String email = "test@example.com";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // when
        boolean result = redisVerificationCodeService.hasVerificationCode(email);

        // then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("인증코드 TTL 조회")
    void getVerificationCodeTTL() {
        // given
        String email = "test@example.com";
        String expectedKey = "admin:verification:" + email;
        long expectedTTL = 3600L; // 1시간

        when(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).thenReturn(expectedTTL);

        // when
        long result = redisVerificationCodeService.getVerificationCodeTTL(email);

        // then
        assertThat(result).isEqualTo(expectedTTL);
        verify(redisTemplate).getExpire(expectedKey, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("만료 시간 계산")
    void calculateExpiryTime() {
        // given
        ZonedDateTime beforeCalculation = ZonedDateTime.now();

        // when
        ZonedDateTime result = redisVerificationCodeService.calculateExpiryTime();

        // then
        ZonedDateTime afterCalculation = ZonedDateTime.now().plusHours(1);

        assertThat(result).isAfter(beforeCalculation);
        assertThat(result).isBefore(afterCalculation.plusMinutes(1)); // 1분 오차 허용
    }

    @Test
    @DisplayName("여러 이메일의 인증코드 독립적 관리")
    void multipleEmailsIndependentManagement() {
        // given
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String code1 = "123456";
        String code2 = "654321";
        String key1 = "admin:verification:" + email1;
        String key2 = "admin:verification:" + email2;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        redisVerificationCodeService.saveVerificationCode(email1, code1);
        redisVerificationCodeService.saveVerificationCode(email2, code2);

        // then
        verify(valueOperations).set(eq(key1), eq(code1), any(Duration.class));
        verify(valueOperations).set(eq(key2), eq(code2), any(Duration.class));
    }

    @Test
    @DisplayName("인증코드 키 패턴 검증")
    void verificationCodeKeyPattern() {
        // given
        String email = "test@example.com";
        String code = "123456";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        redisVerificationCodeService.saveVerificationCode(email, code);

        // then
        verify(valueOperations).set(eq(expectedKey), eq(code), any(Duration.class));
    }

    @Test
    @DisplayName("TTL이 0 이하인 경우 처리")
    void getVerificationCodeTTL_Expired() {
        // given
        String email = "expired@example.com";
        String expectedKey = "admin:verification:" + email;
        long expiredTTL = -1L; // 만료된 키

        when(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).thenReturn(expiredTTL);

        // when
        long result = redisVerificationCodeService.getVerificationCodeTTL(email);

        // then
        assertThat(result).isEqualTo(expiredTTL);
        verify(redisTemplate).getExpire(expectedKey, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("동일한 이메일로 여러 번 인증코드 저장 시 덮어쓰기")
    void saveVerificationCode_Overwrite() {
        // given
        String email = "test@example.com";
        String firstCode = "123456";
        String secondCode = "654321";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        redisVerificationCodeService.saveVerificationCode(email, firstCode);
        redisVerificationCodeService.saveVerificationCode(email, secondCode);

        // then
        verify(valueOperations, times(2)).set(eq(expectedKey), anyString(), any(Duration.class));
        // 첫 번째 호출
        verify(valueOperations).set(eq(expectedKey), eq(firstCode), any(Duration.class));
        // 두 번째 호출 (덮어쓰기)
        verify(valueOperations).set(eq(expectedKey), eq(secondCode), any(Duration.class));
    }

    @Test
    @DisplayName("인증 성공 후 코드 삭제 확인")
    void verifyAndDeleteCode_DeleteAfterSuccess() {
        // given
        String email = "test@example.com";
        String code = "123456";
        String expectedKey = "admin:verification:" + email;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(code);

        // when
        boolean firstVerification = redisVerificationCodeService.verifyAndDeleteCode(email, code);

        // 삭제 후 다시 시도
        when(valueOperations.get(expectedKey)).thenReturn(null);
        boolean secondVerification = redisVerificationCodeService.verifyAndDeleteCode(email, code);

        // then
        assertThat(firstVerification).isTrue();
        assertThat(secondVerification).isFalse(); // 이미 삭제되어 실패

        verify(redisTemplate, times(1)).delete(expectedKey); // 한 번만 삭제 호출
        verify(valueOperations, times(2)).get(expectedKey); // 두 번 조회 호출
    }

    @Test
    @DisplayName("빈 이메일이나 null 코드 처리")
    void handleEmptyOrNullValues() {
        // given
        String emptyEmail = "";
        String nullCode = null;
        String validCode = "123456";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when & then - 빈 이메일
        assertThatCode(() -> redisVerificationCodeService.saveVerificationCode(emptyEmail, validCode))
                .doesNotThrowAnyException();

        // when & then - null 코드 (실제로는 Redis가 null을 문자열 "null"로 저장할 수 있음)
        assertThatCode(() -> redisVerificationCodeService.saveVerificationCode("test@example.com", nullCode))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("만료 시간 설정 변경 테스트")
    void customExpirationTime() {
        // given
        ReflectionTestUtils.setField(redisVerificationCodeService, "expirationHours", 2);
        String email = "test@example.com";
        String code = "123456";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        redisVerificationCodeService.saveVerificationCode(email, code);

        // then
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(anyString(), anyString(), durationCaptor.capture());

        Duration capturedDuration = durationCaptor.getValue();
        assertThat(capturedDuration.toHours()).isEqualTo(2);
    }
}