package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "course_code", nullable = false, unique = true)
    private String courseCode;

    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CourseMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Assignment> assignments = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Content> contents = new HashSet<>();

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
    public void addMembership(CourseMembership membership) {
        memberships.add(membership);
        membership.setCourse(this);
    }

    public void removeMembership(CourseMembership membership) {
        memberships.remove(membership);
        membership.setCourse(null);
    }

    public Set<User> getStudents() {
        Set<User> students = new HashSet<>();
        for (CourseMembership membership : memberships) {
            if (membership.getRole() == CourseMembership.Role.STUDENT) {
                students.add(membership.getUser());
            }
        }
        return students;
    }

    public Set<User> getTeachingAssistants() {
        Set<User> tas = new HashSet<>();
        for (CourseMembership membership : memberships) {
            if (membership.getRole() == CourseMembership.Role.TA) {
                tas.add(membership.getUser());
            }
        }
        return tas;
    }
}