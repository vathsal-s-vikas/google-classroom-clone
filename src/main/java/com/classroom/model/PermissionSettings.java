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
@Table(name = "permission_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_id", "ta_id"})
})
public class PermissionSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ta_id", nullable = false)
    private User ta;

    @Column(name = "can_grade")
    private boolean canGrade;

    @Column(name = "can_post_content")
    private boolean canPostContent;

    @Column(name = "can_manage_teams")
    private boolean canManageTeams;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}