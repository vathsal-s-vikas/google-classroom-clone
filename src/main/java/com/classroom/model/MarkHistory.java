package com.classroom.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "mark_history")
public class MarkHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mark_id", nullable = false)
    private Mark mark;

    @Column(name = "previous_raw_marks", nullable = false)
    private Integer previousRawMarks;

    @Column(name = "previous_final_marks", nullable = false)
    private Integer previousFinalMarks;

    @Column(name = "previous_feedback")
    private String previousFeedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    private String reason;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}