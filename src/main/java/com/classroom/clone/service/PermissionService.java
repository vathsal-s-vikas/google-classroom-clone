package com.classroom.clone.service;

import com.classroom.clone.exception.PermissionDeniedException;
import com.classroom.clone.model.Course;
import com.classroom.clone.model.User;
import com.classroom.clone.repository.PermissionSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final PermissionSettingsRepository permissionSettingsRepository;
    private final CourseService courseService;

    @Autowired
    public PermissionService(PermissionSettingsRepository permissionSettingsRepository, CourseService courseService) {
        this.permissionSettingsRepository = permissionSettingsRepository;
        this.courseService = courseService;
    }

    /**
     * Checks if a Teaching Assistant has permission to grade assignments
     * @param ta The Teaching Assistant user
     * @param course The course to check permissions for
     * @return true if the TA has grading permission
     */
    public boolean canTAGrade(User ta, Course course) {
        // Verify the user is actually a TA for this course
        if (!courseService.isUserTA(ta, course)) {
            return false;
        }

        return permissionSettingsRepository.findByCourseAndTa(course, ta)
                .map(settings -> settings.isCanGrade())
                .orElse(true); // Default to true if no specific settings exist
    }

    /**
     * Checks if a Teaching Assistant has permission to post content
     * @param ta The Teaching Assistant user
     * @param course The course to check permissions for
     * @return true if the TA has content posting permission
     */
    public boolean canTAPostContent(User ta, Course course) {
        // Verify the user is actually a TA for this course
        if (!courseService.isUserTA(ta, course)) {
            return false;
        }

        return permissionSettingsRepository.findByCourseAndTa(course, ta)
                .map(settings -> settings.isCanPostContent())
                .orElse(true); // Default to true if no specific settings exist
    }

    /**
     * Checks if a Teaching Assistant has permission to manage teams
     * @param ta The Teaching Assistant user
     * @param course The course to check permissions for
     * @return true if the TA has team management permission
     */
    public boolean canTAManageTeams(User ta, Course course) {
        // Verify the user is actually a TA for this course
        if (!courseService.isUserTA(ta, course)) {
            return false;
        }

        return permissionSettingsRepository.findByCourseAndTa(course, ta)
                .map(settings -> settings.isCanManageTeams())
                .orElse(false); // Default to false if no specific settings exist
    }

    /**
     * Updates permission settings for a Teaching Assistant
     * @param course The course
     * @param ta The Teaching Assistant
     * @param canGrade Permission to grade assignments
     * @param canPostContent Permission to post content
     * @param canManageTeams Permission to manage teams
     */
    public void updateTAPermissions(Course course, User ta, boolean canGrade, boolean canPostContent, boolean canManageTeams) {
        // Verify the user is actually a TA for this course
        if (!courseService.isUserTA(ta, course)) {
            throw new PermissionDeniedException("User is not a TA for this course");
        }

        var permissionSettings = permissionSettingsRepository.findByCourseAndTa(course, ta)
                .orElse(new PermissionSettings(course, ta));

        permissionSettings.setCanGrade(canGrade);
        permissionSettings.setCanPostContent(canPostContent);
        permissionSettings.setCanManageTeams(canManageTeams);

        permissionSettingsRepository.save(permissionSettings);
    }

    /**
     * Enforces a permission check and throws an exception if the permission is denied
     * @param hasPermission The permission check result
     * @param message The error message if permission is denied
     * @throws PermissionDeniedException if the permission check fails
     */
    public void enforcePermission(boolean hasPermission, String message) {
        if (!hasPermission) {
            throw new PermissionDeniedException(message);
        }
    }
}