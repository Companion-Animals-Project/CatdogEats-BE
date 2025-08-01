package com.team5.catdogeats.global.util;

import java.time.ZoneId;

/**
 * 시간대 관련 상수를 정의하는 클래스
 *
 * @since 2025-08-01
 * @author TaeHoon96
 */
public final class TimeConstants {

    // private 생성자로 인스턴스화 방지
    private TimeConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /**
     * 서울 시간대 (Asia/Seoul)
     * 한국 표준시(KST) UTC+9
     */
    public static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    /**
     * UTC 시간대
     * 국제 표준시
     */
    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    /**
     * 시스템 기본 시간대
     * 서버 환경에 따라 달라질 수 있으므로 주의 필요
     */
    public static final ZoneId SYSTEM_DEFAULT_ZONE_ID = ZoneId.systemDefault();
}