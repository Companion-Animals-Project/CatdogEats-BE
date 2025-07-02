package com.team5.catdogeats.reviews.util;

public class MaskingUtil {
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        // 이름 2글자 이상부터 masking 처리
        if (name.length() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append(name.charAt(0));
            for (int i = 1; i < name.length(); i++) sb.append("*");
            return sb.toString();
        }
        return name;
    }
}
