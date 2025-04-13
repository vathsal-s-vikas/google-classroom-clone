package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private Set<CourseMembership> courseMemberships = new HashSet<>();

    public enum OAuthProvider {
        GOOGLE, MICROSOFT, GITHUB
    }

    public enum UserType {
        TEACHER, STUDENT, TA
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isTeacher() {
        return this.userType == UserType.TEACHER;
    }

    public boolean isStudent() {
        return this.userType == UserType.STUDENT;
    }

    public boolean isTA() {
        return this.userType == UserType.TA;
    }

    public Set<Course> getTeachingCourses() {
        Set<Course> teachingCourses = new HashSet<>();
        for (CourseMembership membership : courseMemberships) {
            if (membership.getRole() == CourseMembership.Role.TEACHER) {
                teachingCourses.add(membership.getCourse());
            }
        }
        return teachingCourses;
    }

    public Set<Course> getEnrolledCourses() {
        Set<Course> enrolledCourses = new HashSet<>();
        for (CourseMembership membership : courseMemberships) {
            if (membership.getRole() == CourseMembership.Role.STUDENT) {
                enrolledCourses.add(membership.getCourse());
            }
        }
        return enrolledCourses;
    }

    public Set<Course> getAssistingCourses() {
        Set<Course> assistingCourses = new HashSet<>();
        for (CourseMembership membership : courseMemberships) {
            if (membership.getRole() == CourseMembership.Role.TA) {
                assistingCourses.add(membership.getCourse());
            }
        }
        return assistingCourses;
    }
}