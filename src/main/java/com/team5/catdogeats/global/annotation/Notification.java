package com.team5.catdogeats.global.annotation;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Notification {
    NotificationType type();
}
