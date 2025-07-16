package com.team5.catdogeats.notifications.repository;

import com.team5.catdogeats.notifications.domain.Notifications;
import com.team5.catdogeats.notifications.domain.mapping.NotificationReceiver;
import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, String> {

    @Query("""
        SELECT n\s
        FROM NotificationReceiver n
        WHERE n.notifications.id = :notificationId AND
             n.users = (SELECT u FROM Users u WHERE u.provider = :provider AND u.providerId = :providerId)
    """)
    Optional<NotificationReceiver> findByUsersAndNotifications(@Param("provider") String provider,
                                                               @Param("providerId") String providerId,
                                                               @Param("notificationId") String notificationId);

    @Modifying
    @Query("""
        UPDATE NotificationReceiver n
        SET n.isRead = true, n.readAt = :readAt
        WHERE n.users = :users AND n.notifications = :notifications
    """)
    void markAsRead(@Param("readAt")ZonedDateTime readAt,
                    @Param("users") Users users,
                    @Param("notifications") Notifications notifications);

    @Query("""
        SELECT COUNT(nr)
        FROM NotificationReceiver nr
        WHERE nr.users = (SELECT u FROM Users u WHERE u.provider = :provider AND u.providerId = :providerId)
        AND nr.isRead = false
    """)
    Long countUnreadNotificationsByUser(@Param("provider") String provider,
                                        @Param("providerId") String providerId);
}
