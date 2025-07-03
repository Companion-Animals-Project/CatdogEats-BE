package com.team5.catdogeats.notifications.event;

import com.team5.catdogeats.notifications.domian.Notifications;
import com.team5.catdogeats.notifications.domian.dto.NotificationDTO;
import com.team5.catdogeats.notifications.domian.dto.OrderCompletedDTO;
import com.team5.catdogeats.notifications.domian.enums.NotificationType;
import com.team5.catdogeats.notifications.repository.NotificationRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCompletedNotificationCreator implements NotificationEventCreator {
    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;


    @Override
    public void create(Object result) {
        if (!(result instanceof OrderCompletedDTO dto)) return;

        try {
            Users user = userRepository.getReferenceById(dto.userId());
            Notifications saved = notificationRepository.save(
                    Notifications.builder()
                            .users(user)
                            .notificationType(NotificationType.NEW_ORDER)
                            .title("주문이 완료되었습니다")
                            .message("주문번호: %s, 결제 금액: %,d원".formatted(dto.orderNumber(), dto.totalPrice()))
                            .isRead(false)
                            .readAt(null)
                            .build()
            );
            sendRedis(dto, saved);
        } catch (Exception e) {
            log.error("알림 생성 실패: {}", e.getMessage());
            throw e;
        }


    }

    @Override
    public NotificationType getType() {
        return NotificationType.NEW_ORDER;
    }

    private void sendRedis(OrderCompletedDTO dto, Notifications saved) {
        try {
            NotificationDTO sseDTO = new NotificationDTO(
                    saved.getTitle(),
                    saved.getMessage(),
                    saved.getNotificationType(),
                    saved.getCreatedAt()
            );
            redisTemplate.convertAndSend("notify:" + saved.getUsers().getId(), sseDTO);
        } catch (Exception e) {
            log.warn("Redis 알림 발송 실패 - 알림은 DB에 저장됨: userId={}, error={}",
                    dto.userId(), e.getMessage());
        }
    }


}
