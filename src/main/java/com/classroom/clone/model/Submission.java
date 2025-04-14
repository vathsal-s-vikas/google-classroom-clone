package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "submissions")
@Getter
@Setter
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "submitted_at")
    private LocalDateTime submissionTime;

    @Lob
    @Column(name = "submission_text")
    private String submissionText;

    @ElementCollection
    @CollectionTable(
            name = "submission_attachments",
            joinColumns = @JoinColumn(name = "submission_id")
    )
    @Column(name = "file_url")
    private List<String> attachmentUris = new ArrayList<>();

    @Column(name = "is_late")
    private boolean late;

    @Column(name = "days_late")
    private Integer daysLate;

    @Column(name = "is_evaluated")
    private boolean evaluated;

    @ManyToOne
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL)
    private Mark mark;

    @PrePersist
    protected void onCreate() {
        if (submissionTime == null) {
            submissionTime = LocalDateTime.now();
        }

        calculateLateStatus();
    }

    /**
     * Calculates if the submission is late and how many days late.
     */
    public void calculateLateStatus() {
        if (assignment != null && submissionTime != null) {
            LocalDateTime deadline = assignment.getDeadline();

            if (deadline != null && submissionTime.isAfter(deadline)) {
                this.late = true;
                // Calculate days late (round up partial days)
                long hoursDifference = ChronoUnit.HOURS.between(deadline, submissionTime);
                this.daysLate = (int) Math.ceil(hoursDifference / 24.0);
            } else {
                this.late = false;
                this.daysLate = 0;
            }
        }
    }

    /**
     * Checks if this submission is submitted by a team.
     *
     * @return true if this is a team submission
     */
    public boolean isTeamSubmission() {
        return team != null;
    }

    /**
     * Checks if this submission is submitted by an individual student.
     *
     * @return true if this is an individual submission
     */
    public boolean isIndividualSubmission() {
        return student != null;
    }

    /**
     * Checks if the submission is accepted based on late submission rules.
     *
     * @return true if submission is accepted
     */
    public boolean isAccepted() {
        if (!late) {
            return true;
        }

        if (assignment != null) {
            boolean lateSubmissionsAllowed = assignment.isLateSubmissionAllowed();
            Integer maxLateDays = assignment.getMaxLateDays();

            if (!lateSubmissionsAllowed) {
                return false;
            }

            return maxLateDays == null || daysLate <= maxLateDays;
        }

        return false;
    }

    /**
     * Calculates the penalty percentage for late submission.
     *
     * @return the penalty percentage (0-100)
     */
    public int calculatePenaltyPercentage() {
        if (!late || assignment == null) {
            return 0;
        }

        int penaltyPerDay = assignment.getLatePenaltyPercentage();
        Integer maxLateDays = assignment.getMaxLateDays();

        // Apply penalty based on days late
        int effectiveDaysLate = daysLate;
        if (maxLateDays != null && daysLate > maxLateDays) {
            effectiveDaysLate = maxLateDays;
        }

        int penalty = effectiveDaysLate * penaltyPerDay;

        // Cap at 100%
        return Math.min(penalty, 100);
    }

    /**
     * Gets the mark for this submission.
     *
     * @return Optional containing mark if it exists
     */
    public Optional<Mark> getMark() {
        return Optional.ofNullable(mark);
    }

    /**
     * Gets the final score after applying any late penalties.
     *
     * @return the final score or 0 if not graded
     */
    public int getFinalScore() {
        if (mark != null) {
            return mark.getFinalMarks();
        }
        return 0;
    }

    /**
     * Gets status of the submission.
     *
     * @return submission status
     */
    public SubmissionStatus getStatus() {
        if (!isAccepted()) {
            return SubmissionStatus.REJECTED;
        } else if (evaluated && mark != null) {
            return SubmissionStatus.GRADED;
        } else {
            return SubmissionStatus.PENDING;
        }
    }

    /**
     * Adds an attachment to the submission.
     *
     * @param fileUrl URL to the attachment
     * @return true if the attachment was added
     */
    public boolean addAttachment(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        if (attachmentUris == null) {
            attachmentUris = new ArrayList<>();
        }

        return attachmentUris.add(fileUrl);
    }

    /**
     * Gets the number of days remaining before the deadline if not yet submitted,
     * or null if already submitted.
     *
     * @return days remaining or null
     */
    public Integer getDaysUntilDeadline() {
        if (assignment == null || assignment.getDeadline() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = assignment.getDeadline();

        if (now.isAfter(deadline)) {
            return 0;
        }

        return (int) ChronoUnit.DAYS.between(now, deadline);
    }

    /**
     * Gets users who can view this submission.
     *
     * @return List of user IDs that can access this submission
     */
    public List<Long> getAuthorizedViewers() {
        List<Long> viewers = new ArrayList<>();

        // Add student who submitted or team members
        if (student != null) {
            viewers.add(Long.valueOf(student.getId()));
        } else if (team != null && team.getMembers() != null) {
            team.getMembers().forEach(member -> viewers.add(Long.valueOf(member.getId())));
        }

        // Add course teachers and TAs with grading permissions
        if (assignment != null && assignment.getCourse() != null) {
            Course course = assignment.getCourse();

            // Add course teacher
            if (course.getTeacher() != null) {
                viewers.add(Long.valueOf(course.getTeacher().getId()));
            }

            // Logic for TAs would depend on your permission model
            // This is just a placeholder assuming you have a method to get TAs with permissions
            // course.getTasWithGradingPermissions().forEach(ta -> viewers.add(ta.getId()));
        }

        return viewers;
    }

    /**
     * Enum for submission status states.
     */
    public enum SubmissionStatus {
        PENDING,
        GRADED,
        REJECTED
    }
}