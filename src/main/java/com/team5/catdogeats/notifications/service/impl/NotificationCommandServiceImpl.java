//package com.team5.catdogeats.notifications.service.impl;
//
//import com.team5.catdogeats.chats.service.UserIdCacheService;
//import com.team5.catdogeats.global.annotation.JpaTransactional;
//import com.team5.catdogeats.notifications.domian.Notifications;
//import com.team5.catdogeats.notifications.domian.mapping.NotificationReceiver;
//import com.team5.catdogeats.notifications.repository.NotificationReceiverRepository;
//import com.team5.catdogeats.notifications.repository.NotificationRepository;
//import com.team5.catdogeats.notifications.service.NotificationCommandService;
//import com.team5.catdogeats.users.domain.Users;
//import com.team5.catdogeats.users.repository.UserRepository;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.ZonedDateTime;
//import java.util.NoSuchElementException;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class NotificationCommandServiceImpl implements NotificationCommandService {
//    private final NotificationRepository notificationRepository;
//    private final NotificationReceiverRepository notificationReceiverRepository;
//    private final UserIdCacheService userIdCacheService;
//    private final UserRepository userRepository;
//
//
//
//    @Override
//    @JpaTransactional
//
//    public void markAsRead(String provider, String providerId, String notificationId) {
//        try {
//            Users users = userRepository.findByProviderAndProviderId(provider, providerId)
//                    .orElseThrow(() -> new NoSuchElementException("유저 정보를 찾을 수 없습니다."));
//            Notifications notifications = notificationRepository.findById(notificationId)
//                    .orElseThrow(() -> new NoSuchElementException("알림 정보를 찾을 수 없습니다."));
//            NotificationReceiver notification = notificationReceiverRepository.findByUsersAndNotifications(users, notifications)
//                    .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다."));
//
//            if (!notification.isRead()) {
//                ZonedDateTime now = ZonedDateTime.now();
//                notificationRepository.markAsRead(now, notification.getId());
//            }
//
//        } catch (NoSuchElementException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("알림 읽음 처리중 오류 발생: {}", e.getMessage());
//            throw e;
//        }
//    }
//
//    @Override
//    @JpaTransactional
//    public void markAllAsRead(String provider, String providerId) {
//        try {
//            Users users = userRepository.findByProviderAndProviderId(provider, providerId)
//                    .orElseThrow(() -> new NoSuchElementException("유저 정보를 찾을 수 없습니다."));
//            notificationRepository.markAllAsRead(users);
//        } catch (NoSuchElementException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("전체 읽음 처리중 오류 발생: {}", e.getMessage());
//            throw e;
//        }
//
//
//    }
//
//    private String getUserId(String provider, String providerId) {
//        String userId = userIdCacheService.getCachedUserId(provider, providerId);
//        if (userId == null) {
//            userIdCacheService.cacheUserIdAndRole(provider, providerId);
//            userId = userIdCacheService.getCachedUserId(provider, providerId);
//        }
//        return userId;
//    }
//}
