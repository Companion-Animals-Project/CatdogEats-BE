package com.team5.catdogeats.notifications.repository;


import com.team5.catdogeats.notifications.domian.Notifications;
import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notifications, String> {

    @Query("SELECT n FROM Notifications n WHERE n.id = :notificationId AND n.users.id = :userId")
    Optional<Notifications> findByIdAndUserId(String notificationId, String userId);

    @Modifying
    @Query("UPDATE Notifications n SET n.isRead = true, n.readAt = :readAt  WHERE n.id = :id")
    void markAsRead(@Param("readAt") ZonedDateTime now, @Param("id") String id);

    @Modifying
    @Query("UPDATE Notifications n SET n.isRead = true WHERE n.users = :users")
    void markAllAsRead(@Param("users") Users users);
}
