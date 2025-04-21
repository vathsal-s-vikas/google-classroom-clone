package com.classroom.service;

import com.classroom.model.Assignment;
import com.classroom.model.Mark;
import com.classroom.model.MarkHistory;
import com.classroom.model.Submission;
import com.classroom.model.User;
import com.classroom.repository.MarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MarkService {

    @Autowired
    private MarkRepository markRepository;
    
    @Autowired
    private SubmissionService submissionService;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Get mark by ID
     */
    public Mark getMarkById(Long id) {
        return markRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mark not found with id: " + id));
    }
    
    /**
     * Get mark by submission
     */
    public Optional<Mark> getMarkBySubmission(Submission submission) {
        return markRepository.findBySubmission(submission);
    }
    
    /**
     * Get all marks evaluated by a specific user
     */
    public List<Mark> getMarksByEvaluator(User evaluator) {
        return markRepository.findByEvaluator(evaluator);
    }
    
    /**
     * Get all marks for a specific assignment
     */
    public List<Mark> getMarksByAssignment(Assignment assignment) {
        return markRepository.findByAssignment(assignment);
    }
    
    /**
     * Get all marks for a specific assignment by ID
     */
    public List<Mark> getMarksByAssignmentId(Long assignmentId) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        return markRepository.findByAssignment(assignment);
    }
    
    /**
     * Count the number of evaluated submissions for an assignment
     */
    public Long countMarksByAssignment(Assignment assignment) {
        return markRepository.countByAssignment(assignment);
    }
    
    /**
     * Count the number of evaluated submissions for an assignment by ID
     */
    public Long countMarksByAssignmentId(Long assignmentId) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        return markRepository.countByAssignment(assignment);
    }
    
    /**
     * Get mark statistics for a specific assignment
     */
    public MarkStatistics getMarkStatisticsForAssignment(Assignment assignment) {
        Integer highestMark = markRepository.findHighestMarkByAssignment(assignment);
        Integer lowestMark = markRepository.findLowestMarkByAssignment(assignment);
        Double averageMark = markRepository.calculateAverageMarkByAssignment(assignment);
        Long totalSubmissions = markRepository.countByAssignment(assignment);
        
        return new MarkStatistics(
            totalSubmissions != null ? totalSubmissions.intValue() : 0,
            highestMark != null ? highestMark : 0,
            lowestMark != null ? lowestMark : 0,
            averageMark != null ? averageMark : 0.0
        );
    }
    
    /**
     * Get mark statistics for a specific assignment by ID
     */
    public MarkStatistics getMarkStatisticsForAssignmentId(Long assignmentId) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        return getMarkStatisticsForAssignment(assignment);
    }
    
    /**
     * Create a new mark for a submission
     */
    @Transactional
    public Mark createMark(Submission submission, Integer rawMarks, Integer penaltyPercentage, 
                           String feedback, User evaluator) {
        // Check if submission exists and is not already evaluated
        if (submission == null) {
            throw new RuntimeException("Submission cannot be null");
        }
        
        if (submission.isEvaluated()) {
            throw new RuntimeException("Submission is already evaluated");
        }
        
        // Calculate final marks after applying penalty
        Integer finalMarks = calculateFinalMarks(rawMarks, penaltyPercentage);
        
        // Create new mark
        Mark mark = new Mark();
        mark.setSubmission(submission);
        mark.setRawMarks(rawMarks);
        mark.setPenaltyPercentage(penaltyPercentage);
        mark.setFinalMarks(finalMarks);
        mark.setFeedback(feedback);
        mark.setEvaluator(evaluator);
        mark.setEvaluatedAt(LocalDateTime.now());
        
        // Mark the submission as evaluated
        submission.setEvaluated(true);
        submissionService.markAsEvaluated(submission.getId());
        
        Mark savedMark = markRepository.save(mark);
        
        // Send notification to the student
        notifyStudentAboutGradedSubmission(submission, savedMark);
        
        return savedMark;
    }
    
    /**
     * Create a new mark for a submission by ID
     */
    @Transactional
    public Mark createMarkBySubmissionId(Long submissionId, Integer rawMarks, 
                                        Integer penaltyPercentage, String feedback, User evaluator) {
        Submission submission = submissionService.getSubmissionById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));
        
        return createMark(submission, rawMarks, penaltyPercentage, feedback, evaluator);
    }
    
    /**
     * Update an existing mark
     */
    @Transactional
    public Mark updateMark(Long markId, Integer rawMarks, Integer penaltyPercentage, 
                           String feedback, User changedBy, String reason) {
        Mark mark = getMarkById(markId);
        
        // Create mark history before updating
        MarkHistory history = new MarkHistory();
        history.setMark(mark);
        history.setPreviousRawMarks(mark.getRawMarks());
        history.setPreviousFinalMarks(mark.getFinalMarks());
        history.setPreviousFeedback(mark.getFeedback());
        history.setChangedBy(changedBy);
        history.setReason(reason);
        
        // Update mark
        mark.setRawMarks(rawMarks);
        mark.setPenaltyPercentage(penaltyPercentage);
        mark.setFinalMarks(calculateFinalMarks(rawMarks, penaltyPercentage));
        mark.setFeedback(feedback);
        
        // Add history to mark
        Set<MarkHistory> historySet = mark.getHistory();
        historySet.add(history);
        mark.setHistory(historySet);
        
        return markRepository.save(mark);
    }
    
    /**
     * Delete a mark
     */
    @Transactional
    public void deleteMark(Long markId) {
        Mark mark = getMarkById(markId);
        
        // Un-evaluate the submission
        Submission submission = mark.getSubmission();
        submission.setEvaluated(false);
        submission.setMark(null);
        
        markRepository.deleteById(markId);
    }
    
    /**
     * Calculate final marks after applying penalty
     */
    private Integer calculateFinalMarks(Integer rawMarks, Integer penaltyPercentage) {
        if (rawMarks == null || penaltyPercentage == null || penaltyPercentage < 0) {
            return rawMarks;
        }
        
        double penalty = (penaltyPercentage / 100.0) * rawMarks;
        return (int) Math.max(0, rawMarks - penalty);
    }
    
    /**
     * Get mark statistics for a list of submissions
     */
    public MarkStatistics getMarkStatisticsForSubmissions(List<Submission> submissions) {
        int totalMarks = 0;
        int totalSubmissions = 0;
        int highestMark = Integer.MIN_VALUE;
        int lowestMark = Integer.MAX_VALUE;
        
        for (Submission submission : submissions) {
            Optional<Mark> markOpt = getMarkBySubmission(submission);
            if (markOpt.isPresent()) {
                Mark mark = markOpt.get();
                int finalMarks = mark.getFinalMarks();
                
                totalMarks += finalMarks;
                totalSubmissions++;
                
                if (finalMarks > highestMark) {
                    highestMark = finalMarks;
                }
                
                if (finalMarks < lowestMark) {
                    lowestMark = finalMarks;
                }
            }
        }
        
        double average = totalSubmissions > 0 ? (double) totalMarks / totalSubmissions : 0;
        
        return new MarkStatistics(
            totalSubmissions,
            highestMark != Integer.MIN_VALUE ? highestMark : 0,
            lowestMark != Integer.MAX_VALUE ? lowestMark : 0,
            average
        );
    }
    
    /**
     * Save a mark directly
     */
    @Transactional
    public Mark saveMark(Mark mark) {
        // Ensure the submission is marked as evaluated
        Submission submission = mark.getSubmission();
        if (submission != null && !submission.isEvaluated()) {
            submission.setEvaluated(true);
            submissionService.markAsEvaluated(submission.getId());
            
            // Send notification to the student
            notifyStudentAboutGradedSubmission(submission, mark);
        }
        
        return markRepository.save(mark);
    }
    
    /**
     * Notify student(s) about their graded submission
     * @param submission The submission that has been graded
     * @param mark The mark that was given
     */
    private void notifyStudentAboutGradedSubmission(Submission submission, Mark mark) {
        if (submission.getStudent() != null) {
            // Individual submission
            notificationService.notifyGradedSubmission(
                submission.getStudent(),
                submission.getAssignment().getTitle(),
                mark.getFinalMarks()
            );
        } else if (submission.getTeam() != null) {
            // Team submission - notify all team members
            submission.getTeam().getUsers().forEach(student -> 
                notificationService.notifyGradedSubmission(
                    student,
                    submission.getAssignment().getTitle(),
                    mark.getFinalMarks()
                )
            );
        }
    }
    
    // Helper class for mark statistics
    public static class MarkStatistics {
        private final int totalSubmissions;
        private final int highestMark;
        private final int lowestMark;
        private final double averageMark;
        
        public MarkStatistics(int totalSubmissions, int highestMark, int lowestMark, double averageMark) {
            this.totalSubmissions = totalSubmissions;
            this.highestMark = highestMark;
            this.lowestMark = lowestMark;
            this.averageMark = averageMark;
        }
        
        public int getTotalSubmissions() {
            return totalSubmissions;
        }
        
        public int getHighestMark() {
            return highestMark;
        }
        
        public int getLowestMark() {
            return lowestMark;
        }
        
        public double getAverageMark() {
            return averageMark;
        }
    }
} 