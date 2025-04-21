package com.classroom.dto;

import com.classroom.model.Assignment;
import com.classroom.model.GroupAssignment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDTO {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private String instructions;
    private String assignmentType;
    private Integer maxMarks;
    private LocalDateTime deadline;
    private boolean lateSubmissionAllowed;
    private Integer latePenaltyPercentage;
    private Integer maxLateDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Group assignment details
    private GroupAssignmentDTO groupAssignment;
    
    public static AssignmentDTO fromEntity(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        if (assignment.getCourse() != null) {
            dto.setCourseId(assignment.getCourse().getId());
        }
        dto.setTitle(assignment.getTitle());
        dto.setDescription(assignment.getDescription());
        dto.setInstructions(assignment.getInstructions());
        if (assignment.getAssignmentType() != null) {
            dto.setAssignmentType(assignment.getAssignmentType().name());
        }
        dto.setMaxMarks(assignment.getMaxMarks());
        dto.setDeadline(assignment.getDeadline());
        dto.setLateSubmissionAllowed(assignment.isLateSubmissionAllowed());
        dto.setLatePenaltyPercentage(assignment.getLatePenaltyPercentage());
        dto.setMaxLateDays(assignment.getMaxLateDays());
        dto.setCreatedAt(assignment.getCreatedAt());
        dto.setUpdatedAt(assignment.getUpdatedAt());
        
        // Set group assignment if available
        if (assignment.getGroupAssignment() != null) {
            dto.setGroupAssignment(GroupAssignmentDTO.fromEntity(assignment.getGroupAssignment()));
        }
        
        return dto;
    }
    
    // Inner class for GroupAssignment details
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupAssignmentDTO {
        private Integer minTeamSize;
        private Integer maxTeamSize;
        private boolean allowSelfFormingTeams;
        private boolean requireProjectTitle;
        
        public static GroupAssignmentDTO fromEntity(GroupAssignment groupAssignment) {
            GroupAssignmentDTO dto = new GroupAssignmentDTO();
            dto.setMinTeamSize(groupAssignment.getMinTeamSize());
            dto.setMaxTeamSize(groupAssignment.getMaxTeamSize());
            dto.setAllowSelfFormingTeams(groupAssignment.isAllowSelfFormingTeams());
            dto.setRequireProjectTitle(groupAssignment.isRequireProjectTitle());
            return dto;
        }
    }
} 