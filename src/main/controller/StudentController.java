package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomAuthenticationConverter authConverter;

    @Autowired
    private CourseMembershipService courseMembershipService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;
    
    @Autowired
    private MarkService markService;

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
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getStudentInfo() {
        final User currentUser = getCurrentUser();
        
        Map<String, String> response = new HashMap<>();
        response.put("name", currentUser.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pending-assignments")
    public ResponseEntity<List<Map<String, Object>>> getPendingAssignments() {
        try {
            final User currentUser = getCurrentUser();
            
            // Get all courses the student is enrolled in
            List<CourseMembership> memberships = courseMembershipService.getMembershipsByUser(currentUser);
            
            // Filter for STUDENT role only
            List<Course> studentCourses = memberships.stream()
                    .filter(m -> m.getRole() == UserType.STUDENT)
                    .map(CourseMembership::getCourse)
                    .filter(course -> !course.isArchived()) // Only active courses
                    .collect(Collectors.toList());
            
            // Get all assignments from these courses with submission status
            List<Map<String, Object>> assignmentsWithStatus = new ArrayList<>();
            
            for (Course course : studentCourses) {
                List<Assignment> courseAssignments = assignmentService.getAssignmentsByCourse(course);
                
                for (Assignment assignment : courseAssignments) {
                    Map<String, Object> assignmentData = new HashMap<>();
                    assignmentData.put("id", assignment.getId());
                    assignmentData.put("title", assignment.getTitle());
                    assignmentData.put("courseId", course.getId());
                    assignmentData.put("courseName", course.getName());
                    assignmentData.put("deadline", assignment.getDeadline());
                    assignmentData.put("maxMarks", assignment.getMaxMarks());
                    assignmentData.put("assignmentType", assignment.getAssignmentType());
                    
                    // Check submission status
                    Optional<Submission> submission = submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
                    
                    if (submission.isPresent()) {
                        assignmentData.put("submitted", true);
                        assignmentData.put("submissionId", submission.get().getId());
                        assignmentData.put("submittedAt", submission.get().getSubmittedAt());
                        assignmentData.put("isLate", submission.get().isLate());
                        assignmentData.put("isEvaluated", submission.get().isEvaluated());
                        
                        // If evaluated, get marks
                        if (submission.get().isEvaluated()) {
                            Mark mark = submission.get().getMark();
                            if (mark != null) {
                                assignmentData.put("marks", mark.getMarks());
                                assignmentData.put("feedback", mark.getFeedback());
                            }
                        }
                    } else {
                        assignmentData.put("submitted", false);
                        
                        // Check if past deadline
                        boolean isPastDeadline = assignment.getDeadline().isBefore(LocalDateTime.now());
                        assignmentData.put("pastDeadline", isPastDeadline);
                        
                        // Check if submission still allowed
                        boolean canSubmit = !isPastDeadline || assignment.isLateSubmissionAllowed();
                        assignmentData.put("canSubmit", canSubmit);
                    }
                    
                    assignmentsWithStatus.add(assignmentData);
                }
            }
            
            return ResponseEntity.ok(assignmentsWithStatus);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
    
    @GetMapping("/evaluated-submissions")
    public ResponseEntity<List<Map<String, Object>>> getEvaluatedSubmissions() {
        try {
            final User currentUser = getCurrentUser();
            
            // Get all submissions for this student that are evaluated
            List<Submission> evaluatedSubmissions = submissionService.getEvaluatedSubmissionsByStudent(currentUser);
            
            List<Map<String, Object>> submissionsWithDetails = evaluatedSubmissions.stream()
                    .map(submission -> {
                        Map<String, Object> submissionData = new HashMap<>();
                        submissionData.put("id", submission.getId());
                        
                        // Assignment info
                        Assignment assignment = submission.getAssignment();
                        Map<String, Object> assignmentInfo = new HashMap<>();
                        assignmentInfo.put("id", assignment.getId());
                        assignmentInfo.put("title", assignment.getTitle());
                        assignmentInfo.put("maxMarks", assignment.getMaxMarks());
                        submissionData.put("assignment", assignmentInfo);
                        
                        // Course info
                        Course course = assignment.getCourse();
                        Map<String, Object> courseInfo = new HashMap<>();
                        courseInfo.put("id", course.getId());
                        courseInfo.put("name", course.getName());
                        submissionData.put("course", courseInfo);
                        
                        // Mark info if available
                        Mark mark = submission.getMark();
                        if (mark != null) {
                            Map<String, Object> markInfo = new HashMap<>();
                            markInfo.put("marks", mark.getMarks());
                            markInfo.put("feedback", mark.getFeedback());
                            markInfo.put("evaluatedAt", mark.getEvaluatedAt());
                            submissionData.put("mark", markInfo);
                        }
                        
                        // Submission details
                        submissionData.put("submittedAt", submission.getSubmittedAt());
                        submissionData.put("isLate", submission.isLate());
                        
                        return submissionData;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(submissionsWithDetails);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/pending-evaluations")
    public ResponseEntity<List<Map<String, Object>>> getPendingEvaluations() {
        try {
            final User currentUser = getCurrentUser();
            
            // Get all submissions for this student that are submitted but not evaluated
            List<Submission> pendingSubmissions = submissionService.getPendingEvaluationSubmissionsByStudent(currentUser);
            
            List<Map<String, Object>> submissionsWithDetails = pendingSubmissions.stream()
                    .map(submission -> {
                        Map<String, Object> submissionData = new HashMap<>();
                        submissionData.put("id", submission.getId());
                        
                        // Assignment info
                        Assignment assignment = submission.getAssignment();
                        Map<String, Object> assignmentInfo = new HashMap<>();
                        assignmentInfo.put("id", assignment.getId());
                        assignmentInfo.put("title", assignment.getTitle());
                        assignmentInfo.put("maxMarks", assignment.getMaxMarks());
                        submissionData.put("assignment", assignmentInfo);
                        
                        // Course info
                        Course course = assignment.getCourse();
                        Map<String, Object> courseInfo = new HashMap<>();
                        courseInfo.put("id", course.getId());
                        courseInfo.put("name", course.getName());
                        submissionData.put("course", courseInfo);
                        
                        // Submission details
                        submissionData.put("submittedAt", submission.getSubmittedAt());
                        submissionData.put("isLate", submission.isLate());
                        
                        return submissionData;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(submissionsWithDetails);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
} 