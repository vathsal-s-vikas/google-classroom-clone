package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "permission_settings")
@Getter
@Setter
public class PermissionSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "ta_id")
    private User ta;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private boolean canGradeSubmissions;
    private boolean canPostContent;
    private boolean canManageTeams;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    /**
     * Creates a new PermissionSettings instance with default permissions.
     *
     * @param ta The teaching assistant
     * @param course The course
     * @return A new permission settings instance with default values
     */
    public static PermissionSettings createDefaultPermissions(User ta, Course course) {
        PermissionSettings permissions = new PermissionSettings();
        permissions.setTa(ta);
        permissions.setCourse(course);
        permissions.setCanGradeSubmissions(true);
        permissions.setCanPostContent(true);
        permissions.setCanManageTeams(false);
        return permissions;
    }

    /**
     * Checks if the TA has any of the available permissions.
     *
     * @return true if the TA has at least one permission
     */
    public boolean hasAnyPermission() {
        return canGradeSubmissions || canPostContent || canManageTeams;
    }

    /**
     * Checks if the TA has all possible permissions.
     *
     * @return true if the TA has all permissions
     */
    public boolean hasAllPermissions() {
        return canGradeSubmissions && canPostContent && canManageTeams;
    }

    /**
     * Creates a copy of this permissions object for another TA.
     *
     * @param newTa The TA to create permissions for
     * @return A new PermissionSettings instance with the same permissions
     */
    public PermissionSettings copyPermissionsFor(User newTa) {
        if (newTa == null || course == null) {
            throw new IllegalArgumentException("TA and Course must not be null");
        }

        PermissionSettings newPermissions = new PermissionSettings();
        newPermissions.setTa(newTa);
        newPermissions.setCourse(course);
        newPermissions.setCanGradeSubmissions(this.canGradeSubmissions);
        newPermissions.setCanPostContent(this.canPostContent);
        newPermissions.setCanManageTeams(this.canManageTeams);
        return newPermissions;
    }

    /**
     * Resets all permissions to their default values.
     */
    public void resetToDefault() {
        this.canGradeSubmissions = true;
        this.canPostContent = true;
        this.canManageTeams = false;
    }

    /**
     * Grants all permissions to this TA.
     */
    public void grantAllPermissions() {
        this.canGradeSubmissions = true;
        this.canPostContent = true;
        this.canManageTeams = true;
    }

    /**
     * Revokes all permissions from this TA.
     */
    public void revokeAllPermissions() {
        this.canGradeSubmissions = false;
        this.canPostContent = false;
        this.canManageTeams = false;
    }

    /**
     * Checks if this permission setting applies to the given TA and course.
     *
     * @param taId The TA's ID
     * @param courseId The course ID
     * @return true if this permission applies to the specified TA and course
     */
    public boolean appliesTo(Long taId, Long courseId) {
        return ta != null && course != null &&
                ta.getId().equals(taId) &&
                course.getId().equals(courseId);
    }

    @Override
    public String toString() {
        return "PermissionSettings{" +
                "id=" + id +
                ", ta=" + (ta != null ? ta.getId() : "null") +
                ", course=" + (course != null ? course.getId() : "null") +
                ", canGrade=" + canGradeSubmissions +
                ", canPostContent=" + canPostContent +
                ", canManageTeams=" + canManageTeams +
                "}";
    }
}