package com.classroom.repository;

import com.classroom.model.Notification;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find all notifications for a specific user
     */
    List<Notification> findByRecipient(User recipient);
    
    /**
     * Find all unread notifications for a specific user
     */
    List<Notification> findByRecipientAndIsReadFalse(User recipient);
    
    /**
     * Find all notifications for a specific user and type
     */
    List<Notification> findByRecipientAndType(User recipient, Notification.NotificationType type);
    
    /**
     * Find all notifications for a user ordered by creation date (most recent first)
     */
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    
    /**
     * Count unread notifications for a user
     */
    long countByRecipientAndIsReadFalse(User recipient);
} 