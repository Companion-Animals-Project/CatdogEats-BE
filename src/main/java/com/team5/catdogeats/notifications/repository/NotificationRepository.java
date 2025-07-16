package com.team5.catdogeats.notifications.repository;


import com.team5.catdogeats.notifications.domain.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notifications, String> {

    Optional<Notifications> findById(String id);
//
//    @Query(value = """
//    SELECT n FROM Notifications n
//    WHERE n.users.id = :userId
//    ORDER BY n.createdAt DESC
//    """,
//            countQuery = "SELECT COUNT(n) FROM Notifications n WHERE n.users.id = :userId")
//    Page<Notifications> findByUserId(@Param("userId") String userId, Pageable pageable);
//
//    @Query("SELECT COUNT(n) FROM Notifications n WHERE n.users.id = :userId AND n.isRead = false")
//    long countUnreadByUserId(@Param("userId") String userId);
//
//    @Query("SELECT n FROM Notifications n WHERE n.id = :notificationId AND n.users.id = :userId")
//    Optional<Notifications> findByIdAndUserId(String notificationId, String userId);
//
//    @Modifying
//    @Query("UPDATE Notifications n SET n.isRead = true, n.readAt = :readAt  WHERE n.id = :id")
//    void markAsRead(@Param("readAt") ZonedDateTime now, @Param("id") String id);
//
//    @Modifying
//    @Query("UPDATE Notifications n SET n.isRead = true WHERE n.users = :users")
//    void markAllAsRead(@Param("users") Users users);
}
