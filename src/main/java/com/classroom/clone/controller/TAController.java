package com.classroom.clone.controller;

import com.classroom.clone.model.*;
import com.classroom.clone.service.AssignmentService;
import com.classroom.clone.service.CourseService;
import com.classroom.clone.service.PermissionService;
import com.classroom.clone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import com.classroom.clone.service.CustomOAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/ta")
public class TAController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        User ta = userService.findByEmail(principal.getEmail());
        List<Course> assistingCourses = courseService.getAssistingCourses(ta.getId());

        model.addAttribute("user", ta);
        model.addAttribute("courses", assistingCourses);
        return "ta/dashboard";
    }

    @GetMapping("/courses/{courseId}")
    public String viewCourse(@AuthenticationPrincipal CustomOAuth2User principal,
                             @PathVariable("courseId") Integer courseId,
                             Model model) {
        User ta = userService.findByEmail(principal.getEmail());
        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (courseOpt.isPresent() && courseService.isTAOfCourse(ta.getId(), courseId)) {
            Course course = courseOpt.get();
            List<User> assignedStudents = userService.getAssignedStudents(ta.getId(), courseId);
            List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(courseId);
            PermissionSettings permissions = permissionService.getPermissions(courseId, ta.getId());

            model.addAttribute("user", ta);
            model.addAttribute("course", course);
            model.addAttribute("assignedStudents", assignedStudents);
            model.addAttribute("assignments", assignments);
            model.addAttribute("permissions", permissions);
            return "ta/course-detail";
        }

        return "redirect:/ta/dashboard";
    }

    @GetMapping("/courses/{courseId}/assignments/{assignmentId}")
    public String viewAssignment(@AuthenticationPrincipal CustomOAuth2User principal,
                                 @PathVariable("courseId") Integer courseId,
                                 @PathVariable("assignmentId") Integer assignmentId,
                                 Model model) {
        User ta = userService.findByEmail(principal.getEmail());

        if (courseService.isTAOfCourse(ta.getId(), courseId)) {
            PermissionSettings permissions = permissionService.getPermissions(courseId, ta.getId());
            Optional<Assignment> assignmentOpt = assignmentService.getAssignmentById(assignmentId);
            Optional<Course> courseOpt = courseService.getCourseById(courseId);

            if (assignmentOpt.isPresent() && courseOpt.isPresent()) {
                Assignment assignment = assignmentOpt.get();
                Course course = courseOpt.get();
                List<User> assignedStudents = userService.getAssignedStudents(ta.getId(), courseId);
                List<Submission> submissions = assignmentService.getSubmissionsForAssignmentByTA(assignmentId, ta.getId());

                model.addAttribute("user", ta);
                model.addAttribute("course", course);
                model.addAttribute("assignment", assignment);
                model.addAttribute("assignedStudents", assignedStudents);
                model.addAttribute("submissions", submissions);
                model.addAttribute("permissions", permissions);

                return "ta/assignment-detail";
            }
        }

        return "redirect:/ta/courses/" + courseId;
    }

    @GetMapping("/courses/{courseId}/submissions/{submissionId}/grade")
    public String showGradeSubmissionForm(@AuthenticationPrincipal CustomOAuth2User principal,
                                          @PathVariable("courseId") Integer courseId,
                                          @PathVariable("submissionId") Integer submissionId,
                                          Model model) {
        User ta = userService.findByEmail(principal.getEmail());

        if (courseService.isTAOfCourse(ta.getId(), courseId)) {
            PermissionSettings permissions = permissionService.getPermissions(courseId, ta.getId());

            if (permissions.getCanGrade()) {
                Optional<Submission> submissionOpt = assignmentService.getSubmissionById(submissionId);

                if (submissionOpt.isPresent()) {
                    Submission submission = submissionOpt.get();
                    Assignment assignment = assignmentService.getAssignmentById(submission.getAssignmentId()).get();

                    // Check if this is a student assigned to this TA
                    boolean isAuthorized = false;
                    if (submission.getStudentId() != null) {
                        isAuthorized = userService.isStudentAssignedToTA(submission.getStudentId(), ta.getId(), courseId);
                    } else if (submission.getTeamId() != null) {
                        // For team submissions, check if any team member is assigned to this TA
                        isAuthorized = assignmentService.isTeamAssignedToTA(submission.getTeamId(), ta.getId(), courseId);
                    }

                    if (isAuthorized) {
                        Mark mark = new Mark();
                        mark.setSubmissionId(submissionId);

                        model.addAttribute("user", ta);
                        model.addAttribute("course", courseService.getCourseById(courseId).get());
                        model.addAttribute("assignment", assignment);
                        model.addAttribute("submission", submission);
                        model.addAttribute("mark", mark);

                        return "ta/grade-submission-form";
                    }
                }
            }
        }

        return "redirect:/ta/courses/" + courseId;
    }

    @PostMapping("/courses/{courseId}/submissions/{submissionId}/grade")
    public String gradeSubmission(@AuthenticationPrincipal CustomOAuth2User principal,
                                  @PathVariable("courseId") Integer courseId,
                                  @PathVariable("submissionId") Integer submissionId,
                                  @ModelAttribute Mark mark,
                                  RedirectAttributes redirectAttributes) {
        User ta = userService.findByEmail(principal.getEmail());

        if (courseService.isTAOfCourse(ta.getId(), courseId)) {
            PermissionSettings permissions = permissionService.getPermissions(courseId, ta.getId());

            if (permissions.getCanGrade()) {
                Optional<Submission> submissionOpt = assignmentService.getSubmissionById(submissionId);

                if (submissionOpt.isPresent()) {
                    Submission submission = submissionOpt.get();

                    // Check if this is a student assigned to this TA
                    boolean isAuthorized = false;
                    if (submission.getStudentId() != null) {
                        isAuthorized = userService.isStudentAssignedToTA(submission.getStudentId(), ta.getId(), courseId);
                    } else if (submission.getTeamId() != null) {
                        // For team submissions, check if any team member is assigned to this TA
                        isAuthorized = assignmentService.isTeamAssignedToTA(submission.getTeamId(), ta.getId(), courseId);
                    }

                    if (isAuthorized) {
                        mark.setEvaluatorId(ta.getId());
                        assignmentService.saveMark(mark);

                        redirectAttributes.addFlashAttribute("message", "Submission graded successfully!");
                        return "redirect:/ta/courses/" + courseId + "/assignments/" + submission.getAssignmentId();
                    }
                }
            }
        }

        return "redirect:/ta/courses/" + courseId;
    }

    @GetMapping("/courses/{courseId}/content/create")
    public String showCreateContentForm(@AuthenticationPrincipal CustomOAuth2User principal,
                                        @PathVariable("courseId") Integer courseId,
                                        Model model) {
        User ta = userService.findByEmail(principal.getEmail());

        if (courseService.isTAOfCourse(ta.getId(), courseId)) {
            PermissionSettings permissions = permissionService.getPermissions(courseId, ta.getId());

            if (permissions.getCanPostContent()) {
                Course course = courseService.getCourseById(courseId).get();
                Content content = new Content();

                model.addAttribute("user", ta);
                model.addAttribute("course", course);
                model.addAttribute("content", content);

                return "ta/content-form";
            }
        }

        return "redirect:/ta/courses/" + courseId;
    }

    @PostMapping("/courses/{courseId}/content/create")
    public String createContent(@AuthenticationPrincipal CustomOAuth2User principal,
                                @PathVariable("courseId") Integer courseId,
                                @ModelAttribute Content content,
                                RedirectAttributes redirectAttributes) {
        User ta = userService.findByEmail(principal.getEmail());

        if (courseService.isTAOfCourse(ta.getId(), courseId)) {
            PermissionSettings permissions = permissionService.getPermissions(courseId, ta.getId());

            if (permissions.getCanPostContent()) {
                content.setCourseId(courseId);
                content.setUploaderId(ta.getId());

                courseService.saveContent(content);
                redirectAttributes.addFlashAttribute("message", "Content created successfully!");
            }
        }

        return "redirect:/ta/courses/" + courseId;
    }

    @GetMapping("/courses/{courseId}/students")
    public String viewAssignedStudents(@AuthenticationPrincipal CustomOAuth2User principal,
                                       @PathVariable("courseId") Integer courseId,
                                       Model model) {
        User ta = userService.findByEmail(principal.getEmail());

        if (courseService.isTAOfCourse(ta.getId(), courseId)) {
            Course course = courseService.getCourseById(courseId).get();
            List<User> assignedStudents = userService.getAssignedStudents(ta.getId(), courseId);

            model.addAttribute("user", ta);
            model.addAttribute("course", course);
            model.addAttribute("assignedStudents", assignedStudents);

            return "ta/assigned-students";
        }

        return "redirect:/ta/dashboard";
    }

    @GetMapping("/notifications")
    public String viewNotifications(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        User ta = userService.findByEmail(principal.getEmail());
        model.addAttribute("user", ta);
        model.addAttribute("notifications", userService.getUserNotifications(ta.getId()));
        return "ta/notifications";
    }
}