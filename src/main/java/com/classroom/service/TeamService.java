package com.classroom.service;

import com.classroom.model.*;
import com.classroom.repository.TeamMembershipRepository;
import com.classroom.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all teams for a specific group assignment
     */
    public List<Team> getTeamsByAssignment(GroupAssignment assignment) {
        return teamRepository.findByAssignment(assignment);
    }

    /**
     * Get a team by its ID
     */
    public Team getTeamById(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> 
            new IllegalArgumentException("Team not found with ID: " + teamId));
    }

    /**
     * Create a new team for a group assignment
     */
    @Transactional
    public Team createTeam(GroupAssignment assignment, String teamName, String projectTitle, User leader) {
        // Check if team with the same name already exists for this assignment
        if (teamRepository.findByAssignmentAndName(assignment, teamName) != null) {
            throw new IllegalArgumentException("A team with this name already exists for this assignment");
        }
        
        // Create the team
        Team team = new Team();
        team.setName(teamName);
        team.setProjectTitle(projectTitle);
        team.setAssignment(assignment);
        team.setCreatedBy(leader);
        team = teamRepository.save(team);
        
        // Add the creator as a leader and member
        TeamMembership membership = new TeamMembership();
        membership.setTeam(team);
        membership.setStudent(leader);
        membership.setLeader(true);
        membership.setAccepted(true);
        teamMembershipRepository.save(membership);
        
        return team;
    }

    /**
     * Update team information
     */
    @Transactional
    public Team updateTeam(Long teamId, String teamName, String projectTitle) {
        Team team = getTeamById(teamId);
        
        // Update team details
        team.setName(teamName);
        team.setProjectTitle(projectTitle);
        
        return teamRepository.save(team);
    }

    /**
     * Delete a team and all its memberships
     */
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = getTeamById(teamId);
        
        // First delete all memberships
        List<TeamMembership> memberships = teamMembershipRepository.findByTeam(team);
        teamMembershipRepository.deleteAll(memberships);
        
        // Then delete the team
        teamRepository.delete(team);
    }

    /**
     * Invite a student to join a team
     */
    @Transactional
    public TeamMembership inviteStudentToTeam(Long teamId, Long studentId) {
        Team team = getTeamById(teamId);
        User student = userService.getUserById(studentId);
        
        // Check if the student is already invited or a member of this team
        TeamMembership existingMembership = teamMembershipRepository.findByTeamAndStudent(team, student);
        if (existingMembership != null) {
            throw new IllegalArgumentException("Student is already invited or a member of this team");
        }
        
        // Check if the student is already in a team for this assignment
        if (teamMembershipRepository.isStudentInAnyTeamForAssignment(student, team.getAssignment().getAssignmentId())) {
            throw new IllegalArgumentException("Student is already in a team for this assignment");
        }
        
        // Check team size limit
        Long currentMembers = teamMembershipRepository.countByTeamAndAcceptedTrue(team);
        Integer maxTeamSize = team.getAssignment().getMaxTeamSize();
        if (maxTeamSize != null && currentMembers >= maxTeamSize) {
            throw new IllegalArgumentException("Team has reached maximum size limit of " + maxTeamSize);
        }
        
        // Create the invitation
        TeamMembership membership = new TeamMembership();
        membership.setTeam(team);
        membership.setStudent(student);
        membership.setLeader(false);
        membership.setAccepted(false);
        
        TeamMembership savedMembership = teamMembershipRepository.save(membership);
        
        // Create notification for the invited student
        try {
            if (notificationService != null) {
                notificationService.createTeamInvitationNotification(student, savedMembership);
            }
        } catch (Exception e) {
            // Log error but continue - notification failure shouldn't prevent the invitation
            System.err.println("Failed to create notification for team invitation: " + e.getMessage());
        }
        
        return savedMembership;
    }

    /**
     * Accept a team invitation
     */
    @Transactional
    public TeamMembership acceptTeamInvitation(Long membershipId) {
        TeamMembership membership = teamMembershipRepository.findById(membershipId)
            .orElseThrow(() -> new IllegalArgumentException("Team membership not found"));
        
        // Check if already accepted
        if (membership.isAccepted()) {
            throw new IllegalArgumentException("Invitation already accepted");
        }
        
        // Accept the invitation
        membership.accept();
        return teamMembershipRepository.save(membership);
    }

    /**
     * Reject a team invitation
     */
    @Transactional
    public void rejectTeamInvitation(Long membershipId) {
        TeamMembership membership = teamMembershipRepository.findById(membershipId)
            .orElseThrow(() -> new IllegalArgumentException("Team membership not found"));
        
        // Delete the invitation
        teamMembershipRepository.delete(membership);
    }

    /**
     * Remove a student from a team
     */
    @Transactional
    public void removeStudentFromTeam(Long teamId, Long studentId) {
        Team team = getTeamById(teamId);
        User student = userService.getUserById(studentId);
        
        TeamMembership membership = teamMembershipRepository.findByTeamAndStudent(team, student);
        if (membership == null) {
            throw new IllegalArgumentException("Student is not a member of this team");
        }
        
        // Check if this is the last leader
        if (membership.isLeader()) {
            long leaderCount = teamMembershipRepository.findByTeam(team).stream()
                .filter(TeamMembership::isLeader)
                .count();
            
            if (leaderCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last team leader");
            }
        }
        
        teamMembershipRepository.delete(membership);
    }

    /**
     * Assign a student as a team leader
     */
    @Transactional
    public TeamMembership assignTeamLeader(Long teamId, Long studentId) {
        Team team = getTeamById(teamId);
        User student = userService.getUserById(studentId);
        
        TeamMembership membership = teamMembershipRepository.findByTeamAndStudent(team, student);
        if (membership == null) {
            throw new IllegalArgumentException("Student is not a member of this team");
        }
        
        if (!membership.isAccepted()) {
            throw new IllegalArgumentException("Student has not accepted team invitation yet");
        }
        
        membership.setLeader(true);
        return teamMembershipRepository.save(membership);
    }

    /**
     * Get all team memberships for a specific team
     */
    public List<TeamMembership> getTeamMemberships(Long teamId) {
        Team team = getTeamById(teamId);
        return teamMembershipRepository.findByTeam(team);
    }

    /**
     * Get all accepted members for a team
     */
    public List<TeamMembership> getAcceptedTeamMembers(Long teamId) {
        Team team = getTeamById(teamId);
        return teamMembershipRepository.findByTeamAndAcceptedTrue(team);
    }

    /**
     * Get all teams that a student is a member of
     */
    public List<Team> getTeamsForStudent(User student) {
        return teamRepository.findTeamsWithStudent(student);
    }

    /**
     * Get a student's team for a specific assignment
     */
    public Team getTeamForStudentAndAssignment(User student, Long assignmentId) {
        return teamRepository.findTeamForStudentAndAssignment(student, assignmentId);
    }

    /**
     * Get pending team invitations for a student
     */
    public List<TeamMembership> getPendingInvitationsForStudent(User student) {
        return teamMembershipRepository.findByStudentAndAcceptedFalse(student);
    }

    /**
     * Count teams for an assignment
     */
    public long countTeamsByAssignment(GroupAssignment assignment) {
        return teamRepository.countByAssignment(assignment);
    }

    /**
     * Get a specific team membership by ID
     */
    public Optional<TeamMembership> getMembershipById(Long membershipId) {
        return teamMembershipRepository.findById(membershipId);
    }
} 