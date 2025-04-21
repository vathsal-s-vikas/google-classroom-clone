package com.classroom.model;

import java.time.LocalDateTime;

/**
 * DTO for team invitations
 */
public class TeamInvitation {
    private Long id;
    private Team team;
    private LocalDateTime timestamp;
    
    public TeamInvitation() {
        // Default constructor
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Team getTeam() {
        return team;
    }
    
    public void setTeam(Team team) {
        this.team = team;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
} 