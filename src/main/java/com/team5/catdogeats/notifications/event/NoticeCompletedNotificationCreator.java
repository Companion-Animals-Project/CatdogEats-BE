    package com.team5.catdogeats.notifications.event;

    import com.team5.catdogeats.notifications.domain.Notifications;
    import com.team5.catdogeats.notifications.domain.dto.NoticeCompletedDTO;
    import com.team5.catdogeats.notifications.domain.dto.NotificationDTO;
    import com.team5.catdogeats.notifications.domain.enums.NotificationType;
    import com.team5.catdogeats.notifications.domain.mapping.NotificationReceiver;
    import com.team5.catdogeats.notifications.repository.NotificationReceiverRepository;
    import com.team5.catdogeats.notifications.repository.NotificationRepository;
    import com.team5.catdogeats.notifications.service.NotificationService;
    import com.team5.catdogeats.users.domain.Users;
    import com.team5.catdogeats.users.repository.UserRepository;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.stereotype.Service;

    import java.util.List;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class NoticeCompletedNotificationCreator implements NotificationEventCreator{
        private final NotificationRepository notificationRepository;
        private final NotificationReceiverRepository notificationReceiverRepository;
        private final UserRepository userRepository;
        private final RedisTemplate<String, Object> redisTemplate;
        private final NotificationService notificationService;

        @Override
        public void create(Object result) {
            if (!(result instanceof NoticeCompletedDTO dto)) return;

            Notifications notice = Notifications.builder()
                    .title(dto.title())
                    .message(dto.message())
                    .notificationType(NotificationType.NOTICE)
                    .build();
            notificationRepository.save(notice);

            List<Users> allUsers = userRepository.findAll();
            List<NotificationReceiver> receivers = allUsers.stream()
                    .map(user -> NotificationReceiver.builder()
                            .notifications(notice)
                            .users(user)
                            .isRead(false)
                            .build())
                    .toList();
            notificationReceiverRepository.saveAll(receivers);
            allUsers.forEach(user -> {
                    try{
                        notificationService.sendNotification(user.getProvider(), user.getProviderId(), dto);
                    } catch (Exception e) {
                        log.warn("사용자 {}에게 알림 발송 실패: {}", user.getId(), e.getMessage());
                    }
            });
        }

        @Override
        public NotificationType getType() {
            return NotificationType.NOTICE;
        }

        private void sendRedis(String userId, Notifications saved) {
            try {
                NotificationDTO sseDTO = new NotificationDTO(
                        saved.getTitle(),
                        saved.getMessage(),
                        saved.getNotificationType(),
                        saved.getCreatedAt()
                );
                // notify:USER_ID 채널로 보냄
                redisTemplate.convertAndSend("notify:" + userId, sseDTO);
            } catch (Exception e) {
                log.warn("Redis 알림 발송 실패 - 알림은 DB에 저장됨:  error={}", e.getMessage());
            }
        }

    }
