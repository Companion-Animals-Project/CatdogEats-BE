package com.team5.catdogeats.notifications.repository;

import com.team5.catdogeats.notifications.domian.Notifications;
import com.team5.catdogeats.notifications.domian.mapping.NotificationReceiver;
import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, String> {

    Optional<NotificationReceiver> findByUsersAndNotifications(Users users, Notifications notifications);
}
