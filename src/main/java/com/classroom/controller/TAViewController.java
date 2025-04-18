package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.*;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TAViewController {

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

    @Autowired
    private CourseMembershipService courseMembershipService;

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

    @GetMapping("/ta-view/dashboard")
    public String taDashboard(Model model) {
        User currentUser = getCurrentUser();
        
        // Get courses where the user is a TA
        List<Course> courses = courseMembershipService.getCoursesForUserWithRole(currentUser, UserType.TA);
        model.addAttribute("courses", courses);
        
        // Count total assignments across all courses
        int totalAssignments = 0;
        List<Assignment> recentAssignments = new ArrayList<>();
        int totalStudents = 0;
        
        for (Course course : courses) {
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
            totalAssignments += assignments.size();
            
            // Get 5 most recent assignments
            assignments.stream()
                    .sorted(Comparator.comparing(Assignment::getCreatedAt).reversed())
                    .limit(5)
                    .forEach(recentAssignments::add);
            
            // Count students in the course
            totalStudents += courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.STUDENT).size();
        }
        
        // Count pending evaluations
        int pendingEvaluations = 0;
        List<Submission> recentSubmissions = new ArrayList<>();
        
        for (Course course : courses) {
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
            for (Assignment assignment : assignments) {
                List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
                pendingEvaluations += submissions.stream().filter(s -> !s.isEvaluated()).count();
                
                // Get 10 most recent submissions
                submissions.stream()
                        .filter(s -> !s.isEvaluated())
                        .sorted(Comparator.comparing(Submission::getSubmittedAt).reversed())
                        .limit(5)
                        .forEach(recentSubmissions::add);
            }
        }
        
        // Get upcoming deadlines (assignments due in the next 7 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekFromNow = now.plusDays(7);
        
        List<Assignment> upcomingDeadlines = courses.stream()
                .flatMap(c -> assignmentService.getAssignmentsByCourse(c).stream())
                .filter(a -> a.getDeadline().isAfter(now) && a.getDeadline().isBefore(oneWeekFromNow))
                .sorted(Comparator.comparing(Assignment::getDeadline))
                .collect(Collectors.toList());
        
        // Create recent activities list
        List<Map<String, String>> recentActivities = new ArrayList<>();
        
        // Add recent assignment creations
        for (Assignment assignment : recentAssignments.stream().limit(3).collect(Collectors.toList())) {
            Map<String, String> activity = new HashMap<>();
            activity.put("title", "New Assignment Created");
            activity.put("description", assignment.getTitle() + " in " + assignment.getCourse().getName());
            activity.put("timeAgo", getTimeAgo(assignment.getCreatedAt()));
            recentActivities.add(activity);
        }
        
        // Add recent submissions
        for (Submission submission : recentSubmissions.stream().limit(3).collect(Collectors.toList())) {
            Map<String, String> activity = new HashMap<>();
            activity.put("title", "New Submission");
            
            String submitterName;
            if (submission.getUser() != null) {
                submitterName = submission.getUser().getName();
            } else if (submission.getTeam() != null) {
                submitterName = submission.getTeam().getName();
            } else {
                submitterName = "Unknown";
            }
            
            activity.put("description", submitterName + " submitted for " + submission.getAssignment().getTitle());
            activity.put("timeAgo", getTimeAgo(submission.getSubmittedAt()));
            recentActivities.add(activity);
        }
        
        // Sort activities by timeAgo
        recentActivities.sort((a, b) -> {
            // Simple sorting logic - this could be enhanced
            String timeA = a.get("timeAgo");
            String timeB = b.get("timeAgo");
            return timeA.compareTo(timeB);
        });
        
        model.addAttribute("totalAssignments", totalAssignments);
        model.addAttribute("pendingEvaluations", pendingEvaluations);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("recentAssignments", recentAssignments);
        model.addAttribute("recentSubmissions", recentSubmissions);
        model.addAttribute("upcomingDeadlines", upcomingDeadlines);
        model.addAttribute("recentActivities", recentActivities);
        model.addAttribute("currentUser", currentUser);
        
        return "ta";
    }

    @GetMapping("/ta-view/course/{courseId}")
    public String viewCourse(@PathVariable Long courseId, Model model) {
        User currentUser = getCurrentUser();
        
        // Get the course
        Course course = courseService.getCourseById(courseId);
        
        // Verify that the current user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-view/dashboard";
        }
        
        // Get assignments for this course
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        
        // Get all submissions for this course
        List<Submission> allSubmissions = new ArrayList<>();
        for (Assignment assignment : assignments) {
            allSubmissions.addAll(submissionService.getSubmissionsByAssignment(assignment));
        }
        
        // Count statistics
        long evaluatedSubmissions = allSubmissions.stream().filter(Submission::isEvaluated).count();
        long pendingSubmissions = allSubmissions.size() - evaluatedSubmissions;
        long studentCount = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.STUDENT).size();
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        model.addAttribute("evaluatedSubmissions", evaluatedSubmissions);
        model.addAttribute("pendingSubmissions", pendingSubmissions);
        model.addAttribute("studentCount", studentCount);
        
        return "course/ta-view";
    }

    @GetMapping("/ta-view/course/{courseId}/assignment/{assignmentId}")
    public String viewAssignmentWithSubmissions(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the course
        Course course = courseService.getCourseById(courseId);
        
        // Verify that the current user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-view/dashboard";
        }
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the assignment belongs to the course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/ta-view/course/" + courseId;
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
    
    @GetMapping("/ta-view/submission/{submissionId}/evaluate")
    public String evaluateSubmissionForm(
            @PathVariable Long submissionId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the submission
        Submission submission = submissionService.getSubmissionById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
        
        // Get the course
        Course course = submission.getAssignment().getCourse();
        
        // Verify that the current user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-view/dashboard";
        }
        
        // Add data to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("submission", submission);
        model.addAttribute("course", course);
        model.addAttribute("assignment", submission.getAssignment());
        
        return "course/submission-evaluate";
    }
    
    @PostMapping("/ta-view/submission/{submissionId}/evaluate")
    public String evaluateSubmission(
            @PathVariable Long submissionId,
            @RequestParam Integer score,
            @RequestParam(required = false) String feedback,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the submission
        Submission submission = submissionService.getSubmissionById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
        
        // Get the course
        Course course = submission.getAssignment().getCourse();
        
        // Verify that the current user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-view/dashboard";
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
        
        // Create mark using MarkService
        markService.createMark(submission, score, penaltyPercentage, feedback, currentUser);
        
        return "redirect:/ta-view/course/" + course.getId() + "/assignment/" + submission.getAssignment().getId();
    }
    
    @GetMapping("/ta-view/course/{courseId}/assignment/{assignmentId}/batch-evaluate")
    public String batchEvaluateSubmissions(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the course
        Course course = courseService.getCourseById(courseId);
        
        // Verify that the current user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-view/dashboard";
        }
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the assignment belongs to the course
        if (!assignment.getCourse().getId().equals(courseId)) {
            return "redirect:/ta-view/course/" + courseId;
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
    
    @PostMapping("/ta-view/assignment/{assignmentId}/batch-evaluate")
    public String saveBatchEvaluations(
            @PathVariable Long assignmentId,
            @RequestParam Map<String, String> allParams,
            Model model) {
        
        User currentUser = getCurrentUser();
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Get the course
        Course course = assignment.getCourse();
        
        // Verify that the current user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-view/dashboard";
        }
        
        // Process each submission
        for (String key : allParams.keySet()) {
            if (key.startsWith("score_")) {
                String submissionIdStr = key.substring("score_".length());
                Long submissionId = Long.parseLong(submissionIdStr);
                
                String scoreStr = allParams.get(key);
                String feedback = allParams.get("feedback_" + submissionIdStr);
                
                if (scoreStr != null && !scoreStr.isEmpty()) {
                    try {
                        Integer score = Integer.parseInt(scoreStr);
                        
                        // Get the submission
                        submissionService.getSubmissionById(submissionId).ifPresent(submission -> {
                            // Ensure score is within max marks
                            Integer maxMarks = submission.getAssignment().getMaxMarks();
                            Integer adjustedScore = score;
                            if (adjustedScore > maxMarks) {
                                adjustedScore = maxMarks;
                            }
                            
                            // Calculate penalty if submission is late
                            Integer penaltyPercentage = 0;
                            if (submission.isLate() && submission.getAssignment().isLateSubmissionAllowed()) {
                                penaltyPercentage = submission.getAssignment().getLatePenaltyPercentage();
                            }
                            
                            // Create mark
                            markService.createMark(submission, adjustedScore, penaltyPercentage, feedback, currentUser);
                        });
                    } catch (NumberFormatException e) {
                        // Skip invalid scores
                    }
                }
            }
        }
        
        return "redirect:/ta-view/course/" + course.getId() + "/assignment/" + assignmentId;
    }
    
    // Helper method to get time ago text
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutesAgo < 1) {
            return "Just now";
        }
        if (minutesAgo < 60) {
            return minutesAgo + "m ago";
        }
        if (minutesAgo < 24 * 60) {
            return (minutesAgo / 60) + "h ago";
        }
        if (minutesAgo < 7 * 24 * 60) {
            return (minutesAgo / (24 * 60)) + "d ago";
        }
        if (minutesAgo < 30 * 24 * 60) {
            return (minutesAgo / (7 * 24 * 60)) + "w ago";
        }
        
        return (minutesAgo / (30 * 24 * 60)) + "mo ago";
    }
} 