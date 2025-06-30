package com.team5.catdogeats.admins.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 인증코드 관리 서비스
 * 인증코드의 생성, 검증, 만료 관리를 Redis TTL로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVerificationCodeService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${admin.invitation.expiration-hours:1}")
    private int expirationHours;

    // Redis 키 패턴
    private static final String VERIFICATION_CODE_PREFIX = "admin:verification:";


    /**
     * 인증코드 저장
     */
    public void saveVerificationCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofHours(expirationHours));
        log.info("인증코드 저장: email={}, expiration={}시간", email, expirationHours);
    }


    /**
     * 인증코드 검증 및 삭제
     */
    public boolean verifyAndDeleteCode(String email, String inputCode) {
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("인증코드가 존재하지 않거나 만료됨: email={}", email);
            return false;
        }

        if (!storedCode.equals(inputCode)) {
            log.warn("인증코드 불일치: email={}", email);
            return false;
        }

        // 인증 성공 시 코드 삭제
        redisTemplate.delete(key);
        log.info("인증코드 검증 성공 및 삭제: email={}", email);
        return true;
    }


    /**
     * 인증코드 존재 여부 확인 (직원 계정 상태 정보 통계에서 사용 )
     */
    public boolean hasVerificationCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return redisTemplate.hasKey(key);
    }



    /**
     * 인증코드 TTL 조회 (남은 시간 확인용)
     */
    public long getVerificationCodeTTL(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }


    /**
     * 만료 시간 계산 (응답용)
     */
    public ZonedDateTime calculateExpiryTime() {
        return ZonedDateTime.now().plusHours(expirationHours);
    }
}
