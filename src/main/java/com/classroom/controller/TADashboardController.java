package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.repository.*;
import com.classroom.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/ta-dashboard")
public class TADashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private CourseMembershipService courseMembershipService;

    @Autowired
    private MarkService markService;

    @Autowired
    private SubmissionAttachmentRepository submissionAttachmentRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User currentUser = getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);

        // Get courses where the user is a TA
        List<Course> taCourses = courseMembershipService.getCoursesForUserWithRole(currentUser, UserType.TA);
        model.addAttribute("courses", taCourses);

        // Calculate stats
        int totalCourses = taCourses.size();
        int totalAssignments = 0;
        int pendingEvaluations = 0;
        int recentActivity = 0; // This would come from an activity service in a real implementation

        Map<Long, CourseStats> courseStats = new HashMap<>();

        for (Course course : taCourses) {
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
            int courseAssignments = assignments.size();
            totalAssignments += courseAssignments;

            int courseStudents = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.STUDENT).size();
            
            int coursePending = 0;
            for (Assignment assignment : assignments) {
                coursePending += (int) submissionService.getSubmissionsByAssignment(assignment).stream()
                        .filter(s -> s.getMark() == null)
                        .count();
            }
            
            pendingEvaluations += coursePending;
            
            courseStats.put(course.getId(), new CourseStats(courseAssignments, courseStudents, coursePending));
        }

        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("totalAssignments", totalAssignments);
        model.addAttribute("pendingEvaluations", pendingEvaluations);
        model.addAttribute("recentActivity", recentActivity);
        model.addAttribute("courseStats", courseStats);

        // Get pending submissions
        List<Submission> pendingSubmissions = new ArrayList<>();
        for (Course course : taCourses) {
            for (Assignment assignment : assignmentService.getAssignmentsByCourse(course)) {
                pendingSubmissions.addAll(
                    submissionService.getSubmissionsByAssignment(assignment).stream()
                        .filter(s -> s.getMark() == null)
                        .collect(Collectors.toList())
                );
            }
        }
        
        // Sort by submission date, most recent first
        pendingSubmissions.sort(Comparator.comparing(Submission::getSubmittedAt).reversed());
        
        // Limit to 10 submissions for dashboard
        if (pendingSubmissions.size() > 10) {
            pendingSubmissions = pendingSubmissions.subList(0, 10);
        }
        
        model.addAttribute("pendingSubmissions", pendingSubmissions);
        
        // Mock activity data - in a real implementation, this would come from a service
        model.addAttribute("activities", Collections.emptyList());

        return "ta-dashboard";
    }

    @GetMapping("/course/{courseId}")
    public String viewCourse(@AuthenticationPrincipal OAuth2User principal, 
                            @PathVariable Long courseId,
                            Model model) {
        User currentUser = getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        
        Course course = courseService.getCourseById(courseId);
        if (course == null) {
            return "redirect:/ta-dashboard";
        }
        
        // Check if the user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
            
        if (!isTA) {
            return "redirect:/ta-dashboard";
        }
        
        model.addAttribute("course", course);
        
        // Get assignments for this course
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        model.addAttribute("assignments", assignments);
        
        // Get students for this course
        List<User> students = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.STUDENT)
            .stream()
            .map(CourseMembership::getUser)
            .collect(Collectors.toList());
        model.addAttribute("students", students);
        
        // Calculate submission statistics
        Map<Long, AssignmentStats> assignmentStats = new HashMap<>();
        for (Assignment assignment : assignments) {
            List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
            int totalSubmissions = submissions.size();
            int evaluatedSubmissions = (int) submissions.stream()
                .filter(s -> s.getMark() != null)
                .count();
            int lateSubmissions = (int) submissions.stream()
                .filter(Submission::isLate)
                .count();
                
            assignmentStats.put(assignment.getId(), 
                new AssignmentStats(totalSubmissions, evaluatedSubmissions, lateSubmissions));
        }
        model.addAttribute("assignmentStats", assignmentStats);
        
        return "course/ta-view";
    }
    
    @GetMapping("/assignment/{assignmentId}")
    public String viewAssignment(@AuthenticationPrincipal OAuth2User principal,
                               @PathVariable Long assignmentId,
                               Model model) {
        User currentUser = getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        if (assignment == null) {
            return "redirect:/ta-dashboard";
        }
        
        Course course = assignment.getCourse();
        
        // Check if the user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
            
        if (!isTA) {
            return "redirect:/ta-dashboard";
        }
        
        model.addAttribute("assignment", assignment);
        model.addAttribute("course", course);
        
        return "assignment/ta-assignment-view";
    }
    
    @GetMapping("/assignment/{assignmentId}/submissions")
    public String viewSubmissions(@AuthenticationPrincipal OAuth2User principal,
                                @PathVariable Long assignmentId,
                                Model model) {
        User currentUser = getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        if (assignment == null) {
            return "redirect:/ta-dashboard";
        }
        
        Course course = assignment.getCourse();
        
        // Check if the user is a TA for this course
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
            
        if (!isTA) {
            return "redirect:/ta-dashboard";
        }
        
        // Get all submissions for this assignment
        List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
        model.addAttribute("submissions", submissions);
        
        // Count statistics
        int evaluatedCount = (int) submissions.stream().filter(s -> s.getMark() != null).count();
        int pendingCount = submissions.size() - evaluatedCount;
        
        model.addAttribute("assignment", assignment);
        model.addAttribute("course", course);
        model.addAttribute("evaluatedCount", evaluatedCount);
        model.addAttribute("pendingCount", pendingCount);
        
        return "assignment/ta-submissions-view";
    }
    
    @GetMapping("/submission/{submissionId}/evaluate")
    public String evaluateSubmission(@AuthenticationPrincipal OAuth2User principal,
                                    @PathVariable Long submissionId,
                                    Model model) {
        User currentUser = getCurrentUser(principal);
        Submission submission = submissionService.getSubmissionById(submissionId).get();
        
        if (submission == null) {
            return "redirect:/ta-dashboard";
        }
        
        Course course = submission.getAssignment().getCourse();
        List<CourseMembership> taMemberships = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA);
        boolean isTA = taMemberships.stream().anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-dashboard";
        }
        
        model.addAttribute("submission", submission);
        model.addAttribute("assignment", submission.getAssignment());
        model.addAttribute("course", course);
        
        return "ta/evaluate-submission";
    }
    
    @PostMapping("/submission/{submissionId}/evaluate")
    public String evaluateSubmission(@AuthenticationPrincipal OAuth2User principal,
                                    @PathVariable Long submissionId,
                                    @RequestParam Integer score,
                                    @RequestParam(required = false) String feedback) {
        User currentUser = getCurrentUser(principal);
        Submission submission = submissionService.getSubmissionById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));
        
        Assignment assignment = submission.getAssignment();
        Course course = assignment.getCourse();
        
        // Check if the current user is a TA for this course
        boolean isTA = courseMembershipService.getCourseMembershipsByCourseAndRole(course, UserType.TA)
                .stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        
        if (!isTA) {
            return "redirect:/ta-dashboard";
        }
        
        // Ensure score is within limits
        if (score < 0) {
            score = 0;
        } else if (score > assignment.getMaxMarks()) {
            score = assignment.getMaxMarks();
        }
        
        // Calculate penalty for late submission
        Integer finalScore = score;
        Integer penaltyPercentage = 0;
        if (submission.getSubmittedAt().isAfter(assignment.getDeadline())) {
            // Apply late submission penalty - 10% per day late
            int daysLate = (int) ChronoUnit.DAYS.between(assignment.getDeadline(), submission.getSubmittedAt());
            if (daysLate == 0) daysLate = 1; // If it's late but less than a day, count as 1 day late
            
            penaltyPercentage = Integer.min(daysLate * 10, 100); // Maximum 100% penalty
            double penaltyPoints = score * (penaltyPercentage / 100.0);
            finalScore = (int)Math.max(0, score - penaltyPoints);
        }
        
        // Create or update mark
        Mark mark = submission.getMark();
        if (mark == null) {
            mark = new Mark();
            mark.setSubmission(submission);
            mark.setEvaluator(currentUser);
        }
        
        mark.setRawMarks(score);
        mark.setPenaltyPercentage(penaltyPercentage);
        mark.setFinalMarks(finalScore);
        mark.setFeedback(feedback);
        mark.setEvaluatedAt(LocalDateTime.now());
        
        markService.saveMark(mark);
        
        return "redirect:/ta-dashboard/course/" + course.getId();
    }
    
    private User getCurrentUser(OAuth2User principal) {
        Map<String, Object> attributes = principal.getAttributes();
        String email = (String) attributes.get("email");
        return userService.getUserByEmail(email);
    }
    
    // Inner classes for statistics
    public static class CourseStats {
        private int assignmentCount;
        private int studentCount;
        private int pendingCount;
        
        public CourseStats(int assignmentCount, int studentCount, int pendingCount) {
            this.assignmentCount = assignmentCount;
            this.studentCount = studentCount;
            this.pendingCount = pendingCount;
        }
        
        public int getAssignmentCount() {
            return assignmentCount;
        }
        
        public int getStudentCount() {
            return studentCount;
        }
        
        public int getPendingCount() {
            return pendingCount;
        }
    }
    
    public static class AssignmentStats {
        private int totalSubmissions;
        private int evaluatedSubmissions;
        private int lateSubmissions;
        
        public AssignmentStats(int totalSubmissions, int evaluatedSubmissions, int lateSubmissions) {
            this.totalSubmissions = totalSubmissions;
            this.evaluatedSubmissions = evaluatedSubmissions;
            this.lateSubmissions = lateSubmissions;
        }
        
        public int getTotalSubmissions() {
            return totalSubmissions;
        }
        
        public int getEvaluatedSubmissions() {
            return evaluatedSubmissions;
        }
        
        public int getLateSubmissions() {
            return lateSubmissions;
        }
        
        public int getPendingSubmissions() {
            return totalSubmissions - evaluatedSubmissions;
        }
        
        public double getCompletionRate() {
            return totalSubmissions == 0 ? 0 : (double) evaluatedSubmissions / totalSubmissions * 100;
        }
    }
    
    // Mock class for Activity - in a real implementation, this would be a proper model
    public static class Activity {
        private String type;
        private String title;
        private String course;
        private String user;
        private String timeAgo;
        
        // Constructor, getters, setters...
    }
} 