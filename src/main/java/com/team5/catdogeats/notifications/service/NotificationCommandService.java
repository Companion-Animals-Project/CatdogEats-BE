package com.team5.catdogeats.notifications.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.notifications.domain.dto.NotificationResponseDTO;
import com.team5.catdogeats.notifications.domain.dto.NotificationSearchRequestDTO;

import java.util.List;

public interface NotificationCommandService {
    void markAsRead(UserPrincipal userPrincipal, String notificationId);
    List<NotificationResponseDTO> getNotifications(UserPrincipal userPrincipal,
                                                   NotificationSearchRequestDTO dto,
                                                   int size);
    Long countUnreadNotifications(UserPrincipal userPrincipal);
}
