package com.team5.catdogeats.notifications.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.notifications.domain.dto.NotificationResponseDTO;
import com.team5.catdogeats.notifications.domain.dto.NotificationSearchRequestDTO;
import com.team5.catdogeats.notifications.domain.mapping.NotificationReceiver;
import com.team5.catdogeats.notifications.mapper.NotificationMapper;
import com.team5.catdogeats.notifications.repository.NotificationReceiverRepository;
import com.team5.catdogeats.notifications.service.NotificationCommandService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandServiceImpl implements NotificationCommandService {
    private final NotificationReceiverRepository notificationReceiverRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @JpaTransactional
    public void markAsRead(UserPrincipal userPrincipal, String notificationId) {
        try {

            NotificationReceiver notificationReceiver = notificationReceiverRepository.findByUsersAndNotifications(userPrincipal.provider(), userPrincipal.providerId(), notificationId)
                    .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다."));

            if (!notificationReceiver.isRead()) {
                ZonedDateTime now = ZonedDateTime.now();
                notificationReceiverRepository.markAsRead(now, notificationReceiver.getUsers(), notificationReceiver.getNotifications());
            }

        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            log.error("알림 읽음 처리중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }


    @MybatisTransactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(UserPrincipal userPrincipal,
                                                          NotificationSearchRequestDTO dto,
                                                          int size
    ) {
        try {
            return notificationMapper.findNotificationsWithCursorAndReadFilter(userPrincipal.provider(),
                                                                        userPrincipal.providerId(),
                                                                        dto.cursorCreatedAt(),
                                                                        dto.cursorId(),
                                                                        size);
    } catch (Exception e) {
            log.error("알림 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("알림을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @JpaTransactional(readOnly = true)
    public Long countUnreadNotifications(UserPrincipal userPrincipal) {
        return notificationReceiverRepository.countUnreadNotificationsByUser(userPrincipal.provider(), userPrincipal.providerId());
    }
}
