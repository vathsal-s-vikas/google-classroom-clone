package com.classroom.service;

import com.classroom.model.Submission;
import com.classroom.model.SubmissionAttachment;
import com.classroom.repository.SubmissionAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubmissionAttachmentService {

    @Autowired
    private SubmissionAttachmentRepository submissionAttachmentRepository;

    /**
     * Get all attachments for a submission
     */
    public List<SubmissionAttachment> getAttachmentsBySubmission(Submission submission) {
        return submissionAttachmentRepository.findBySubmission(submission);
    }

    /**
     * Get attachment by ID
     */
    public SubmissionAttachment getAttachmentById(Long id) {
        return submissionAttachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission attachment not found with id: " + id));
    }
    
    /**
     * Save a single attachment for a submission
     */
    @Transactional
    public SubmissionAttachment saveAttachment(Submission submission, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }
        
        SubmissionAttachment attachment = new SubmissionAttachment();
        attachment.setSubmission(submission);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize((int) file.getSize());
        attachment.setFileData(file.getBytes());
        
        if (attachment.getFileUrl() == null) {
            // Set a default file URL if not provided
            attachment.setFileUrl("/api/attachments/" + submission.getId() + "/" + file.getOriginalFilename());
        }
        
        return submissionAttachmentRepository.save(attachment);
    }

    /**
     * Save multiple attachments for a submission
     */
    @Transactional
    public List<SubmissionAttachment> saveAttachments(Submission submission, List<MultipartFile> files) {
        List<SubmissionAttachment> savedAttachments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    SubmissionAttachment attachment = new SubmissionAttachment();
                    attachment.setSubmission(submission);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFileType(file.getContentType());
                    attachment.setFileSize((int) file.getSize());
                    attachment.setFileData(file.getBytes());
                    
                    savedAttachments.add(submissionAttachmentRepository.save(attachment));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
                }
            }
        }
        
        return savedAttachments;
    }

    /**
     * Delete an attachment by ID
     */
    @Transactional
    public void deleteAttachment(Long id) {
        submissionAttachmentRepository.deleteById(id);
    }

    /**
     * Delete all attachments for a submission
     */
    @Transactional
    public void deleteAttachmentsBySubmission(Submission submission) {
        submissionAttachmentRepository.deleteBySubmission(submission);
    }

    /**
     * Count attachments for a submission
     */
    public long countAttachmentsBySubmission(Submission submission) {
        return submissionAttachmentRepository.countBySubmission(submission);
    }
} 