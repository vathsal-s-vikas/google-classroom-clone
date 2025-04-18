package com.classroom.controller;

import com.classroom.model.Course;
import com.classroom.model.CourseMembership;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.model.Assignment;
import com.classroom.model.Submission;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.CourseMembershipService;
import com.classroom.service.CourseService;
import com.classroom.service.AssignmentService;
import com.classroom.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
public class DashboardController {

    @Autowired
    private CourseMembershipService courseMembershipService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomAuthenticationConverter authConverter;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private SubmissionService submissionService;
    
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
        
        // Get all assignments for these courses to separate them by submission status
        List<Assignment> allAssignments = new ArrayList<>();
        for (Course course : courses) {
            List<Assignment> courseAssignments = assignmentService.getAssignmentsByCourse(course);
            allAssignments.addAll(courseAssignments);
        }
        
        // Prepare assignment lists by status
        List<Map<String, Object>> pendingAssignments = new ArrayList<>();
        List<Map<String, Object>> submittedAssignments = new ArrayList<>();
        List<Map<String, Object>> unevaluatedSubmissions = new ArrayList<>();
        List<Map<String, Object>> evaluatedSubmissions = new ArrayList<>();
        List<Map<String, Object>> missedAssignments = new ArrayList<>();
        
        // Current time for deadline comparison
        LocalDateTime now = LocalDateTime.now();
        
        // Process each assignment to determine its status
        for (Assignment assignment : allAssignments) {
            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("id", assignment.getId());
            assignmentData.put("title", assignment.getTitle());
            assignmentData.put("description", assignment.getDescription());
            assignmentData.put("deadline", assignment.getDeadline());
            assignmentData.put("courseId", assignment.getCourse().getId());
            assignmentData.put("courseName", assignment.getCourse().getName());
            assignmentData.put("lateSubmissionAllowed", assignment.isLateSubmissionAllowed());
            assignmentData.put("maxLateDays", assignment.getMaxLateDays());
            
            // Check if there's a submission for this assignment
            Optional<Submission> submissionOpt = submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
            
            if (submissionOpt.isPresent()) {
                // Student has submitted this assignment
                Submission submission = submissionOpt.get();
                assignmentData.put("submissionId", submission.getId());
                assignmentData.put("submittedAt", submission.getSubmittedAt());
                assignmentData.put("isLate", submission.isLate());
                
                submittedAssignments.add(assignmentData);
                
                if (submission.isEvaluated()) {
                    // Add mark info if evaluated
                    if (submission.getMark() != null) {
                        assignmentData.put("mark", submission.getMark());
                    }
                    evaluatedSubmissions.add(assignmentData);
                } else {
                    unevaluatedSubmissions.add(assignmentData);
                }
            } else {
                // No submission yet
                boolean isOverdue = now.isAfter(assignment.getDeadline());
                
                if (isOverdue && !assignment.isLateSubmissionAllowed()) {
                    // Assignment is overdue and late submissions aren't allowed - it's a missed assignment
                    assignmentData.put("status", "Not Submitted");
                    missedAssignments.add(assignmentData);
                } else if (isOverdue && assignment.isLateSubmissionAllowed()) {
                    // Late submission is allowed - check if it's still within the late submission window
                    long daysLate = ChronoUnit.DAYS.between(assignment.getDeadline(), now);
                    if (assignment.getMaxLateDays() != null && daysLate > assignment.getMaxLateDays()) {
                        // Beyond late submission window - missed
                        assignmentData.put("status", "Not Submitted");
                        missedAssignments.add(assignmentData);
                    } else {
                        // Still within late submission window - pending but mark as late
                        assignmentData.put("status", "Late");
                        pendingAssignments.add(assignmentData);
                    }
                } else {
                    // Still within deadline - pending
                    pendingAssignments.add(assignmentData);
                }
            }
        }
        
        // Add all data to the model
        model.addAttribute("courses", courses);
        model.addAttribute("pendingAssignments", pendingAssignments);
        model.addAttribute("submittedAssignments", submittedAssignments);
        model.addAttribute("unevaluatedSubmissions", unevaluatedSubmissions);
        model.addAttribute("evaluatedSubmissions", evaluatedSubmissions);
        model.addAttribute("missedAssignments", missedAssignments);
        model.addAttribute("currentUser", currentUser);
        
        return "student"; // looks for student.html in /resources/templates
    }

    @GetMapping("/dashboard/teacher")
    public String teacherDashboard(Model model) {
        User currentUser = getCurrentUser();
        
        // Get all courses created by this teacher
        List<Course> courses = courseService.getActiveCoursesByTeacher(currentUser);
        List<Course> archivedCourses = courseService.getArchivedCoursesByTeacher(currentUser);
        
        // Get assignments that either:
        // 1. Have unevaluated submissions OR
        // 2. Haven't reached their deadline yet
        List<Map<String, Object>> relevantAssignments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Course course : courses) {
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
            
            for (Assignment assignment : assignments) {
                // Get all submissions for this assignment
                List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
                
                // Count unevaluated submissions
                long unevaluatedCount = submissions.stream()
                        .filter(s -> !s.isEvaluated())
                        .count();
                
                boolean deadlinePassed = now.isAfter(assignment.getDeadline());
                boolean hasPendingEvaluations = unevaluatedCount > 0;
                
                // Include if deadline hasn't passed OR there are pending evaluations
                if (!deadlinePassed || hasPendingEvaluations) {
                    Map<String, Object> assignmentData = new HashMap<>();
                    assignmentData.put("id", assignment.getId());
                    assignmentData.put("title", assignment.getTitle());
                    assignmentData.put("courseId", course.getId());
                    assignmentData.put("courseName", course.getName());
                    assignmentData.put("deadline", assignment.getDeadline());
                    assignmentData.put("deadlinePassed", deadlinePassed);
                    assignmentData.put("pendingCount", unevaluatedCount);
                    assignmentData.put("totalCount", submissions.size());
                    
                    relevantAssignments.add(assignmentData);
                }
            }
        }
        
        // Add courses and current user to the model
        model.addAttribute("courses", courses);
        model.addAttribute("archivedCourses", archivedCourses);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pendingEvaluations", relevantAssignments);
        
        return "teacher";
    }

    @GetMapping("/dashboard/ta")
    public String taDashboard() {
        return "redirect:/ta-view/dashboard";
    }

    @GetMapping("/test-course-creation")
    public String testCourseCreation() {
        return "test-course-creation";
    }

    @GetMapping("/api/dashboard/teacher/course-counts")
    @ResponseBody
    public Map<String, Object> getTeacherCourseCounts() {
        User currentUser = getCurrentUser();
        
        // Get all courses created by this teacher
        List<Course> courses = courseService.getActiveCoursesByTeacher(currentUser);
        List<Course> archivedCourses = courseService.getArchivedCoursesByTeacher(currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("activeCourseCount", courses.size());
        response.put("archivedCourseCount", archivedCourses.size());
        response.put("teacherName", currentUser.getName());
        
        return response;
    }

}
