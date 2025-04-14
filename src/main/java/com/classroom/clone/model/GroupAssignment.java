package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Setter
@Getter
@Entity
@DiscriminatorValue("GROUP")
public class GroupAssignment extends Assignment {

    private int minTeamSize = 2;
    private int maxTeamSize;
    private boolean allowSelfFormingTeams = true;
    private boolean requireProjectTitle = false;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
    private List<Team> teams;

    /**
     * Gets the team that a student belongs to for this assignment.
     *
     * @param studentId The ID of the student
     * @return Optional containing the team if the student is part of one, empty otherwise
     */
    public Optional<Team> getTeamByStudent(Integer studentId) {
        if (teams == null || studentId == null) {
            return Optional.empty();
        }

        return teams.stream()
                .filter(team -> team.getMembers().stream()
                        .anyMatch(member -> member.getStudent().getId().equals(studentId)))
                .findFirst();
    }

    /**
     * Checks if a team name is available for use in this assignment.
     *
     * @param teamName The name to check
     * @return true if the name is available, false if already taken
     */
    public boolean isTeamNameAvailable(String teamName) {
        if (teams == null || teamName == null || teamName.trim().isEmpty()) {
            return false;
        }

        return teams.stream()
                .noneMatch(team -> team.getName().equalsIgnoreCase(teamName.trim()));
    }

    /**
     * Checks if a project title is available for use in this assignment.
     *
     * @param projectTitle The project title to check
     * @return true if the title is available, false if already taken
     */
    public boolean isProjectTitleAvailable(String projectTitle) {
        if (!requireProjectTitle || teams == null || projectTitle == null || projectTitle.trim().isEmpty()) {
            return false;
        }

        return teams.stream()
                .noneMatch(team -> projectTitle.trim().equalsIgnoreCase(
                        team.getProjectTitle() != null ? team.getProjectTitle().trim() : ""));
    }

    /**
     * Gets all teams that have fewer than the maximum allowed members.
     *
     * @return List of teams that can accept more members
     */
    public List<Team> getTeamsWithOpenSpots() {
        if (teams == null) {
            return List.of();
        }

        return teams.stream()
                .filter(team -> team.getMembers().size() < maxTeamSize)
                .toList();
    }

    /**
     * Checks if a student can join a specific team.
     *
     * @param team The team to check
     * @param studentId The ID of the student trying to join
     * @return true if the student can join, false otherwise
     */
    public boolean canStudentJoinTeam(Team team, Integer studentId) {
        if (team == null || studentId == null) {
            return false;
        }

        // Check if team belongs to this assignment
        if (!teams.contains(team)) {
            return false;
        }

        // Check if team is full
        if (team.getMembers().size() >= maxTeamSize) {
            return false;
        }

        // Check if student is already in a team for this assignment
        return getTeamByStudent(studentId).isEmpty();
    }

    /**
     * Checks if this assignment allows students to create their own teams.
     *
     * @return true if self-forming teams are allowed
     */
    public boolean canStudentsFormTeams() {
        return allowSelfFormingTeams;
    }

    /**
     * Gets the average team size across all teams.
     *
     * @return The average team size or 0 if no teams exist
     */
    public double getAverageTeamSize() {
        if (teams == null || teams.isEmpty()) {
            return 0;
        }

        return teams.stream()
                .mapToInt(team -> team.getMembers().size())
                .average()
                .orElse(0);
    }

    /**
     * Checks if a team meets the minimum size requirement.
     *
     * @param team The team to check
     * @return true if the team meets minimum size requirements
     */
    public boolean doesTeamMeetMinimumSize(Team team) {
        if (team == null || team.getMembers() == null) {
            return false;
        }

        return team.getMembers().size() >= minTeamSize;
    }
}