package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentService;
import com.classroom.service.TeamService;
import com.classroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        // Log for debugging
        System.out.println("Debug - Authentication principal: " + (principal != null ? principal.getClass().getName() : "null"));
        
        try {
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUser();
            } else if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                return userService.getUserByEmail(username);
            } else if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                Map<String, Object> attributes = oauth2User.getAttributes();
                
                // Try to find email attribute
                String email = null;
                if (attributes.containsKey("email")) {
                    email = (String) attributes.get("email");
                } else if (attributes.containsKey("mail")) {
                    email = (String) attributes.get("mail");
                } else if (attributes.containsKey("preferred_username")) {
                    email = (String) attributes.get("preferred_username");
                } else {
                    // Try to find an attribute that looks like an email
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                        if (entry.getValue() instanceof String && 
                            ((String)entry.getValue()).contains("@")) {
                            email = (String)entry.getValue();
                            break;
                        }
                    }
                }
                
                if (email != null) {
                    return userService.getUserByEmail(email);
                }
            } else if (principal instanceof String) {
                return userService.getUserByEmail((String) principal);
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Get all teams for an assignment
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<?> getTeamsByAssignment(@PathVariable Long assignmentId) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Authentication required. Please log in.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            System.out.println("User found: " + currentUser.getEmail() + " for teams/assignment/" + assignmentId);
            
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Verify the assignment is a group assignment
            if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP || assignment.getGroupAssignment() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bad Request");
                error.put("message", "This is not a group assignment.");
                return ResponseEntity.badRequest().body(error);
            }
            
            GroupAssignment groupAssignment = assignment.getGroupAssignment();
            List<Team> teams = teamService.getTeamsByAssignment(groupAssignment);
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get a specific team by ID
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long teamId) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Team team = teamService.getTeamById(teamId);
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get team members for a specific team
     */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> getTeamMembers(
            @PathVariable Long teamId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        // Check if principal is null
        if (principal == null) {
            System.err.println("Principal is null for teams/" + teamId + "/members");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Unauthorized");
            error.put("message", "Authentication required. Please log in.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        try {
            // Try to get user from security context if principal doesn't have right attributes
            if (principal.getAttribute("email") == null && 
                principal.getAttribute("mail") == null && 
                principal.getAttribute("preferred_username") == null) {
                
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                    User user = userDetails.getUser();
                    System.out.println("Retrieved user from SecurityContext for team members: " + user.getEmail());
                    
                    // Verify the team exists
                    Team team;
                    try {
                        team = teamService.getTeamById(teamId);
                    } catch (Exception e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Not Found");
                        error.put("message", "Team not found with ID: " + teamId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                    
                    List<TeamMembership> members = teamService.getTeamMemberships(teamId);
                    return ResponseEntity.ok(members);
                }
            }
            
            User currentUser = getCurrentUser();
            
            // Check if current user was found
            if (currentUser == null) {
                System.err.println("Current user is null for teams/" + teamId + "/members" + 
                                  " with principal class: " + principal.getClass().getName());
                
                Map<String, Object> attributes = principal.getAttributes();
                System.err.println("Principal attributes: " + attributes.keySet());
                
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "User not found. Please log in again.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            System.out.println("User found: " + currentUser.getEmail() + " for teams/" + teamId + "/members");
            
            // Verify the team exists
            Team team;
            try {
                team = teamService.getTeamById(teamId);
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not Found");
                error.put("message", "Team not found with ID: " + teamId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            List<TeamMembership> members = teamService.getTeamMemberships(teamId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Create a new team
     */
    @PostMapping("/assignment/{assignmentId}")
    public ResponseEntity<Team> createTeam(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, String> teamData,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser();
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
        
        User currentUser = getCurrentUser();
        
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
        
        User currentUser = getCurrentUser();
        
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
        
        User currentUser = getCurrentUser();
        
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
        
        // Check if principal is null
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            User currentUser = getCurrentUser();
            
            // Check if current user was found
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            TeamMembership membership = teamService.acceptTeamInvitation(membershipId);
            
            // Verify the invitation belongs to the current user
            if (!membership.getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(membership);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject a team invitation
     */
    @PostMapping("/memberships/{membershipId}/reject")
    public ResponseEntity<Void> rejectTeamInvitation(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        // Check if principal is null
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            User currentUser = getCurrentUser();
            
            // Check if current user was found
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        
        User currentUser = getCurrentUser();
        
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
        
        User currentUser = getCurrentUser();
        
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
     * Get current user's pending team invitations
     */
    @GetMapping("/invitations")
    public ResponseEntity<?> getPendingInvitations() {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("Auth failed: No current user found for team invitations");
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Authentication required. Please log in.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            System.out.println("Debug - Getting pending invitations for user: " + currentUser.getEmail() + " (ID: " + currentUser.getId() + ")");
            
            List<TeamInvitation> invitations = new ArrayList<>();
            List<TeamMembership> memberships = teamService.getPendingInvitationsForStudent(currentUser);
            
            // Convert TeamMembership to TeamInvitation DTOs
            for (TeamMembership membership : memberships) {
                TeamInvitation invitation = new TeamInvitation();
                invitation.setId(membership.getId());
                invitation.setTeam(membership.getTeam());
                
                // Direct conversion from Date to LocalDateTime
                Date createdDate = membership.getCreatedAt();
                if (createdDate != null) {
                    LocalDateTime ldt = LocalDateTime.ofInstant(
                        createdDate.toInstant(), 
                        ZoneId.systemDefault()
                    );
                    invitation.setTimestamp(ldt);
                } else {
                    invitation.setTimestamp(LocalDateTime.now());
                }
                
                invitations.add(invitation);
            }
            
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get current user's team for a specific assignment
     */
    @GetMapping("/my-team/assignment/{assignmentId}")
    public ResponseEntity<?> getMyTeamByAssignment(@PathVariable Long assignmentId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Debug - Auth principal type: " + 
                (authentication != null && authentication.getPrincipal() != null ? 
                authentication.getPrincipal().getClass().getName() : "null"));
            
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("Auth failed: No current user found for my-team/assignment/" + assignmentId);
                
                // Log authentication details for debugging
                if (authentication != null) {
                    System.err.println("Authentication details: name=" + authentication.getName() + 
                        ", principal=" + (authentication.getPrincipal() != null ? 
                            authentication.getPrincipal().getClass().getName() : "null"));
                }
                
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Authentication required. Please log in.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            System.out.println("Debug - Getting team for user: " + currentUser.getEmail() + 
                " (ID: " + currentUser.getId() + ") for assignment: " + assignmentId);
            
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            System.out.println("Debug - Found assignment: " + assignment.getTitle() + 
                ", type: " + assignment.getAssignmentType());
            
            // Verify the assignment is a group assignment
            if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP) {
                System.err.println("Error: Assignment " + assignmentId + " is not a group assignment. Type: " + 
                    assignment.getAssignmentType());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bad Request");
                error.put("message", "This is not a group assignment.");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (assignment.getGroupAssignment() == null) {
                System.err.println("Error: GroupAssignment is null for assignment " + assignmentId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bad Request");
                error.put("message", "Group assignment details not found.");
                return ResponseEntity.badRequest().body(error);
            }
            
            GroupAssignment groupAssignment = assignment.getGroupAssignment();
            System.out.println("Debug - GroupAssignment found with ID: " + groupAssignment.getAssignmentId() + 
                ", min team size: " + groupAssignment.getMinTeamSize() + 
                ", max team size: " + groupAssignment.getMaxTeamSize());
                
            Team team = teamService.getTeamForStudentAndAssignment(currentUser, assignmentId);
            
            if (team == null) {
                System.out.println("Debug - No team found for user " + currentUser.getId() + 
                    " in assignment " + assignmentId);
                Map<String, Object> response = new HashMap<>();
                response.put("status", "NO_TEAM");
                response.put("message", "You are not in a team for this assignment.");
                return ResponseEntity.ok(response);
            }
            
            System.out.println("Debug - Found team: " + team.getName() + " (ID: " + team.getId() + ")");
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 