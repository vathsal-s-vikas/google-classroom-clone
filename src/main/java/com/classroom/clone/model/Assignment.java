package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "assignments")
@Data
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false)
    private AssignmentType assignmentType;

    @Column(name = "max_marks", nullable = false)
    private Integer maxMarks;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "late_submission_allowed", nullable = false)
    private boolean lateSubmissionAllowed = true;

    @Column(name = "late_penalty_percentage", nullable = false)
    private Integer latePenaltyPercentage = 0;

    @Column(name = "max_late_days")
    private Integer maxLateDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Submission> submissions = new HashSet<>();

    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private GroupAssignment groupAssignment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if submissions are still open (not past deadline)
     * @return true if submission deadline has not passed
     */
    public boolean isSubmissionOpen() {
        return LocalDateTime.now().isBefore(deadline);
    }

    /**
     * Checks if late submissions are still allowed
     * @return true if late submissions are allowed and within max late days
     */
    public boolean isLateSubmissionAllowed() {
        if (!lateSubmissionAllowed) {
            return false;
        }

        if (maxLateDays == null) {
            return true; // No limit on late days
        }

        LocalDateTime latestPossibleSubmission = deadline.plusDays(maxLateDays);
        return LocalDateTime.now().isBefore(latestPossibleSubmission);
    }

    /**
     * Gets the number of days past deadline
     * @return days past deadline, or 0 if not past deadline
     */
    public int getDaysPastDeadline() {
        if (isSubmissionOpen()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(deadline, now);
        return (int) Math.max(0, daysBetween);
    }

    /**
     * Calculates the penalty percentage for current submission time
     * @return the penalty percentage based on days late and penalty rate
     */
    public int calculateCurrentPenalty() {
        int daysLate = getDaysPastDeadline();

        if (daysLate == 0 || !lateSubmissionAllowed) {
            return 0;
        }

        if (maxLateDays != null && daysLate > maxLateDays) {
            daysLate = maxLateDays;
        }

        int penalty = daysLate * latePenaltyPercentage;
        return Math.min(penalty, 100); // Cap at 100%
    }

    /**
     * Returns whether this is a group assignment
     * @return true if this is a group assignment
     */
    public boolean isGroupAssignment() {
        return assignmentType == AssignmentType.GROUP;
    }

    /**
     * Gets student's submission for this assignment
     * @param student the student
     * @return the submission or null if not found
     */
    public Submission getSubmissionByStudent(User student) {
        return submissions.stream()
                .filter(s -> s.getStudent() != null && s.getStudent().equals(student))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a team's submission for this assignment
     * @param team the team
     * @return the submission or null if not found
     */
    public Submission getSubmissionByTeam(Team team) {
        return submissions.stream()
                .filter(s -> s.getTeam() != null && s.getTeam().equals(team))
                .findFirst()
                .orElse(null);
    }
}