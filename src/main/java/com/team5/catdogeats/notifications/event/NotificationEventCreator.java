package com.team5.catdogeats.notifications.event;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;

public interface NotificationEventCreator {
    void create(Object result);

    NotificationType getType();
}
