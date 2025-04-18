package com.classroom.controller;

import com.classroom.model.Notification;
import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import com.classroom.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Helper method to get current user
     */
    private User getCurrentUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Get all notifications for the current user
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread notifications for the current user
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(currentUser);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get count of unread notifications
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(@AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        long count = notificationService.countUnreadNotificationsForUser(currentUser);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark a notification as read
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark all notifications as read
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllNotificationsAsRead(@AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        notificationService.markAllNotificationsAsRead(currentUser);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }
} 