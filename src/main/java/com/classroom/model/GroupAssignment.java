package com.classroom.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "group_assignments")
public class GroupAssignment {
    @Id
    @Column(name = "assignment_id")
    private Long assignmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @Column(name = "min_team_size", nullable = false)
    private Integer minTeamSize;

    @Column(name = "max_team_size", nullable = false)
    private Integer maxTeamSize;

    @Column(name = "allow_self_forming_teams")
    private boolean allowSelfFormingTeams;

    @Column(name = "require_project_title")
    private boolean requireProjectTitle;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Team> teams = new HashSet<>();
}