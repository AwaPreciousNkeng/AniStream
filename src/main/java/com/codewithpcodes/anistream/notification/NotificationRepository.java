package com.codewithpcodes.anistream.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

    List<Notification> findByReceiverIdAndIsReadFalse(UUID receiverId);

    long countByReceiverIdAndIsReadFalse(UUID receiverId);

    //Mark all as read for a user
    @Modifying
    @Query(value = "update Notification n set n.isRead = true " +
            "where n.receiver.id = :userId " +
            "and n.isRead = false")
    void markAllAsRead(
            @Param("userId") UUID userId
    );
}
