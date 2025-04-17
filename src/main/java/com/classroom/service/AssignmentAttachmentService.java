package com.classroom.service;

import com.classroom.model.Assignment;
import com.classroom.model.AssignmentAttachment;
import com.classroom.repository.AssignmentAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class AssignmentAttachmentService {

    @Autowired
    private AssignmentAttachmentRepository assignmentAttachmentRepository;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    public List<AssignmentAttachment> getAttachmentsByAssignment(Assignment assignment) {
        return assignmentAttachmentRepository.findByAssignment(assignment);
    }
    
    public AssignmentAttachment getAttachmentById(Long id) {
        return assignmentAttachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id " + id));
    }
    
    public AssignmentAttachment saveAttachment(Assignment assignment, MultipartFile file) throws IOException {
        // Create uploads directory if it doesn't exist
        Path uploadsPath = Paths.get(uploadDir, "assignments", assignment.getId().toString());
        if (!Files.exists(uploadsPath)) {
            Files.createDirectories(uploadsPath);
        }
        
        // Generate a unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Save the file
        Path filePath = uploadsPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // Create and save the attachment record
        AssignmentAttachment attachment = new AssignmentAttachment();
        attachment.setAssignment(assignment);
        attachment.setFileName(originalFilename);
        attachment.setFileType(file.getContentType());
        attachment.setFilePath(filePath.toString());
        attachment.setFileSize(file.getSize());
        
        return assignmentAttachmentRepository.save(attachment);
    }
    
    public void deleteAttachment(Long id) throws IOException {
        AssignmentAttachment attachment = getAttachmentById(id);
        
        // Delete the file
        Path filePath = Paths.get(attachment.getFilePath());
        Files.deleteIfExists(filePath);
        
        // Delete the record
        assignmentAttachmentRepository.deleteById(id);
    }
} 