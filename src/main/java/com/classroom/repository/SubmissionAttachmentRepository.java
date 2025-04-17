package com.classroom.repository;

import com.classroom.model.Submission;
import com.classroom.model.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, Long> {
    
    // Find all attachments for a specific submission
    List<SubmissionAttachment> findBySubmission(Submission submission);
    
    // Delete all attachments for a specific submission
    void deleteBySubmission(Submission submission);
    
    // Count attachments for a submission
    long countBySubmission(Submission submission);
} 