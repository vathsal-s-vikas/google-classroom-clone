package com.classroom.service;

import com.classroom.model.Assignment;
import com.classroom.model.Submission;
import com.classroom.model.User;
import com.classroom.model.Team;
import com.classroom.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionAttachmentService submissionAttachmentService;

    @Autowired
    private AssignmentService assignmentService;

    /**
     * Get all submissions for a specific assignment
     */
    public List<Submission> getSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignment(assignment);
    }

    /**
     * Get a specific submission by ID
     */
    public Optional<Submission> getSubmissionById(Long id) {
        return submissionRepository.findById(id);
    }

    /**
     * Get a submission for a specific assignment and student
     */
    public Optional<Submission> getSubmissionByAssignmentAndStudent(Assignment assignment, User student) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student);
    }

    /**
     * Get a submission for a specific assignment and team
     */
    public Optional<Submission> getSubmissionByAssignmentAndTeam(Assignment assignment, Team team) {
        return submissionRepository.findByAssignmentAndTeam(assignment, team);
    }

    /**
     * Get all submissions by a student
     */
    public List<Submission> getSubmissionsByStudent(User student) {
        return submissionRepository.findByStudent(student);
    }

    /**
     * Get all submissions by a team
     */
    public List<Submission> getSubmissionsByTeam(Team team) {
        return submissionRepository.findByTeam(team);
    }

    /**
     * Get all submissions that haven't been evaluated yet
     */
    public List<Submission> getUnevaluatedSubmissions() {
        return submissionRepository.findByIsEvaluatedFalse();
    }

    /**
     * Get all submissions for an assignment that haven't been evaluated
     */
    public List<Submission> getUnevaluatedSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignmentAndIsEvaluatedFalse(assignment);
    }

    /**
     * Get submission count for an assignment
     */
    public int getSubmissionCountByAssignment(Assignment assignment) {
        return (int) submissionRepository.countByAssignment(assignment);
    }

    /**
     * Create a new submission for an individual student
     */
    @Transactional
    public Submission createStudentSubmission(Assignment assignment, User student, String submissionText) {
        // Check if student already submitted
        Optional<Submission> existingSubmission = getSubmissionByAssignmentAndStudent(assignment, student);
        if (existingSubmission.isPresent()) {
            throw new RuntimeException("You have already submitted for this assignment");
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmissionText(submissionText);
        
        // Check if submission is late
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = assignment.getDeadline();
        
        if (now.isAfter(deadline)) {
            submission.setLate(true);
            
            // Calculate days late
            long daysLate = ChronoUnit.DAYS.between(deadline, now);
            submission.setDaysLate((int) daysLate);
            
            // Check if late submission is allowed
            if (!assignment.isLateSubmissionAllowed()) {
                throw new RuntimeException("Late submissions are not allowed for this assignment");
            }
            
            // Check if it's within max late days
            if (assignment.getMaxLateDays() != null && daysLate > assignment.getMaxLateDays()) {
                throw new RuntimeException("Submission is too late. Maximum allowed late days: " + assignment.getMaxLateDays());
            }
        } else {
            submission.setLate(false);
            submission.setDaysLate(0);
        }
        
        return submissionRepository.save(submission);
    }

    /**
     * Create a new submission for a team
     */
    @Transactional
    public Submission createTeamSubmission(Assignment assignment, Team team, String submissionText) {
        // Check if team already submitted
        Optional<Submission> existingSubmission = getSubmissionByAssignmentAndTeam(assignment, team);
        if (existingSubmission.isPresent()) {
            throw new RuntimeException("Your team has already submitted for this assignment");
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setTeam(team);
        submission.setSubmissionText(submissionText);
        
        // Check if submission is late
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = assignment.getDeadline();
        
        if (now.isAfter(deadline)) {
            submission.setLate(true);
            
            // Calculate days late
            long daysLate = ChronoUnit.DAYS.between(deadline, now);
            submission.setDaysLate((int) daysLate);
            
            // Check if late submission is allowed
            if (!assignment.isLateSubmissionAllowed()) {
                throw new RuntimeException("Late submissions are not allowed for this assignment");
            }
            
            // Check if it's within max late days
            if (assignment.getMaxLateDays() != null && daysLate > assignment.getMaxLateDays()) {
                throw new RuntimeException("Submission is too late. Maximum allowed late days: " + assignment.getMaxLateDays());
            }
        } else {
            submission.setLate(false);
            submission.setDaysLate(0);
        }
        
        return submissionRepository.save(submission);
    }

    /**
     * Update a submission
     */
    @Transactional
    public Submission updateSubmission(Long submissionId, String submissionText) {
        Submission submission = getSubmissionById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));
        submission.setSubmissionText(submissionText);
        return submissionRepository.save(submission);
    }

    /**
     * Mark a submission as evaluated
     */
    @Transactional
    public Submission markAsEvaluated(Long submissionId) {
        Submission submission = getSubmissionById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));
        submission.setEvaluated(true);
        return submissionRepository.save(submission);
    }

    /**
     * Create a new submission for an individual student or team
     */
    @Transactional
    public Submission createSubmission(Long assignmentId, User student, Team team, String submissionText, List<MultipartFile> files) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        
        // Check if submission already exists
        Submission existingSubmission = null;
        if (student != null) {
            Optional<Submission> existingOpt = submissionRepository.findByAssignmentAndStudent(assignment, student);
            if (existingOpt.isPresent()) {
                existingSubmission = existingOpt.get();
            }
        } else if (team != null) {
            Optional<Submission> existingOpt = submissionRepository.findByAssignmentAndTeam(assignment, team);
            if (existingOpt.isPresent()) {
                existingSubmission = existingOpt.get();
            }
        }
        
        if (existingSubmission != null) {
            // Update existing submission
            existingSubmission.setSubmissionText(submissionText);
            existingSubmission.setSubmittedAt(LocalDateTime.now());
            
            // Check if submission is late
            LocalDateTime deadline = assignment.getDeadline();
            if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
                existingSubmission.setLate(true);
                // Calculate days late
                long daysLate = ChronoUnit.DAYS.between(deadline, LocalDateTime.now());
                existingSubmission.setDaysLate((int) daysLate);
            }
            
            Submission savedSubmission = submissionRepository.save(existingSubmission);
            
            // Handle attachments if files are provided
            if (files != null && !files.isEmpty()) {
                // First remove existing attachments
                submissionAttachmentService.deleteAttachmentsBySubmission(savedSubmission);
                // Then add new ones
                submissionAttachmentService.saveAttachments(savedSubmission, files);
            }
            
            return savedSubmission;
        } else {
            // Create new submission
            Submission submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
            submission.setTeam(team);
            submission.setSubmissionText(submissionText);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setEvaluated(false);
            
            // Check if submission is late
            LocalDateTime deadline = assignment.getDeadline();
            if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
                submission.setLate(true);
                // Calculate days late
                long daysLate = ChronoUnit.DAYS.between(deadline, LocalDateTime.now());
                submission.setDaysLate((int) daysLate);
            } else {
                submission.setLate(false);
                submission.setDaysLate(0);
            }
            
            Submission savedSubmission = submissionRepository.save(submission);
            
            // Handle attachments if files are provided
            if (files != null && !files.isEmpty()) {
                submissionAttachmentService.saveAttachments(savedSubmission, files);
            }
            
            return savedSubmission;
        }
    }

    /**
     * Delete a submission
     */
    @Transactional
    public void deleteSubmission(Long submissionId) {
        submissionRepository.deleteById(submissionId);
    }
} 