package com.classroom.controller;

import com.classroom.model.Content;
import com.classroom.model.Course;
import com.classroom.model.CourseMembership;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.CourseMembershipRepository;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.ContentService;
import com.classroom.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseContentController {

    @Autowired
    private ContentService contentService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseMembershipRepository courseMembershipRepository;
    
    /**
     * Get current authenticated user from various authentication methods
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("No authentication found");
        }
        
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof UserDetails) {
            final String username = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + username));
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        } else if (principal instanceof String) {
            String username = (String) principal;
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + username));
        } else {
            throw new RuntimeException("Unsupported principal type: " + principal.getClass().getName());
        }
    }
    
    /**
     * Check if a user is a member of a course
     */
    private boolean isUserMemberOfCourse(User user, Course course) {
        return !courseMembershipRepository.findByUserAndCourse(user, course).isEmpty();
    }
    
    /**
     * Check if a user is a teacher of a course
     */
    private boolean isUserTeacherOfCourse(User user, Course course) {
        List<CourseMembership> memberships = courseMembershipRepository.findByUserAndCourseAndRole(user, course, UserType.TEACHER);
        return !memberships.isEmpty() || course.getTeacher().equals(user);
    }
    
    /**
     * Check if a user is a TA of a course
     */
    private boolean isUserTAOfCourse(User user, Course course) {
        List<CourseMembership> memberships = courseMembershipRepository.findByUserAndCourseAndRole(user, course, UserType.TA);
        return !memberships.isEmpty();
    }
    
    /**
     * Check if a user can manage content (teacher or TA)
     */
    private boolean canUserManageContent(User user, Course course) {
        return isUserTeacherOfCourse(user, course) || isUserTAOfCourse(user, course);
    }
    
    /**
     * Get all content for a course - this endpoint is accessible to all course members
     * But students will only see visible content
     */
    @GetMapping("/{courseId}/content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getContentByCourse(@PathVariable Long courseId) {
        try {
            System.out.println("Getting content for course ID: " + courseId);
            
            User currentUser = getCurrentUser();
            Course course = courseService.getCourseById(courseId);
            
            // Check if the user is a member of the course
            if (!isUserMemberOfCourse(currentUser, course)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not a member of this course"));
            }
            
            // If user is a teacher or TA, return all content
            List<Content> contentList;
            if (canUserManageContent(currentUser, course)) {
                contentList = contentService.getContentByCourse(course);
                System.out.println("Returning " + contentList.size() + " content items (teacher/TA view)");
            } else {
                // If user is a student, return only visible content
                contentList = contentService.getVisibleContentByCourse(course);
                System.out.println("Returning " + contentList.size() + " content items (student view)");
            }
            
            return ResponseEntity.ok(contentList);
        } catch (Exception e) {
            System.err.println("Error getting course content: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get course content: " + e.getMessage()));
        }
    }
} 