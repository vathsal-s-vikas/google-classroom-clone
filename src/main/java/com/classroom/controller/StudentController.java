package com.classroom.controller;

import com.classroom.model.Course;
import com.classroom.model.CourseMembership;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.CourseMembershipService;
import com.classroom.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseMembershipService courseMembershipService;
    
    @Autowired
    private CustomAuthenticationConverter authConverter;
    
    @Autowired
    private UserRepository userRepository;
    
    private User getCurrentUser() {
        // First convert the authentication if needed
        authConverter.convertAuthentication();
        
        // Get the updated authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof UserDetails) {
            // If it's a regular UserDetails, get the username and fetch the user from repository
            final String username = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else if (principal instanceof String) {
            // If it's just a username string
            final String username = (String) principal;
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else {
            throw new RuntimeException("Unsupported principal type: " + principal.getClass().getName());
        }
    }
    
    @PostMapping("/join-course")
    @Transactional
    public ResponseEntity<?> joinCourse(@RequestBody Map<String, String> requestData) {
        try {
            String inviteCode = requestData.get("inviteCode");
            if (inviteCode == null || inviteCode.isEmpty()) {
                return ResponseEntity.badRequest().body("Invite code is required");
            }
            
            User currentUser = getCurrentUser();
            
            // Find the course by invite code
            Course course = courseService.getCourseByInviteCode(inviteCode);
            if (course == null) {
                return ResponseEntity.badRequest().body("Invalid invite code");
            }
            
            // Check if user is already a member of this course
            boolean alreadyMember = courseMembershipService.getMembershipsByUser(currentUser)
                    .stream()
                    .anyMatch(membership -> membership.getCourse().getId().equals(course.getId()));
                    
            if (alreadyMember) {
                return ResponseEntity.badRequest().body("You are already a member of this course");
            }
            
            // Create a new membership
            CourseMembership membership = new CourseMembership();
            membership.setCourse(course);
            membership.setUser(currentUser);
            membership.setRole(UserType.STUDENT);
            membership.setJoinedAt(LocalDateTime.now());
            
            // Save the membership
            courseMembershipService.addMembership(membership);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Successfully joined course: " + course.getName(), 
                "courseId", course.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error joining course: " + e.getMessage());
        }
    }
} 