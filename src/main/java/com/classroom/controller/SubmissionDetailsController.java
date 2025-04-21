package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentService;
import com.classroom.service.SubmissionService;
import com.classroom.service.SubmissionAttachmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
public class SubmissionDetailsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomAuthenticationConverter authConverter;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionAttachmentService attachmentService;

    private User getCurrentUser() {
        // First convert the authentication if needed
        authConverter.convertAuthentication();
        
        // Get the updated authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        
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
    
    /**
     * Handles the page for submitting a new assignment
     */
    @GetMapping("/student/course/{courseId}/assignment/{assignmentId}/submit")
    public String showSubmitAssignmentForm(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Check if the assignment belongs to the specified course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/dashboard/student";
        }
        
        // Check if student is enrolled in the course
        boolean isEnrolled = assignment.getCourse().getMemberships().stream()
                .anyMatch(membership -> 
                        membership.getUser().getId().equals(currentUser.getId()) && 
                        membership.getRole() == UserType.STUDENT);
        
        if (!isEnrolled) {
            return "redirect:/dashboard/student";
        }
        
        // Check if student has already submitted
        Optional<Submission> existingSubmission = 
                submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
        
        if (existingSubmission.isPresent()) {
            // Already submitted, redirect to view submission
            return "redirect:/student/course/" + courseId + "/assignment/" + assignmentId + "/submission";
        }
        
        // Add data to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("course", assignment.getCourse());
        model.addAttribute("assignment", assignment);
        
        return "course/student-submission-form";
    }

    @GetMapping("/student/course/{courseId}/assignment/{assignmentId}/submission")
    public String viewStudentSubmission(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Check if the assignment belongs to the specified course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/dashboard/student";
        }
        
        // Check if student is enrolled in the course
        boolean isEnrolled = assignment.getCourse().getMemberships().stream()
                .anyMatch(membership -> 
                        membership.getUser().getId().equals(currentUser.getId()) && 
                        membership.getRole() == UserType.STUDENT);
        
        if (!isEnrolled) {
            return "redirect:/dashboard/student";
        }
        
        // Get student's submission
        Optional<Submission> submissionOpt = 
                submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
        
        if (!submissionOpt.isPresent()) {
            // No submission found, redirect to course view
            return "redirect:/student/course/" + courseId;
        }
        
        Submission submission = submissionOpt.get();
        
        // Get submission attachments
        List<SubmissionAttachment> attachments = 
                attachmentService.getAttachmentsBySubmission(submission);
        
        // Add data to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submission);
        model.addAttribute("attachments", attachments);
        model.addAttribute("course", assignment.getCourse());
        
        return "course/student-submission-view";
    }
    
    @GetMapping("/teacher/course/{courseId}/assignment/{assignmentId}/submission/{submissionId}")
    public String viewTeacherSubmission(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Check if the assignment belongs to the specified course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/dashboard/teacher";
        }
        
        // Check if current user is the teacher of this course
        if (!assignment.getCourse().getTeacher().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard/teacher";
        }
        
        // Get the submission
        Optional<Submission> submissionOpt = submissionService.getSubmissionById(submissionId);
        
        if (!submissionOpt.isPresent()) {
            return "redirect:/teacher/course/" + courseId;
        }
        
        Submission submission = submissionOpt.get();
        
        // Check if the submission belongs to the specified assignment
        if (!submission.getAssignment().getId().equals(assignmentId)) {
            return "redirect:/teacher/course/" + courseId;
        }
        
        // Get submission attachments
        List<SubmissionAttachment> attachments = 
                attachmentService.getAttachmentsBySubmission(submission);
        
        // Get student info if available
        User student = submission.getStudent();
        
        // Add data to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submission);
        model.addAttribute("attachments", attachments);
        model.addAttribute("course", assignment.getCourse());
        model.addAttribute("student", student);
        
        return "course/submission-evaluate";
    }
} 