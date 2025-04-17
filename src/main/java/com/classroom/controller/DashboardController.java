package com.classroom.controller;

import com.classroom.model.Course;
import com.classroom.model.CourseMembership;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.CourseMembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private CourseMembershipService courseMembershipService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomAuthenticationConverter authConverter;
    
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

    @GetMapping("/dashboard/student")
    public String studentDashboard(Model model) {
        User currentUser = getCurrentUser();
        
        // Get all courses the student is a member of
        List<CourseMembership> memberships = courseMembershipService.getMembershipsByUser(currentUser);
        
        // Extract courses from memberships where the user role is STUDENT
        List<Course> courses = memberships.stream()
                .filter(m -> m.getRole() == UserType.STUDENT)
                .map(CourseMembership::getCourse)
                .collect(Collectors.toList());
        
        // Add courses and current user to the model
        model.addAttribute("courses", courses);
        model.addAttribute("currentUser", currentUser);
        
        return "student"; // looks for student.html in /resources/templates
    }

    @GetMapping("/dashboard/teacher")
    public String teacherDashboard() {
        return "teacher";
    }

    @GetMapping("/dashboard/ta")
    public String taDashboard() {
        return "ta";
    }

    @GetMapping("/test-course-creation")
    public String testCourseCreation() {
        return "test-course-creation";
    }

}
