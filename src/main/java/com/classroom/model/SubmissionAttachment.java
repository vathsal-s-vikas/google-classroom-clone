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
@Table(name = "submission_attachments")
public class SubmissionAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;
    
    @Lob
    @Column(name = "file_data", nullable = true, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Explicit getter methods in case Lombok isn't working correctly
    public Long getId() {
        return id;
    }
    
    public Submission getSubmission() {
        return submission;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public Integer getFileSize() {
        return fileSize;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    // Manual setter methods since Lombok isn't working properly
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setSubmission(Submission submission) {
        this.submission = submission;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }
    
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}