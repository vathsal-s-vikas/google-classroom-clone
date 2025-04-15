package com.classroom.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
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
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "submission_text")
    private String submissionText;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "is_late")
    private boolean isLate;

    @Column(name = "days_late")
    private Integer daysLate;

    @Column(name = "is_evaluated")
    private boolean isEvaluated;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubmissionAttachment> attachments = new HashSet<>();

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private Mark mark;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    @AssertTrue(message = "Either student or team must be provided, but not both")
    private boolean isValidSubmission() {
        return (student != null && team == null) || (student == null && team != null);
    }
}