package com.classroom.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "teams", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"assignment_id", "name"}),
        @UniqueConstraint(columnNames = {"assignment_id", "project_title"})
})
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private GroupAssignment assignment;

    @Column(nullable = false)
    private String name;

    @Column(name = "project_title")
    private String projectTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Date updatedAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMembership> memberships = new ArrayList<>();

    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private Submission submission;

    /**
     * Add a student to this team
     * @param student The student to add
     * @param accepted Whether the membership is already accepted
     * @return The created membership
     */
    public TeamMembership addMember(User student, boolean accepted) {
        TeamMembership membership = new TeamMembership();
        membership.setTeam(this);
        membership.setStudent(student);
        membership.setAccepted(accepted);
        this.memberships.add(membership);
        return membership;
    }

    /**
     * Check if a student is a member of this team
     * @param student The student to check
     * @return true if the student is a member, false otherwise
     */
    public boolean hasMember(User student) {
        return this.memberships.stream()
                .anyMatch(m -> m.getStudent().equals(student) && m.isAccepted());
    }

    /**
     * Get the pending membership for a student, if any
     * @param student The student to check
     * @return The pending membership or null if none exists
     */
    public TeamMembership getPendingMembership(User student) {
        return this.memberships.stream()
                .filter(m -> m.getStudent().equals(student) && !m.isAccepted())
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove a student from this team
     * @param student The student to remove
     */
    public void removeMember(User student) {
        this.memberships.removeIf(m -> m.getStudent().equals(student));
    }
}