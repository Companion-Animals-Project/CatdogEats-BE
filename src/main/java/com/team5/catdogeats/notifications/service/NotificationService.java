package com.team5.catdogeats.notifications.service;

public interface NotificationService {
    void sendNotification(String provider, String providerId, String message);
}
