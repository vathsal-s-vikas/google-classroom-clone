package com.classroom.controller;

import com.classroom.model.*;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentService;
import com.classroom.service.SubmissionService;
import com.classroom.service.SubmissionAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;
    
    @Autowired
    private SubmissionAttachmentService attachmentService;
    
    @Autowired
    private AssignmentService assignmentService;

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
    public ResponseEntity<?> createSubmission(@RequestBody Map<String, Object> submissionData) {
        try {
            User currentUser = getCurrentUser();
            
            // Extract data from request
            Long assignmentId = Long.parseLong(submissionData.get("assignmentId").toString());
            String submissionText = (String) submissionData.get("submissionText");
            
            // Get the assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Check if the user is a student
            if (currentUser.getUserType() != UserType.STUDENT) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Only students can create submissions"
                ));
            }
            
            // Check if student is enrolled in the course
            boolean isEnrolled = assignment.getCourse().getMemberships().stream()
                    .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
            
            if (!isEnrolled) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "You are not enrolled in this course"
                ));
            }
            
            // Check if student has already submitted
            Optional<Submission> existingSubmission = 
                    submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
            
            if (existingSubmission.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You have already submitted for this assignment"
                ));
            }
            
            // Create the submission
            Submission submission = submissionService.createStudentSubmission(
                    assignment, currentUser, submissionText);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Submission created successfully",
                "submissionId", submission.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error creating submission: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{submissionId}/upload")
    public ResponseEntity<?> uploadSubmissionFile(
            @PathVariable Long submissionId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            User currentUser = getCurrentUser();
            
            // Get the submission
            Optional<Submission> optSubmission = submissionService.getSubmissionById(submissionId);
            if (!optSubmission.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Submission not found"
                ));
            }
            
            Submission submission = optSubmission.get();
            
            // Validate that the current user is the owner of the submission
            if (!submission.getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "You are not authorized to upload files for this submission"
                ));
            }
            
            // Save the attachment
            SubmissionAttachment attachment = attachmentService.saveAttachment(submission, file);
            
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
    
    @GetMapping("/{submissionId}/attachments")
    public ResponseEntity<List<SubmissionAttachment>> getSubmissionAttachments(@PathVariable Long submissionId) {
        try {
            User currentUser = getCurrentUser();
            Optional<Submission> optSubmission = submissionService.getSubmissionById(submissionId);
            
            if (!optSubmission.isPresent()) {
                return ResponseEntity.badRequest().body(null);
            }
            
            Submission submission = optSubmission.get();
            
            // Check if user is authorized (student who submitted or teacher of the course)
            boolean isSubmitter = submission.getStudent() != null && 
                    submission.getStudent().getId().equals(currentUser.getId());
            boolean isTeacher = submission.getAssignment().getCourse().getTeacher().getId()
                    .equals(currentUser.getId());
            
            if (!isSubmitter && !isTeacher) {
                return ResponseEntity.status(403).body(null);
            }
            
            List<SubmissionAttachment> attachments = attachmentService.getAttachmentsBySubmission(submission);
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
            SubmissionAttachment attachment = attachmentService.getAttachmentById(attachmentId);
            
            // Validate that the current user is the submitter
            if (!attachment.getSubmission().getStudent().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "You are not authorized to delete this attachment"
                ));
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
    
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<Submission>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Validate that the current user is the teacher of the course
            if (!assignment.getCourse().getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(null);
            }
            
            // Get submissions
            List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
            
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @GetMapping("/{submissionId}")
    public ResponseEntity<Submission> getSubmission(@PathVariable Long submissionId) {
        try {
            User currentUser = getCurrentUser();
            Optional<Submission> optSubmission = submissionService.getSubmissionById(submissionId);
            
            if (!optSubmission.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Submission submission = optSubmission.get();
            
            // Check if user is authorized (student who submitted or teacher of the course)
            boolean isSubmitter = submission.getStudent() != null && 
                    submission.getStudent().getId().equals(currentUser.getId());
            boolean isTeacher = submission.getAssignment().getCourse().getTeacher().getId()
                    .equals(currentUser.getId());
            
            if (!isSubmitter && !isTeacher) {
                return ResponseEntity.status(403).body(null);
            }
            
            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @PutMapping("/{submissionId}/mark-evaluated")
    public ResponseEntity<?> markSubmissionAsEvaluated(@PathVariable Long submissionId) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the submission
            Optional<Submission> optSubmission = submissionService.getSubmissionById(submissionId);
            
            if (!optSubmission.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Submission not found"
                ));
            }
            
            Submission submission = optSubmission.get();
            
            // Validate that the current user is the teacher of the course
            boolean isTeacher = submission.getAssignment().getCourse().getTeacher().getId()
                    .equals(currentUser.getId());
            
            if (!isTeacher) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Only teachers can mark submissions as evaluated"
                ));
            }
            
            // Mark as evaluated
            Submission updatedSubmission = submissionService.markAsEvaluated(submissionId);
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Submission marked as evaluated",
                "submission", updatedSubmission
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error marking submission as evaluated: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/submit")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("submissionText") String submissionText,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {
        
        try {
            User currentUser = getCurrentUser();
            
            // Get the assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Check if the user is a student
            if (currentUser.getUserType() != UserType.STUDENT) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Only students can submit assignments"
                ));
            }
            
            // Check if student is enrolled in the course
            boolean isEnrolled = assignment.getCourse().getMemberships().stream()
                    .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
            
            if (!isEnrolled) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "You are not enrolled in this course"
                ));
            }
            
            // Check if student has already submitted
            Optional<Submission> existingSubmission = 
                    submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
            
            if (existingSubmission.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You have already submitted for this assignment"
                ));
            }
            
            // Create the submission
            Submission submission = submissionService.createStudentSubmission(
                    assignment, currentUser, submissionText);
            
            // Save attachments if any
            if (attachments != null && !attachments.isEmpty()) {
                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        attachmentService.saveAttachment(submission, file);
                    }
                }
            }
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Assignment submitted successfully",
                "submissionId", submission.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error submitting assignment: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/check-status/{assignmentId}")
    public ResponseEntity<Map<String, Object>> checkSubmissionStatus(@PathVariable Long assignmentId) {
        try {
            User currentUser = getCurrentUser();
            
            // Get the assignment
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            
            // Check if student has submitted
            Optional<Submission> existingSubmission = 
                    submissionService.getSubmissionByAssignmentAndStudent(assignment, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            
            if (existingSubmission.isPresent()) {
                Submission submission = existingSubmission.get();
                response.put("submitted", true);
                response.put("submissionId", submission.getId());
                response.put("submittedAt", submission.getSubmittedAt());
                response.put("isLate", submission.isLate());
                response.put("isEvaluated", submission.isEvaluated());
            } else {
                response.put("submitted", false);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error checking submission status: " + e.getMessage()
            ));
        }
    }
} 