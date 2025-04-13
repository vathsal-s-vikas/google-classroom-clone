package com.classroom.clone.controller;

import com.classroom.clone.model.Course;
import com.classroom.clone.model.CourseMembership;
import com.classroom.clone.model.User;
import com.classroom.clone.repository.CourseMembershipRepository;
import com.classroom.clone.repository.CourseRepository;
import com.classroom.clone.service.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository courseMembershipRepository;

    @Autowired
    public StudentController(CourseRepository courseRepository,
                             CourseMembershipRepository courseMembershipRepository) {
        this.courseRepository = courseRepository;
        this.courseMembershipRepository = courseMembershipRepository;
    }

    // Dashboard: Display student information and enrolled courses.
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        User user = principal.getUser();
        model.addAttribute("user", user);
        model.addAttribute("enrolledCourses", user.getEnrolledCourses());
        return "student/dashboard";
    }

    // View details for a specific enrolled course.
    @GetMapping("/course/{courseId}")
    public String courseDetail(@PathVariable("courseId") Integer courseId,
                               @AuthenticationPrincipal CustomOAuth2User principal,
                               Model model) {
        User user = principal.getUser();
        // Check if the user is actually enrolled in the course.
        Course course = user.getEnrolledCourses().stream()
                .filter(c -> c.getId().equals(courseId))
                .findFirst()
                .orElse(null);

        if (course == null) {
            // Redirect with an error if not enrolled.
            return "redirect:/student/dashboard?error=courseNotFound";
        }

        model.addAttribute("course", course);
        return "student/course_detail";
    }

    // Show the join course page where students can enter an invite code.
    @GetMapping("/course/join")
    public String showJoinCourseForm() {
        return "student/join_course";
    }

    // Process the join course form submission.
    @PostMapping("/course/join")
    public String joinCourse(@AuthenticationPrincipal CustomOAuth2User principal,
                             @RequestParam("inviteCode") String inviteCode,
                             Model model) {
        User user = principal.getUser();
        // Find course by invite code.
        Course course = courseRepository.findByInviteCode(inviteCode);
        if (course == null) {
            model.addAttribute("error", "Invalid invite code.");
            return "student/join_course";
        }
        // Check if the student is already enrolled in the course.
        boolean alreadyEnrolled = user.getEnrolledCourses().stream()
                .anyMatch(c -> c.getId().equals(course.getId()));
        if (alreadyEnrolled) {
            model.addAttribute("error", "You are already enrolled in this course.");
            return "student/join_course";
        }
        // Create a new course membership for the student.
        CourseMembership membership = new CourseMembership();
        membership.setCourse(course);
        membership.setUser(user);
        membership.setRole(CourseMembership.Role.STUDENT);
        courseMembershipRepository.save(membership);

        return "redirect:/student/dashboard?joined=true";
    }
}
