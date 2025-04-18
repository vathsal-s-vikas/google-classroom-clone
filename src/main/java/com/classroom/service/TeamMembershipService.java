package com.classroom.service;

import com.classroom.model.Team;
import com.classroom.model.TeamMembership;
import com.classroom.model.User;
import com.classroom.repository.TeamMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TeamMembershipService {

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    /**
     * Get all memberships for a specific team
     */
    public List<TeamMembership> getMembershipsByTeam(Team team) {
        return teamMembershipRepository.findByTeam(team);
    }

    /**
     * Get all team memberships for a student
     */
    public List<TeamMembership> getMembershipsByStudent(User student) {
        return teamMembershipRepository.findByStudent(student);
    }

    /**
     * Find a specific membership by team and student
     */
    public Optional<TeamMembership> getMembershipByTeamAndStudent(Team team, User student) {
        return Optional.ofNullable(teamMembershipRepository.findByTeamAndStudent(team, student));
    }

    /**
     * Create a new team membership
     */
    @Transactional
    public TeamMembership createMembership(Team team, User student, boolean isLeader) {
        // Check if student is already a member of this team
        TeamMembership existingMembership = teamMembershipRepository.findByTeamAndStudent(team, student);
        if (existingMembership != null) {
            return existingMembership;
        }

        // Check if student is already in another team for the same assignment
        if (teamMembershipRepository.isStudentInAnyTeamForAssignment(student, team.getAssignment().getAssignmentId())) {
            throw new IllegalStateException("Student is already in a team for this assignment");
        }

        // Create new membership
        TeamMembership membership = TeamMembership.builder()
                .team(team)
                .student(student)
                .leader(isLeader)
                .accepted(isLeader) // Leader automatically accepts
                .build();

        return teamMembershipRepository.save(membership);
    }

    /**
     * Accept a team invitation
     */
    @Transactional
    public TeamMembership acceptInvitation(TeamMembership membership) {
        if (membership.isAccepted()) {
            return membership; // Already accepted
        }

        membership.setAccepted(true);
        return teamMembershipRepository.save(membership);
    }

    /**
     * Remove a member from a team
     */
    @Transactional
    public void removeMembership(TeamMembership membership) {
        teamMembershipRepository.delete(membership);
    }

    /**
     * Check if a student is in any team for the given assignment
     */
    public boolean isStudentInAnyTeamForAssignment(User student, Long assignmentId) {
        return teamMembershipRepository.isStudentInAnyTeamForAssignment(student, assignmentId);
    }

    /**
     * Get all memberships for teams in a specific assignment
     */
    public List<TeamMembership> getMembershipsByAssignment(Long assignmentId) {
        return teamMembershipRepository.findMembershipsByAssignment(assignmentId);
    }

    /**
     * Count members in a team
     */
    public long countMembersByTeam(Team team) {
        return teamMembershipRepository.countByTeam(team);
    }
} 