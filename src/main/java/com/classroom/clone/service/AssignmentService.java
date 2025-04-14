package com.classroom.clone.service;

import com.classroom.clone.exception.PermissionDeniedException;
import com.classroom.clone.exception.ResourceNotFoundException;
import com.classroom.clone.model.Assignment;
import com.classroom.clone.model.Course;
import com.classroom.clone.model.User;
import com.classroom.clone.repository.AssignmentRepository;
import com.classroom.clone.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final PermissionService permissionService;
    private final CourseService courseService;

    @Autowired
    public AssignmentService(AssignmentRepository assignmentRepository,
                             CourseRepository courseRepository,
                             PermissionService permissionService,
                             CourseService courseService) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.permissionService = permissionService;
        this.courseService = courseService;
    }

    /**
     * Create a new assignment in a course.
     */
    public Assignment createAssignment(Assignment assignment, Integer courseId, User creator) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        boolean isTeacher = course.getTeacher().getId().equals(creator.getId());
        boolean isTAWithPermission = permissionService.canTAPostContent(creator.getId(), courseId);

        if (!isTeacher && !isTAWithPermission) {
            throw new PermissionDeniedException("You don't have permission to create assignments for this course.");
        }

        validateAssignment(assignment);

        assignment.setCourse(course);
        return assignmentRepository.save(assignment);
    }

    /**
     * Update an existing assignment.
     */
    public Assignment updateAssignment(Integer assignmentId, Assignment updatedAssignment, User editor) {
        Assignment existingAssignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        Course course = existingAssignment.getCourse();
        boolean isTeacher = course.getTeacher().getId().equals(editor.getId());
        boolean isTAWithPermission = permissionService.canTAPostContent(editor.getId(), course.getId());

        if (!isTeacher && !isTAWithPermission) {
            throw new PermissionDeniedException("You don't have permission to update this assignment.");
        }

        validateAssignment(updatedAssignment);

        existingAssignment.setTitle(updatedAssignment.getTitle());
        existingAssignment.setDescription(updatedAssignment.getDescription());
        existingAssignment.setInstructions(updatedAssignment.getInstructions());
        existingAssignment.setAssignmentType(updatedAssignment.getAssignmentType());
        existingAssignment.setMaxMarks(updatedAssignment.getMaxMarks());
        existingAssignment.setDeadline(updatedAssignment.getDeadline());
        existingAssignment.setLateSubmissionAllowed(updatedAssignment.isLateSubmissionAllowed());
        existingAssignment.setLatePenaltyPercentage(updatedAssignment.getLatePenaltyPercentage());
        existingAssignment.setMaxLateDays(updatedAssignment.getMaxLateDays());

        return assignmentRepository.save(existingAssignment);
    }

    /**
     * Get all assignments for a course.
     */
    public List<Assignment> getAssignmentsByCourse(Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        return assignmentRepository.findByCourseOrderByDeadlineAsc(course);
    }

    /**
     * Get upcoming assignments for a course.
     */
    public List<Assignment> getUpcomingAssignmentsByCourse(Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        return assignmentRepository.findUpcomingAssignmentsByCourse(course, LocalDateTime.now());
    }

    /**
     * Get upcoming assignments across all courses for a user.
     */
    public List<Assignment> getUpcomingAssignmentsForUser(User user) {
        List<Course> userCourses = courseService.getAllUserCourses(user);
        return assignmentRepository.findUpcomingAssignmentsByCoursesIn(userCourses, LocalDateTime.now());
    }

    /**
     * Get a specific assignment by ID.
     */
    public Optional<Assignment> getAssignmentById(Integer assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }

    /**
     * Delete an assignment.
     */
    public void deleteAssignment(Integer assignmentId, User deleter) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        Course course = assignment.getCourse();
        if (!course.getTeacher().getId().equals(deleter.getId())) {
            throw new PermissionDeniedException("Only the teacher can delete assignments.");
        }

        assignmentRepository.delete(assignment);
    }

    /**
     * Check if a user can view an assignment.
     */
    public boolean canUserViewAssignment(Integer assignmentId, Integer userId) {
        Optional<Assignment> assignmentOptional = assignmentRepository.findById(assignmentId);
        if (assignmentOptional.isEmpty()) return false;

        Assignment assignment = assignmentOptional.get();
        Course course = assignment.getCourse();

        if (course.getTeacher().getId().equals(userId)) {
            return true;
        }

        return course.getMemberships().stream()
                .anyMatch(membership ->
                        membership.getUser().getId().equals(userId) &&
                                (membership.getRole().equalsIgnoreCase("STUDENT") ||
                                        membership.getRole().equalsIgnoreCase("TA")));
    }

    /**
     * Basic validation for an assignment.
     */
    private void validateAssignment(Assignment assignment) {
        if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title cannot be empty.");
        }

        if (assignment.getMaxMarks() != null && assignment.getMaxMarks() < 0) {
            throw new IllegalArgumentException("Max marks must be non-negative.");
        }

        if (assignment.getDeadline() != null && assignment.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Deadline must be in the future.");
        }

        if (assignment.isLateSubmissionAllowed() && (assignment.getMaxLateDays() == null || assignment.getLatePenaltyPercentage() == null)) {
            throw new IllegalArgumentException("Late submission policy must include max late days and penalty percentage.");
        }
    }
}
