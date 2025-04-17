package com.classroom.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "assignment_attachments")
public class AssignmentAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileType;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    // Explicit getter methods in case Lombok isn't working correctly
    public String getFileName() {
        return fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}