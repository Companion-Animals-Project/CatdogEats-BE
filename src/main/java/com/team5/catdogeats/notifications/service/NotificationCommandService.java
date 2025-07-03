package com.team5.catdogeats.notifications.service;

public interface NotificationCommandService {
    void markAsRead(String provider, String providerId, String notificationId);
    void markAllAsRead(String provider, String providerId);
}
