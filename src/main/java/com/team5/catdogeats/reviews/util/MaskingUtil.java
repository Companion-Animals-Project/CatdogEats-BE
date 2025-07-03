package com.team5.catdogeats.reviews.util;

public class MaskingUtil {
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        // 이름 2글자 이상부터 masking 처리
        if (name.length() > 1) {
            return name.charAt(0) +
                    "*".repeat(name.length() - 1);
        }
        return name;
    }
}
