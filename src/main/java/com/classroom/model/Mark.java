package com.classroom.model;

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
@Table(name = "marks")
public class Mark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(name = "raw_marks", nullable = false)
    private Integer rawMarks;

    @Column(name = "penalty_percentage", nullable = false)
    private Integer penaltyPercentage;

    @Column(name = "final_marks", nullable = false)
    private Integer finalMarks;

    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @OneToMany(mappedBy = "mark", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MarkHistory> history = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }
}