package com.classroom.clone.controller;

import com.classroom.clone.model.Assignment;
import com.classroom.clone.model.Course;
import com.classroom.clone.service.CustomOAuth2User;
import com.classroom.clone.model.User;
import com.classroom.clone.service.AssignmentService;
import com.classroom.clone.service.CourseService;
import com.classroom.clone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private AssignmentService assignmentService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        User teacher = userService.findByEmail(principal.getEmail());
        List<Course> teachingCourses = courseService.getTeachingCourses(teacher.getId());

        model.addAttribute("user", teacher);
        model.addAttribute("courses", teachingCourses);
        return "teacher/dashboard";
    }

    @GetMapping("/courses/{courseId}")
    public String viewCourse(@AuthenticationPrincipal CustomOAuth2User principal,
                             @PathVariable("courseId") Integer courseId,
                             Model model) {
        User teacher = userService.findByEmail(principal.getEmail());
        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (courseOpt.isPresent() && courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            Course course = courseOpt.get();
            List<User> students = userService.getStudentsInCourse(courseId);
            List<User> teachingAssistants = userService.getTAsInCourse(courseId);
            List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(courseId);

            model.addAttribute("user", teacher);
            model.addAttribute("course", course);
            model.addAttribute("students", students);
            model.addAttribute("teachingAssistants", teachingAssistants);
            model.addAttribute("assignments", assignments);
            return "teacher/course-detail";
        }

        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/courses/create")
    public String showCreateCourseForm(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        User teacher = userService.findByEmail(principal.getEmail());
        model.addAttribute("user", teacher);
        model.addAttribute("course", new Course());
        return "teacher/course-form";
    }

    @PostMapping("/courses/create")
    public String createCourse(@AuthenticationPrincipal CustomOAuth2User principal,
                               @ModelAttribute Course course,
                               RedirectAttributes redirectAttributes) {
        User teacher = userService.findByEmail(principal.getEmail());
        course.setTeacherId(teacher.getId());

        // Generate unique course code and invite code
        String courseCode = courseService.generateUniqueCourseCode();
        String inviteCode = courseService.generateUniqueInviteCode();
        course.setCourseCode(courseCode);
        course.setInviteCode(inviteCode);

        Course savedCourse = courseService.saveCourse(course);

        // Add the teacher as a course member
        courseService.addCourseMembership(savedCourse.getId(), teacher.getId(), "TEACHER");

        redirectAttributes.addFlashAttribute("message", "Course created successfully!");
        return "redirect:/teacher/courses/" + savedCourse.getId();
    }

    @GetMapping("/courses/{courseId}/edit")
    public String showEditCourseForm(@AuthenticationPrincipal CustomOAuth2User principal,
                                     @PathVariable("courseId") Integer courseId,
                                     Model model) {
        User teacher = userService.findByEmail(principal.getEmail());
        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (courseOpt.isPresent() && courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            model.addAttribute("user", teacher);
            model.addAttribute("course", courseOpt.get());
            return "teacher/course-form";
        }

        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/courses/{courseId}/edit")
    public String updateCourse(@AuthenticationPrincipal CustomOAuth2User principal,
                               @PathVariable("courseId") Integer courseId,
                               @ModelAttribute Course course,
                               RedirectAttributes redirectAttributes) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            course.setId(courseId);
            courseService.updateCourse(course);
            redirectAttributes.addFlashAttribute("message", "Course updated successfully!");
        }

        return "redirect:/teacher/courses/" + courseId;
    }

    @GetMapping("/courses/{courseId}/archive")
    public String archiveCourse(@AuthenticationPrincipal CustomOAuth2User principal,
                                @PathVariable("courseId") Integer courseId,
                                RedirectAttributes redirectAttributes) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            courseService.archiveCourse(courseId);
            redirectAttributes.addFlashAttribute("message", "Course archived successfully!");
        }

        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/courses/{courseId}/assignments/create")
    public String showCreateAssignmentForm(@AuthenticationPrincipal CustomOAuth2User principal,
                                           @PathVariable("courseId") Integer courseId,
                                           Model model) {
        User teacher = userService.findByEmail(principal.getEmail());
        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (courseOpt.isPresent() && courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            model.addAttribute("user", teacher);
            model.addAttribute("course", courseOpt.get());
            model.addAttribute("assignment", new Assignment());
            return "teacher/assignment-form";
        }

        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/courses/{courseId}/assignments/create")
    public String createAssignment(@AuthenticationPrincipal CustomOAuth2User principal,
                                   @PathVariable("courseId") Integer courseId,
                                   @ModelAttribute Assignment assignment,
                                   RedirectAttributes redirectAttributes) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            assignment.setCourseId(courseId);
            Assignment savedAssignment = assignmentService.saveAssignment(assignment);
            redirectAttributes.addFlashAttribute("message", "Assignment created successfully!");
            return "redirect:/teacher/courses/" + courseId + "/assignments/" + savedAssignment.getId();
        }

        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/courses/{courseId}/assignments/{assignmentId}")
    public String viewAssignment(@AuthenticationPrincipal CustomOAuth2User principal,
                                 @PathVariable("courseId") Integer courseId,
                                 @PathVariable("assignmentId") Integer assignmentId,
                                 Model model) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            Optional<Assignment> assignmentOpt = assignmentService.getAssignmentById(assignmentId);
            Optional<Course> courseOpt = courseService.getCourseById(courseId);

            if (assignmentOpt.isPresent() && courseOpt.isPresent()) {
                Assignment assignment = assignmentOpt.get();
                Course course = courseOpt.get();

                model.addAttribute("user", teacher);
                model.addAttribute("course", course);
                model.addAttribute("assignment", assignment);
                model.addAttribute("submissions", assignmentService.getSubmissionsForAssignment(assignmentId));

                return "teacher/assignment-detail";
            }
        }

        return "redirect:/teacher/courses/" + courseId;
    }

    @GetMapping("/courses/{courseId}/students")
    public String viewStudents(@AuthenticationPrincipal CustomOAuth2User principal,
                               @PathVariable("courseId") Integer courseId,
                               Model model) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            Optional<Course> courseOpt = courseService.getCourseById(courseId);

            if (courseOpt.isPresent()) {
                List<User> students = userService.getStudentsInCourse(courseId);

                model.addAttribute("user", teacher);
                model.addAttribute("course", courseOpt.get());
                model.addAttribute("students", students);

                return "teacher/course-students";
            }
        }

        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/courses/{courseId}/tas")
    public String viewTAs(@AuthenticationPrincipal CustomOAuth2User principal,
                          @PathVariable("courseId") Integer courseId,
                          Model model) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            Optional<Course> courseOpt = courseService.getCourseById(courseId);

            if (courseOpt.isPresent()) {
                List<User> tas = userService.getTAsInCourse(courseId);

                model.addAttribute("user", teacher);
                model.addAttribute("course", courseOpt.get());
                model.addAttribute("teachingAssistants", tas);

                return "teacher/course-tas";
            }
        }

        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/courses/{courseId}/add-ta")
    public String showAddTAForm(@AuthenticationPrincipal CustomOAuth2User principal,
                                @PathVariable("courseId") Integer courseId,
                                Model model) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            Optional<Course> courseOpt = courseService.getCourseById(courseId);

            if (courseOpt.isPresent()) {
                model.addAttribute("user", teacher);
                model.addAttribute("course", courseOpt.get());
                return "teacher/add-ta-form";
            }
        }

        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/courses/{courseId}/add-ta")
    public String addTA(@AuthenticationPrincipal CustomOAuth2User principal,
                        @PathVariable("courseId") Integer courseId,
                        @RequestParam("email") String taEmail,
                        RedirectAttributes redirectAttributes) {
        User teacher = userService.findByEmail(principal.getEmail());

        if (courseService.isTeacherOfCourse(teacher.getId(), courseId)) {
            Optional<User> taOpt = userService.findOptionalByEmail(taEmail);

            if (taOpt.isPresent()) {
                User ta = taOpt.get();
                courseService.addCourseMembership(courseId, ta.getId(), "TA");
                redirectAttributes.addFlashAttribute("message", "Teaching Assistant added successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "User with email " + taEmail + " not found.");
            }

            return "redirect:/teacher/courses/" + courseId + "/tas";
        }

        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/notifications")
    public String viewNotifications(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        User teacher = userService.findByEmail(principal.getEmail());
        model.addAttribute("user", teacher);
        model.addAttribute("notifications", userService.getUserNotifications(teacher.getId()));
        return "teacher/notifications";
    }
}