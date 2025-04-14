package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "course_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"}))
@Data
@Setter
@Getter
@NoArgsConstructor
public class CourseMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private static User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public User getUser() {
        return user;
    }

    public enum Role {
        TEACHER, STUDENT, TA;

        public boolean equalsIgnoreCase(String student) {
            return user.getUserType() == User.UserType.valueOf(student.toUpperCase());
        }
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}