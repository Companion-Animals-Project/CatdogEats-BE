package com.team5.catdogeats.notifications.event;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.annotation.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotificationAspect {
    private final NotificationRegistryCreator registryCreator;

    @Async(value = "SSE")
    @AfterReturning(pointcut = "@annotation(notification)", returning = "result")
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(JoinPoint jp, Notification notification, Object result) {
        try {
            NotificationEventCreator creator = registryCreator.getCreator(notification.type());
            creator.create(result);
        } catch (Exception e) {
            log.error("알림 처리 중 오류 발생: method={}, type={}, error={}",
                    jp.getSignature().getName(), notification.type(), e.getMessage(), e);
        }
    }
}
