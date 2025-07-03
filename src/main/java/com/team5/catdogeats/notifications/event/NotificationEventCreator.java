package com.team5.catdogeats.notifications.event;

import com.team5.catdogeats.notifications.domian.enums.NotificationType;

public interface NotificationEventCreator {
    void create(Object result);

    NotificationType getType();
}
