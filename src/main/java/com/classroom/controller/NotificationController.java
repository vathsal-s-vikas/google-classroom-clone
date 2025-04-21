package com.classroom.controller;

import com.classroom.model.Notification;
import com.classroom.model.User;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.NotificationService;
import com.classroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * Get all notifications for the current user
     */
    @GetMapping
    public String getAllNotifications(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
        model.addAttribute("notifications", notifications);
        
        return "notifications/list";
    }

    /**
     * Get unread notifications count for the current user (AJAX endpoint)
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount() {
        User currentUser = getCurrentUser();
        long count = notificationService.countUnreadNotificationsForUser(currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        return response;
    }

    /**
     * Get recent unread notifications (AJAX endpoint)
     */
    @GetMapping("/recent-unread")
    @ResponseBody
    public List<Notification> getRecentUnread() {
        User currentUser = getCurrentUser();
        return notificationService.getUnreadNotificationsForUser(currentUser);
    }

    /**
     * Mark a notification as read
     */
    @PostMapping("/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        User currentUser = getCurrentUser();
        notificationService.markAllAsRead(currentUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userService.getUserByEmail(username);
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oauth2User.getAttributes();
            String email = (String) attributes.get("email");
            return userService.getUserByEmail(email);
        } else if (principal instanceof String) {
            return userService.getUserByEmail((String) principal);
        }
        
        return null;
    }
} 