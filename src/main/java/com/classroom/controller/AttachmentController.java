package com.classroom.controller;

import com.classroom.model.AssignmentAttachment;
import com.classroom.model.SubmissionAttachment;
import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.AssignmentAttachmentService;
import com.classroom.service.SubmissionAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private AssignmentAttachmentService assignmentAttachmentService;
    
    @Autowired
    private SubmissionAttachmentService submissionAttachmentService;
    
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
    
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) throws IOException {
        // First, try to find it as an assignment attachment
        try {
            AssignmentAttachment attachment = assignmentAttachmentService.getAttachmentById(attachmentId);
            
            // In a real application, you would download the file from your storage system
            // For this example, we'll simulate returning a file from a local file system
            
            // Create a dummy file path - in a real app, you'd get this from your storage system
            String filePath = attachment.getFilePath();
            
            // For now, just return a placeholder text file
            String content = "This is a placeholder for file: " + attachment.getFileName();
            ByteArrayResource resource = new ByteArrayResource(content.getBytes());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            // If not found as assignment attachment, try submission attachment
            try {
                SubmissionAttachment attachment = submissionAttachmentService.getAttachmentById(attachmentId);
                
                // Similar logic as above
                String fileUrl = attachment.getFileUrl();
                
                // For now, just return a placeholder text file
                String content = "This is a placeholder for file: " + attachment.getFileName();
                ByteArrayResource resource = new ByteArrayResource(content.getBytes());
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(attachment.getFileType()))
                        .contentLength(resource.contentLength())
                        .body(resource);
            } catch (Exception ex) {
                // Not found in either repository
                return ResponseEntity.notFound().build();
            }
        }
    }
} 