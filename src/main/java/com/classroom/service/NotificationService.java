package com.classroom.service;

import com.classroom.model.Notification;
import com.classroom.model.Notification.NotificationType;
import com.classroom.model.User;
import com.classroom.model.Content;
import com.classroom.model.TeamMembership;
import com.classroom.model.Team;
import com.classroom.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create a new notification
     */
    @Transactional
    public Notification createNotification(User recipient, String message, NotificationType type, Long relatedEntityId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .build();
        
        return notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByRecipientAndIsReadFalse(user);
    }

    /**
     * Count unread notifications for a user
     */
    public long countUnreadNotificationsForUser(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    /**
     * Mark all notifications for a user as read
     */
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndIsReadFalse(user);
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
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

    /**
     * Create a notification for a new submission
     */
    @Transactional
    public Notification notifyNewSubmission(User ta, String submitterName, String assignmentTitle, Long submissionId) {
        String message = submitterName + " submitted " + assignmentTitle + " for evaluation";
        return createNotification(ta, message, NotificationType.NEW_SUBMISSION, submissionId);
    }

    /**
     * Create a notification for a graded submission
     */
    @Transactional
    public Notification notifySubmissionGraded(User student, String assignmentTitle, Long submissionId) {
        String message = "Your submission for " + assignmentTitle + " has been graded";
        return createNotification(student, message, NotificationType.SUBMISSION_GRADED, submissionId);
    }

    /**
     * Create a notification for a graded submission
     */
    @Transactional
    public Notification notifyGradedSubmission(User student, String assignmentTitle, Integer score) {
        String message = "Your submission for " + assignmentTitle + " has been graded with score: " + score;
        return createNotification(student, message, NotificationType.SUBMISSION_GRADED, null);
    }

    /**
     * Create notifications for new course content
     */
    @Transactional
    public List<Notification> createCourseContentNotification(Content content, List<User> recipients) {
        List<Notification> notifications = new ArrayList<>();
        String courseName = content.getCourse().getName();
        String contentTitle = content.getTitle();
        String message = "New " + content.getContentType().toString().toLowerCase() + " in " + courseName + ": " + contentTitle;
        
        for (User recipient : recipients) {
            Notification notification = createNotification(
                recipient, 
                message, 
                NotificationType.COURSE_UPDATE, 
                content.getId()
            );
            notifications.add(notification);
        }
        
        return notifications;
    }

    /**
     * Create a notification for team invitation
     */
    @Transactional
    public Notification createTeamInvitationNotification(User user, TeamMembership membership) {
        Team team = membership.getTeam();
        String message = "You have been invited to join team " + team.getName() + " for " + team.getAssignment().getTitle();
        return createNotification(
            user,
            message,
            NotificationType.TEAM_INVITATION,
            membership.getId()
        );
    }
} 