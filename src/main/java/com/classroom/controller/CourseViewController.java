package com.classroom.controller;

import com.classroom.dto.CourseDTO;
import com.classroom.model.*;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentService;
import com.classroom.service.CourseMembershipService;
import com.classroom.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CourseViewController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseMembershipService courseMembershipService;
    
    @Autowired
    private AssignmentService assignmentService;
    
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

    @GetMapping("/teacher/course/{courseId}")
    public String teacherCourseView(@PathVariable Long courseId, Model model) {
        User currentUser = getCurrentUser();
        
        // Get the course
        Course course = courseService.getCourseById(courseId);
        
        // Verify the current user is the teacher of this course
        if (!course.getTeacher().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard/teacher";
        }
        
        // Get course members
        List<CourseMembership> memberships = courseMembershipService.getMembershipsByCourse(course);
        
        // Get course assignments - Force fetch to ensure we have the latest
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        
        // Add data to the model
        model.addAttribute("course", CourseDTO.fromEntity(course));
        model.addAttribute("students", memberships.stream()
                .filter(m -> m.getRole() == UserType.STUDENT)
                .collect(Collectors.toList()));
        model.addAttribute("tas", memberships.stream()
                .filter(m -> m.getRole() == UserType.TA)
                .collect(Collectors.toList()));
        model.addAttribute("assignments", assignments);
        model.addAttribute("currentUser", currentUser);
        
        return "course/teacher-view";
    }
    
    @GetMapping("/student/course/{courseId}")
    public String studentCourseView(@PathVariable Long courseId, Model model) {
        User currentUser = getCurrentUser();
        
        // Get the course
        Course course = courseService.getCourseById(courseId);
        
        // Verify the current user is a member of this course
        boolean isMember = courseMembershipService.getMembershipsByUser(currentUser)
                .stream()
                .anyMatch(membership -> 
                        membership.getCourse().getId().equals(courseId) && 
                        membership.getRole() == UserType.STUDENT);
        
        if (!isMember) {
            return "redirect:/dashboard/student";
        }
        
        // Get course assignments - Force fetch to ensure we have the latest
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        
        // Add data to the model
        model.addAttribute("course", CourseDTO.fromEntity(course));
        model.addAttribute("assignments", assignments);
        model.addAttribute("currentUser", currentUser);
        
        // To fix the student view issue, let's add a log message to help debug
        System.out.println("Student View - Loaded " + assignments.size() + " assignments for course ID: " + courseId);
        
        return "course/student-view";
    }
    
    @GetMapping("/ta/course/{courseId}")
    public String taCourseView(@PathVariable Long courseId, Model model) {
        User currentUser = getCurrentUser();
        
        // Get the course
        Course course = courseService.getCourseById(courseId);
        
        // Verify the current user is a TA of this course
        boolean isTA = courseMembershipService.getMembershipsByUser(currentUser)
                .stream()
                .anyMatch(membership -> 
                        membership.getCourse().getId().equals(courseId) && 
                        membership.getRole() == UserType.TA);
        
        if (!isTA) {
            return "redirect:/dashboard/ta";
        }
        
        // Get course members
        List<CourseMembership> memberships = courseMembershipService.getMembershipsByCourse(course);
        
        // Get course assignments
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        
        // Add data to the model
        model.addAttribute("course", CourseDTO.fromEntity(course));
        model.addAttribute("students", memberships.stream()
                .filter(m -> m.getRole() == UserType.STUDENT)
                .collect(Collectors.toList()));
        model.addAttribute("assignments", assignments);
        model.addAttribute("currentUser", currentUser);
        
        return "course/ta-view";
    }
} 