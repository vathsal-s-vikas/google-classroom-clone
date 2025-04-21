package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.service.AssignmentService;
import com.classroom.service.TeamService;
import com.classroom.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StudentViewController {

    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomAuthenticationConverter authConverter;
    
    private User getCurrentUser() {
        // First convert the authentication if needed
        authConverter.convertAuthentication();
        
        // Get the updated authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        
        if (principal instanceof UserDetails) {
            // If it's a regular UserDetails, get the username and fetch the user from repository
            final String username = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else if (principal instanceof String) {
            // If it's just a username string
            final String username = (String) principal;
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else {
            throw new RuntimeException("Unsupported principal type: " + principal.getClass().getName());
        }
    }
    
    /**
     * Show team management page for a group assignment
     */
    @GetMapping("/student/assignment/{assignmentId}/team")
    public String showTeamManagement(@PathVariable Long assignmentId, Model model) {
        User currentUser = getCurrentUser();
        
        // Get the assignment
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Verify it's a group assignment
        if (assignment.getAssignmentType() != Assignment.AssignmentType.GROUP || assignment.getGroupAssignment() == null) {
            return "redirect:/dashboard/student";
        }
        
        // Get group assignment details
        GroupAssignment groupAssignment = assignment.getGroupAssignment();
        
        // Get the student's team for this assignment, if any
        Team team = teamService.getTeamForStudentAndAssignment(currentUser, assignmentId);
        
        // Get pending invitations for this student for this assignment
        java.util.List<TeamMembership> pendingInvitations = teamService.getPendingInvitationsForStudent(currentUser).stream()
                .filter(invitation -> invitation.getTeam().getAssignment().getAssignmentId().equals(assignmentId))
                .collect(java.util.stream.Collectors.toList());
        
        // Get all teams for this assignment if self-forming teams are allowed
        java.util.List<Team> availableTeams = null;
        if (groupAssignment.isAllowSelfFormingTeams() && team == null) {
            availableTeams = teamService.getTeamsByAssignment(groupAssignment);
        }
        
        // Add data to the model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("assignment", assignment);
        model.addAttribute("groupAssignment", groupAssignment);
        model.addAttribute("team", team);
        model.addAttribute("pendingInvitations", pendingInvitations);
        model.addAttribute("availableTeams", availableTeams);
        
        if (team != null) {
            // Get team members
            java.util.List<TeamMembership> members = teamService.getAcceptedTeamMembers(team.getId());
            model.addAttribute("teamMembers", members);
        }
        
        return "course/student-team-management";
    }
} 