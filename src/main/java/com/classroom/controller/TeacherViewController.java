package com.classroom.controller;

import com.classroom.model.Assignment;
import com.classroom.model.Course;
import com.classroom.model.Mark;
import com.classroom.model.Submission;
import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentService;
import com.classroom.service.CourseService;
import com.classroom.service.MarkService;
import com.classroom.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class TeacherViewController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomAuthenticationConverter authConverter;

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

    @GetMapping("/teacher/course/{courseId}/assignment/{assignmentId}")
    public String viewAssignmentWithSubmissions(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the course and verify that the current user is the teacher
        Course course = courseService.getCourseById(courseId);
        if (!course.getTeacher().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard/teacher";
        }
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the assignment belongs to the course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/teacher/course/" + courseId;
        }
        
        // Get all submissions for this assignment
        List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
        
        // Count evaluated and unevaluated submissions
        long evaluatedCount = submissions.stream().filter(Submission::isEvaluated).count();
        long unevaluatedCount = submissions.size() - evaluatedCount;
        
        // Add data to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("course", course);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("evaluatedCount", evaluatedCount);
        model.addAttribute("unevaluatedCount", unevaluatedCount);
        
        return "course/assignment-submissions";
    }
    
    @PostMapping("/teacher/submission/{submissionId}/evaluate")
    public String evaluateSubmission(
            @PathVariable Long submissionId,
            @RequestParam Integer score,
            @RequestParam(required = false) String feedback,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the submission
        Submission submission = submissionService.getSubmissionById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
        
        // Verify the current user is the teacher of the course
        Course course = submission.getAssignment().getCourse();
        if (!course.getTeacher().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard/teacher";
        }
        
        // Ensure score is within assignment max score
        Integer maxMarks = submission.getAssignment().getMaxMarks();
        if (score > maxMarks) {
            score = maxMarks;
        }
        
        // Calculate penalty if submission is late
        Integer penaltyPercentage = 0;
        if (submission.isLate() && submission.getAssignment().isLateSubmissionAllowed()) {
            penaltyPercentage = submission.getAssignment().getLatePenaltyPercentage();
        }
        
        // Check if mark already exists
        Mark existingMark = submission.getMark();
        if (existingMark != null) {
            // Update existing mark
            markService.updateMark(existingMark.getId(), score, penaltyPercentage, 
                                  feedback, currentUser, "Updated via evaluation form");
        } else {
            // Create new mark
            markService.createMark(submission, score, penaltyPercentage, feedback, currentUser);
        }
        
        return "redirect:/teacher/course/" + course.getId() + "/assignment/" + submission.getAssignment().getId();
    }

    @GetMapping("/teacher/course/{courseId}/assignment/{assignmentId}/batch-evaluate")
    public String batchEvaluateSubmissions(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the course and verify that the current user is the teacher
        Course course = courseService.getCourseById(courseId);
        if (!course.getTeacher().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard/teacher";
        }
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the assignment belongs to the course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/teacher/course/" + courseId;
        }
        
        // Get all submissions for this assignment
        List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
        
        // Add data to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("course", course);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        
        return "course/batch-evaluate";
    }
    
    @PostMapping("/teacher/assignment/{assignmentId}/batch-evaluate")
    public String saveBatchEvaluations(
            @PathVariable Long assignmentId,
            @RequestParam Map<String, String> allParams,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the current user is the teacher of the course
        Course course = assignment.getCourse();
        if (!course.getTeacher().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard/teacher";
        }
        
        // Process each submission score
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("score_")) {
                try {
                    // Extract submissionId from the parameter name (format: score_{submissionId})
                    Long submissionId = Long.parseLong(entry.getKey().substring(6));
                    
                    // Get score value
                    Integer score = Integer.parseInt(entry.getValue());
                    
                    // Get feedback if present
                    String feedback = allParams.get("feedback_" + submissionId);
                    
                    // Get the submission
                    Optional<Submission> submissionOpt = submissionService.getSubmissionById(submissionId);
                    
                    if (submissionOpt.isPresent()) {
                        Submission submission = submissionOpt.get();
                        
                        // Ensure score is within assignment max score
                        Integer maxMarks = assignment.getMaxMarks();
                        if (score > maxMarks) {
                            score = maxMarks;
                        }
                        
                        // Calculate penalty if submission is late
                        Integer penaltyPercentage = 0;
                        if (submission.isLate() && assignment.isLateSubmissionAllowed()) {
                            penaltyPercentage = assignment.getLatePenaltyPercentage();
                        }
                        
                        // Check if mark already exists
                        Mark existingMark = submission.getMark();
                        if (existingMark != null) {
                            // Update existing mark
                            markService.updateMark(existingMark.getId(), score, penaltyPercentage, 
                                                 feedback, currentUser, "Updated via batch evaluation");
                        } else {
                            // Create new mark
                            markService.createMark(submission, score, penaltyPercentage, feedback, currentUser);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                    continue;
                }
            }
        }
        
        return "redirect:/teacher/course/" + course.getId() + "/assignment/" + assignmentId;
    }
} 