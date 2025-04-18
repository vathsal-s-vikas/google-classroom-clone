package com.classroom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "team_memberships", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "student_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMembership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @Column(nullable = false)
    private boolean accepted;
    
    /**
     * Indicates whether this student is the team leader
     */
    @Column(name = "is_leader", nullable = false)
    private boolean leader;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invited_at")
    private Date invitedAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "joined_at")
    private Date joinedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private Date updatedAt;
    
    /**
     * Accept this membership invitation
     */
    public void accept() {
        this.accepted = true;
    }
    
    /**
     * Reject this membership invitation
     */
    public void reject() {
        this.accepted = false;
    }
    
    /**
     * Check if this member is a leader
     */
    public boolean isLeader() {
        return leader;
    }
    
    /**
     * Set leader status
     */
    public void setLeader(boolean leader) {
        this.leader = leader;
    }
}