package com.team5.catdogeats.notifications.event;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationRegistryCreator {
    private final Map<NotificationType, NotificationEventCreator> creatorMap;

    public NotificationRegistryCreator(List<NotificationEventCreator> creators) {
        this.creatorMap = creators.stream()
                .collect(Collectors.toUnmodifiableMap(NotificationEventCreator::getType, Function.identity()));
    }

    public NotificationEventCreator getCreator(NotificationType type) {
        NotificationEventCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("지원하지 않는 알림 타입입니다: " + type);
        }
        return creator;    }

}
