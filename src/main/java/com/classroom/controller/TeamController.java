package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.service.AssignmentService;
import com.classroom.service.TeamService;
import com.classroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private UserService userService;

    private User getCurrentUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userService.getUserByEmail(email);
    }

    /**
     * Get all teams for an assignment
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<Team>> getTeamsByAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the assignment is a group assignment
        if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP || assignment.getGroupAssignment() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        GroupAssignment groupAssignment = assignment.getGroupAssignment();
        List<Team> teams = teamService.getTeamsByAssignment(groupAssignment);
        return ResponseEntity.ok(teams);
    }

    /**
     * Get a specific team by ID
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<Team> getTeamById(
            @PathVariable Long teamId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Team team = teamService.getTeamById(teamId);
        return ResponseEntity.ok(team);
    }

    /**
     * Get team members for a specific team
     */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMembership>> getTeamMembers(
            @PathVariable Long teamId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        List<TeamMembership> members = teamService.getTeamMemberships(teamId);
        return ResponseEntity.ok(members);
    }

    /**
     * Create a new team
     */
    @PostMapping("/assignment/{assignmentId}")
    public ResponseEntity<Team> createTeam(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, String> teamData,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify the assignment is a group assignment
        if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP || assignment.getGroupAssignment() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        GroupAssignment groupAssignment = assignment.getGroupAssignment();
        String teamName = teamData.get("name");
        String projectTitle = teamData.get("projectTitle");
        
        if (teamName == null || teamName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Team team = teamService.createTeam(groupAssignment, teamName, projectTitle, currentUser);
            return new ResponseEntity<>(team, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update team details
     */
    @PutMapping("/{teamId}")
    public ResponseEntity<Team> updateTeam(
            @PathVariable Long teamId,
            @RequestBody Map<String, String> teamData,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            // Verify user is a leader of this team
            Team team = teamService.getTeamById(teamId);
            List<TeamMembership> memberships = teamService.getTeamMemberships(teamId);
            boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getStudent().getId().equals(currentUser.getId()) && m.isLeader());
            
            if (!isLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            String teamName = teamData.get("name");
            String projectTitle = teamData.get("projectTitle");
            
            Team updatedTeam = teamService.updateTeam(teamId, teamName, projectTitle);
            return ResponseEntity.ok(updatedTeam);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a team
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            // Verify user is a leader of this team
            Team team = teamService.getTeamById(teamId);
            List<TeamMembership> memberships = teamService.getTeamMemberships(teamId);
            boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getStudent().getId().equals(currentUser.getId()) && m.isLeader());
            
            if (!isLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            teamService.deleteTeam(teamId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Invite a student to join a team
     */
    @PostMapping("/{teamId}/invite/{studentId}")
    public ResponseEntity<TeamMembership> inviteStudentToTeam(
            @PathVariable Long teamId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            // Verify user is a leader of this team
            Team team = teamService.getTeamById(teamId);
            List<TeamMembership> memberships = teamService.getTeamMemberships(teamId);
            boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getStudent().getId().equals(currentUser.getId()) && m.isLeader());
            
            if (!isLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            TeamMembership membership = teamService.inviteStudentToTeam(teamId, studentId);
            return new ResponseEntity<>(membership, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Accept a team invitation
     */
    @PostMapping("/memberships/{membershipId}/accept")
    public ResponseEntity<TeamMembership> acceptTeamInvitation(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            TeamMembership membership = teamService.acceptTeamInvitation(membershipId);
            
            // Verify the invitation belongs to the current user
            if (!membership.getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(membership);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reject a team invitation
     */
    @PostMapping("/memberships/{membershipId}/reject")
    public ResponseEntity<Void> rejectTeamInvitation(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            // Get the membership directly from the repository
            TeamMembership membership = teamService.getMembershipById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
            
            if (!membership.getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            teamService.rejectTeamInvitation(membershipId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a student from a team
     */
    @DeleteMapping("/{teamId}/members/{studentId}")
    public ResponseEntity<Void> removeStudentFromTeam(
            @PathVariable Long teamId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            // Verify user is a leader of this team or removing themselves
            boolean isLeaderOrSelf = studentId.equals(currentUser.getId());
            
            if (!isLeaderOrSelf) {
                List<TeamMembership> memberships = teamService.getTeamMemberships(teamId);
                isLeaderOrSelf = memberships.stream()
                    .anyMatch(m -> m.getStudent().getId().equals(currentUser.getId()) && m.isLeader());
            }
            
            if (!isLeaderOrSelf) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            teamService.removeStudentFromTeam(teamId, studentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Assign a student as a team leader
     */
    @PostMapping("/{teamId}/leaders/{studentId}")
    public ResponseEntity<TeamMembership> assignTeamLeader(
            @PathVariable Long teamId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        
        try {
            // Verify user is a leader of this team
            List<TeamMembership> memberships = teamService.getTeamMemberships(teamId);
            boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getStudent().getId().equals(currentUser.getId()) && m.isLeader());
            
            if (!isLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            TeamMembership membership = teamService.assignTeamLeader(teamId, studentId);
            return ResponseEntity.ok(membership);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get pending team invitations for the current user
     */
    @GetMapping("/invitations")
    public ResponseEntity<List<Map<String, Object>>> getPendingInvitations(
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        List<TeamMembership> pendingInvitations = teamService.getPendingInvitationsForStudent(currentUser);
        
        List<Map<String, Object>> invitationsWithDetails = pendingInvitations.stream()
            .map(membership -> {
                Map<String, Object> details = new HashMap<>();
                details.put("membershipId", membership.getId());
                details.put("team", membership.getTeam());
                details.put("assignment", membership.getTeam().getAssignment());
                details.put("course", membership.getTeam().getAssignment().getAssignment().getCourse());
                return details;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(invitationsWithDetails);
    }

    /**
     * Get the current user's team for a specific assignment
     */
    @GetMapping("/my-team/assignment/{assignmentId}")
    public ResponseEntity<Team> getMyTeamForAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Team team = teamService.getTeamForStudentAndAssignment(currentUser, assignmentId);
        
        if (team == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(team);
    }

    /**
     * Get the current user's team status for a specific assignment
     */
    @GetMapping("/my-team/{assignmentId}")
    public ResponseEntity<?> getMyTeamForAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Verify the assignment is a group assignment
            if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP || assignment.getGroupAssignment() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "This is not a group assignment"
                ));
            }
            
            // Get the student's team for this assignment
            Team team = teamService.getTeamForStudentAndAssignment(currentUser, assignmentId);
            
            // Get pending invitations for this student for this assignment
            List<TeamMembership> pendingInvitations = teamService.getPendingInvitationsForStudent(currentUser).stream()
                    .filter(invitation -> invitation.getTeam().getAssignment().getAssignmentId().equals(assignmentId))
                    .collect(Collectors.toList());
            
            // Return the team details and pending invitations
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            if (team != null) {
                // Student is in a team
                response.put("status", "IN_TEAM");
                response.put("team", team);
                
                // Also include team members
                List<TeamMembership> members = teamService.getAcceptedTeamMembers(team.getId());
                response.put("members", members);
            } else if (!pendingInvitations.isEmpty()) {
                // Student has pending invitations
                response.put("status", "HAS_INVITATIONS");
                response.put("pendingInvitations", pendingInvitations);
            } else {
                // Student is not in a team yet
                response.put("status", "NO_TEAM");
                
                // Include assignment details for team formation
                GroupAssignment groupAssignment = assignment.getGroupAssignment();
                response.put("groupAssignment", groupAssignment);
                
                // Include all existing teams if self-forming teams are allowed
                if (groupAssignment.isAllowSelfFormingTeams()) {
                    List<Team> teams = teamService.getTeamsByAssignment(groupAssignment);
                    response.put("availableTeams", teams);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error getting team status: " + e.getMessage()
            ));
        }
    }

    /**
     * Create a new team for a group assignment (student view)
     */
    @PostMapping("/create-team/{assignmentId}")
    public ResponseEntity<?> createTeamAsStudent(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, String> teamData,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Verify the assignment is a group assignment
            if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP || assignment.getGroupAssignment() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "This is not a group assignment"
                ));
            }
            
            GroupAssignment groupAssignment = assignment.getGroupAssignment();
            
            // Check if self-forming teams are allowed
            if (!groupAssignment.isAllowSelfFormingTeams()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Students are not allowed to create teams for this assignment"
                ));
            }
            
            // Check if student is already in a team for this assignment
            Team existingTeam = teamService.getTeamForStudentAndAssignment(currentUser, assignmentId);
            if (existingTeam != null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You are already in a team for this assignment"
                ));
            }
            
            // Get team details from request
            String teamName = teamData.get("name");
            String projectTitle = teamData.get("projectTitle");
            
            if (teamName == null || teamName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Team name is required"
                ));
            }
            
            // Check if project title is required
            if (groupAssignment.isRequireProjectTitle() && (projectTitle == null || projectTitle.trim().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Project title is required"
                ));
            }
            
            // Create the team
            Team team = teamService.createTeam(groupAssignment, teamName, projectTitle, currentUser);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Team created successfully",
                "team", team
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error creating team: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Request to join an existing team
     */
    @PostMapping("/join-team/{teamId}")
    public ResponseEntity<?> requestToJoinTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Team team = teamService.getTeamById(teamId);
            
            // Verify the assignment is a group assignment
            Assignment assignment = assignmentService.getAssignmentById(team.getAssignment().getAssignmentId());
            if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "This is not a group assignment"
                ));
            }
            
            // Check if student is already in a team for this assignment
            Team existingTeam = teamService.getTeamForStudentAndAssignment(currentUser, assignment.getId());
            if (existingTeam != null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You are already in a team for this assignment"
                ));
            }
            
            // Check if team has reached maximum size
            long currentMemberCount = teamService.getAcceptedTeamMembers(team.getId()).size();
            if (currentMemberCount >= team.getAssignment().getMaxTeamSize()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Team has reached maximum size"
                ));
            }
            
            // Create a join request (this will add the student to the team with accepted=false)
            TeamMembership membership = new TeamMembership();
            membership.setTeam(team);
            membership.setStudent(currentUser);
            membership.setLeader(false);
            membership.setAccepted(false);
            teamService.saveTeamMembership(membership);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Join request sent successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error joining team: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Accept an invitation to join a team
     */
    @PostMapping("/accept-invitation/{membershipId}")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            // Get the membership
            Optional<TeamMembership> membershipOpt = teamService.getMembershipById(membershipId);
            if (!membershipOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invitation not found"
                ));
            }
            
            TeamMembership membership = membershipOpt.get();
            
            // Verify the invitation belongs to the current user
            if (!membership.getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "This invitation does not belong to you"
                ));
            }
            
            // Check if student is already in a team for this assignment
            Team existingTeam = teamService.getTeamForStudentAndAssignment(
                    currentUser, 
                    membership.getTeam().getAssignment().getAssignmentId());
                    
            if (existingTeam != null && !existingTeam.getId().equals(membership.getTeam().getId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You are already in a different team for this assignment"
                ));
            }
            
            // Accept the invitation
            membership.setAccepted(true);
            membership.setJoinedAt(new Date());
            teamService.saveTeamMembership(membership);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "You have joined the team",
                "team", membership.getTeam()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error accepting invitation: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Decline an invitation to join a team
     */
    @PostMapping("/decline-invitation/{membershipId}")
    public ResponseEntity<?> declineInvitation(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            // Get the membership
            Optional<TeamMembership> membershipOpt = teamService.getMembershipById(membershipId);
            if (!membershipOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invitation not found"
                ));
            }
            
            TeamMembership membership = membershipOpt.get();
            
            // Verify the invitation belongs to the current user
            if (!membership.getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "This invitation does not belong to you"
                ));
            }
            
            // Delete the membership (decline the invitation)
            teamService.deleteTeamMembership(membership.getId());
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Invitation declined"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error declining invitation: " + e.getMessage()
            ));
        }
    }
} 