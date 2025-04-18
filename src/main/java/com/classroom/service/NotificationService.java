package com.classroom.service;

import com.classroom.model.Content;
import com.classroom.model.Notification;
import com.classroom.model.TeamMembership;
import com.classroom.model.User;
import com.classroom.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Get all notifications for a user
     */
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUser(user);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user);
    }

    /**
     * Count unread notifications for a user
     */
    public long countUnreadNotificationsForUser(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllNotificationsAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(user);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * Create a team invitation notification
     */
    @Transactional
    public Notification createTeamInvitationNotification(User user, TeamMembership teamMembership) {
        Notification notification = Notification.builder()
                .user(user)
                .message("You have been invited to join team '" + teamMembership.getTeam().getName() + "' for assignment '" 
                       + teamMembership.getTeam().getAssignment().getAssignment().getTitle() + "'")
                .notificationType(Notification.NotificationType.TEAM_INVITATION)
                .relatedId(teamMembership.getId())
                .isRead(false)
                .build();
        
        return notificationRepository.save(notification);
    }

    /**
     * Create a course content added notification
     */
    @Transactional
    public void createCourseContentNotification(Content content, List<User> courseStudents) {
        String message = "New " + content.getContentType().toString().toLowerCase() + 
                         " added to course '" + content.getCourse().getName() + "': " + content.getTitle();
        
        for (User student : courseStudents) {
            Notification notification = Notification.builder()
                    .user(student)
                    .message(message)
                    .notificationType(Notification.NotificationType.COURSE_CONTENT_ADDED)
                    .relatedId(content.getId())
                    .isRead(false)
                    .build();
            
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
} 