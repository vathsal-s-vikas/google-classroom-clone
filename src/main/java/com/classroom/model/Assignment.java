package com.classroom.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "assignments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @Column(nullable = false)
    private String title;

    private String description;

    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false)
    private AssignmentType assignmentType;

    @Column(name = "max_marks", nullable = false)
    private Integer maxMarks;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "late_submission_allowed")
    private boolean lateSubmissionAllowed;

    @Column(name = "late_penalty_percentage")
    private Integer latePenaltyPercentage;

    @Column(name = "max_late_days")
    private Integer maxLateDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Submission> submissions = new HashSet<>();

    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private GroupAssignment groupAssignment;
    
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<AssignmentAttachment> attachments = new HashSet<>();
    
    // Manual getter and setter for course field
    public Course getCourse() {
        return course;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AssignmentType {
        INDIVIDUAL, GROUP
    }
}