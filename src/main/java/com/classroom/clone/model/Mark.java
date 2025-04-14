package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "marks")
@Getter
@Setter
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "raw_marks")
    private int rawMarks;

    @Column(name = "penalty_percentage")
    private int penaltyPercentage;

    @Column(name = "final_marks")
    private int finalMarks;

    @Lob
    private String feedback;

    @ManyToOne
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // Don't update evaluatedAt on every change to avoid losing the original grading timestamp
        calculateFinalMarks();
    }

    /**
     * Calculates the final marks after applying the penalty percentage.
     */
    public void calculateFinalMarks() {
        if (penaltyPercentage < 0) {
            penaltyPercentage = 0;
        } else if (penaltyPercentage > 100) {
            penaltyPercentage = 100;
        }

        finalMarks = (int) Math.round(rawMarks * (1 - (penaltyPercentage / 100.0)));

        // Ensure final marks is not negative
        if (finalMarks < 0) {
            finalMarks = 0;
        }
    }

    /**
     * Applies a late submission penalty based on the submission's lateness.
     *
     * @return true if penalty was applied
     */
    public boolean applyLatePenalty() {
        if (submission == null) {
            return false;
        }

        int calculatedPenalty = submission.calculatePenaltyPercentage();
        if (calculatedPenalty > 0) {
            this.penaltyPercentage = calculatedPenalty;
            calculateFinalMarks();
            return true;
        }

        return false;
    }

    /**
     * Gets the percentage score based on max marks.
     *
     * @return percentage score (0-100)
     */
    public int getPercentageScore() {
        if (submission == null || submission.getAssignment() == null) {
            return 0;
        }

        int maxMarks = submission.getAssignment().getMaxMarks();
        if (maxMarks <= 0) {
            return 0;
        }

        return (int) Math.round((finalMarks * 100.0) / maxMarks);
    }

    /**
     * Gets the letter grade based on percentage score.
     *
     * @return letter grade
     */
    public String getLetterGrade() {
        int percentage = getPercentageScore();

        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    /**
     * Creates a grade history entry for this mark.
     * This is a placeholder that would integrate with your MarkHistory entity.
     *
     * @param reason Reason for the change
     * @param changedBy User who made the change
     * @return true if history was recorded
     */
    public boolean recordHistory(String reason, User changedBy) {
        // This would create and save a MarkHistory entity
        // Implementation depends on your MarkHistory model and repository
        return true;
    }

    /**
     * Checks if the mark is passing.
     *
     * @return true if the mark is a passing grade (≥ 60%)
     */
    public boolean isPassing() {
        return getPercentageScore() >= 60;
    }

    /**
     * Builder pattern for creating a new Mark.
     */
    public static class Builder {
        private Mark mark = new Mark();

        public Builder submission(Submission submission) {
            mark.setSubmission(submission);
            return this;
        }

        public Builder rawMarks(int rawMarks) {
            mark.setRawMarks(rawMarks);
            return this;
        }

        public Builder feedback(String feedback) {
            mark.setFeedback(feedback);
            return this;
        }

        public Builder evaluator(User evaluator) {
            mark.setEvaluator(evaluator);
            return this;
        }

        public Mark build() {
            // Apply late penalty if applicable
            mark.applyLatePenalty();

            // Calculate final marks
            if (mark.getFinalMarks() == 0) {
                mark.calculateFinalMarks();
            }

            return mark;
        }
    }

    /**
     * Creates a new builder for Mark.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Mark{" +
                "id=" + id +
                ", raw=" + rawMarks +
                ", penalty=" + penaltyPercentage + "%" +
                ", final=" + finalMarks +
                ", percentage=" + getPercentageScore() + "%" +
                ", grade='" + getLetterGrade() + "'" +
                "}";
    }
}