package com.classroom.repository;

import com.classroom.model.Notification;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find all notifications for a specific user
     */
    List<Notification> findByUser(User user);
    
    /**
     * Find all unread notifications for a specific user
     */
    List<Notification> findByUserAndIsReadFalse(User user);
    
    /**
     * Find all notifications for a specific user and type
     */
    List<Notification> findByUserAndNotificationType(User user, Notification.NotificationType notificationType);
    
    /**
     * Count unread notifications for a user
     */
    long countByUserAndIsReadFalse(User user);
    
    /**
     * Find team invitation notifications for a specific user and relatedId
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notificationType = 'TEAM_INVITATION' AND n.relatedId = :teamMembershipId")
    List<Notification> findTeamInvitationByUserAndTeamMembershipId(@Param("user") User user, @Param("teamMembershipId") Long teamMembershipId);
} 