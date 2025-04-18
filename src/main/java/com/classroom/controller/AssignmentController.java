package com.classroom.controller;

import com.classroom.dto.AssignmentDTO;
import com.classroom.model.Assignment;
import com.classroom.model.AssignmentAttachment;
import com.classroom.model.Course;
import com.classroom.model.Submission;
import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentAttachmentService;
import com.classroom.service.AssignmentService;
import com.classroom.service.CourseService;
import com.classroom.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomAuthenticationConverter authConverter;
    
    @Autowired
    private AssignmentAttachmentService attachmentService;

    @Autowired
    private SubmissionService submissionService;

    private User getCurrentUser() {
        // First convert the authentication if needed
        authConverter.convertAuthentication();
        
        // Get the updated authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof UserDetails) {
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

    @PostMapping("/create")
    public ResponseEntity<?> createAssignment(@RequestBody Map<String, Object> assignmentData) {
        try {
            User currentUser = getCurrentUser();
            
            // Extract data from request
            Long courseId = Long.parseLong(assignmentData.get("courseId").toString());
            String title = (String) assignmentData.get("title");
            String description = (String) assignmentData.get("description");
            String instructions = (String) assignmentData.get("instructions");
            String assignmentType = (String) assignmentData.get("assignmentType");
            Integer maxMarks = Integer.parseInt(assignmentData.get("maxMarks").toString());
            
            // Parse deadline
            String deadlineStr = (String) assignmentData.get("deadline");
            LocalDateTime deadline = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_DATE_TIME);
            
            // Late submission options
            Boolean lateSubmissionAllowed = Boolean.parseBoolean(assignmentData.get("lateSubmissionAllowed").toString());
            Integer latePenaltyPercentage = null;
            Integer maxLateDays = null;
            
            if (lateSubmissionAllowed) {
                if (assignmentData.containsKey("latePenaltyPercentage") && assignmentData.get("latePenaltyPercentage") != null) {
                    latePenaltyPercentage = Integer.parseInt(assignmentData.get("latePenaltyPercentage").toString());
                }
                
                if (assignmentData.containsKey("maxLateDays") && assignmentData.get("maxLateDays") != null) {
                    maxLateDays = Integer.parseInt(assignmentData.get("maxLateDays").toString());
                }
            }
            
            // Get the course
            Course course = courseService.getCourseById(courseId);
            
            // Validate that the current user is the teacher of the course
            if (!course.getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to create assignments for this course");
            }
            
            // Create the assignment
            Assignment assignment = new Assignment();
            assignment.setCourse(course);
            assignment.setTitle(title);
            assignment.setDescription(description);
            assignment.setInstructions(instructions);
            assignment.setAssignmentType(Assignment.AssignmentType.valueOf(assignmentType));
            assignment.setMaxMarks(maxMarks);
            assignment.setDeadline(deadline);
            assignment.setLateSubmissionAllowed(lateSubmissionAllowed);
            
            if (lateSubmissionAllowed) {
                assignment.setLatePenaltyPercentage(latePenaltyPercentage);
                assignment.setMaxLateDays(maxLateDays);
            }
            
            // Save the assignment
            Assignment savedAssignment = assignmentService.createAssignment(assignment);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Assignment created successfully",
                "assignmentId", savedAssignment.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error creating assignment: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{assignmentId}/upload")
    public ResponseEntity<?> uploadAssignmentFile(
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            User currentUser = getCurrentUser();
            
            // Get the assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Validate that the current user is the teacher of the course
            if (!assignment.getCourse().getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to upload files for this assignment");
            }
            
            // Save the attachment
            AssignmentAttachment attachment = attachmentService.saveAttachment(assignment, file);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "File uploaded successfully",
                "attachmentId", attachment.getId(),
                "fileName", attachment.getFileName()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error uploading file: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{assignmentId}/attachments")
    public ResponseEntity<List<AssignmentAttachment>> getAssignmentAttachments(@PathVariable Long assignmentId) {
        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            List<AssignmentAttachment> attachments = attachmentService.getAttachmentsByAssignment(assignment);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long attachmentId) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the attachment
            AssignmentAttachment attachment = attachmentService.getAttachmentById(attachmentId);
            
            // Validate that the current user is the teacher of the course
            if (!attachment.getAssignment().getCourse().getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to delete this attachment");
            }
            
            // Delete the attachment
            attachmentService.deleteAttachment(attachmentId);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Attachment deleted successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error deleting attachment: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getAssignmentsByCourse(@PathVariable Long courseId) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the course
            Course course = courseService.getCourseById(courseId);
            
            // Get assignments
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
            
            // Convert to DTOs
            List<AssignmentDTO> assignmentDTOs = assignments.stream()
                    .map(AssignmentDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(assignmentDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error retrieving assignments: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{assignmentId}")
    public ResponseEntity<?> getAssignment(@PathVariable Long assignmentId) {
        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            AssignmentDTO assignmentDTO = AssignmentDTO.fromEntity(assignment);
            return ResponseEntity.ok(assignmentDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error retrieving assignment: " + e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<?> deleteAssignment(@PathVariable Long assignmentId) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Validate that the current user is the teacher of the course
            if (!assignment.getCourse().getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to delete this assignment");
            }
            
            // Delete the assignment
            assignmentService.deleteAssignment(assignmentId);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Assignment deleted successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error deleting assignment: " + e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{assignmentId}/update")
    public ResponseEntity<?> updateAssignment(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, Object> assignmentData) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the existing assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Validate that the current user is the teacher of the course
            if (!assignment.getCourse().getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to update this assignment");
            }
            
            // Extract data from request
            String title = (String) assignmentData.get("title");
            String description = (String) assignmentData.get("description");
            String instructions = (String) assignmentData.get("instructions");
            String assignmentType = (String) assignmentData.get("assignmentType");
            Integer maxMarks = Integer.parseInt(assignmentData.get("maxMarks").toString());
            
            // Parse deadline
            String deadlineStr = (String) assignmentData.get("deadline");
            LocalDateTime deadline = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_DATE_TIME);
            
            // Late submission options
            Boolean lateSubmissionAllowed = Boolean.parseBoolean(assignmentData.get("lateSubmissionAllowed").toString());
            Integer latePenaltyPercentage = null;
            Integer maxLateDays = null;
            
            if (lateSubmissionAllowed) {
                if (assignmentData.containsKey("latePenaltyPercentage") && assignmentData.get("latePenaltyPercentage") != null) {
                    latePenaltyPercentage = Integer.parseInt(assignmentData.get("latePenaltyPercentage").toString());
                }
                
                if (assignmentData.containsKey("maxLateDays") && assignmentData.get("maxLateDays") != null) {
                    maxLateDays = Integer.parseInt(assignmentData.get("maxLateDays").toString());
                }
            }
            
            // Update the assignment
            assignment.setTitle(title);
            assignment.setDescription(description);
            assignment.setInstructions(instructions);
            assignment.setAssignmentType(Assignment.AssignmentType.valueOf(assignmentType));
            assignment.setMaxMarks(maxMarks);
            assignment.setDeadline(deadline);
            assignment.setLateSubmissionAllowed(lateSubmissionAllowed);
            
            if (lateSubmissionAllowed) {
                assignment.setLatePenaltyPercentage(latePenaltyPercentage);
                assignment.setMaxLateDays(maxLateDays);
            } else {
                assignment.setLatePenaltyPercentage(null);
                assignment.setMaxLateDays(null);
            }
            
            // Save the updated assignment
            Assignment updatedAssignment = assignmentService.updateAssignment(assignment);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Assignment updated successfully",
                "assignmentId", updatedAssignment.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error updating assignment: " + e.getMessage()
            ));
        }
    }
} 