package com.classroom.controller;

import com.classroom.model.Mark;
import com.classroom.model.Submission;
import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import com.classroom.service.MarkService;
import com.classroom.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/marks")
public class MarkController {

    @Autowired
    private MarkService markService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get a mark by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Mark> getMarkById(@PathVariable Long id) {
        try {
            Mark mark = markService.getMarkById(id);
            return ResponseEntity.ok(mark);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get marks for a specific submission
     */
    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<?> getMarkBySubmission(@PathVariable Long submissionId) {
        Optional<Submission> submissionOpt = submissionService.getSubmissionById(submissionId);
        if (submissionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submission not found");
        }

        Optional<Mark> markOpt = markService.getMarkBySubmission(submissionOpt.get());
        if (markOpt.isPresent()) {
            return ResponseEntity.ok(markOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mark not found for this submission");
        }
    }

    /**
     * Get marks for a specific assignment
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<Mark>> getMarksByAssignment(@PathVariable Long assignmentId) {
        try {
            List<Mark> marks = markService.getMarksByAssignmentId(assignmentId);
            return ResponseEntity.ok(marks);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
    }

    /**
     * Get mark statistics for a specific assignment
     */
    @GetMapping("/assignment/{assignmentId}/statistics")
    public ResponseEntity<MarkService.MarkStatistics> getMarkStatisticsForAssignment(@PathVariable Long assignmentId) {
        try {
            MarkService.MarkStatistics statistics = markService.getMarkStatisticsForAssignmentId(assignmentId);
            return ResponseEntity.ok(statistics);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Create a new mark for a submission
     */
    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<?> createMark(
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal OAuth2User principal) {

        Integer rawMarks = (Integer) requestBody.get("rawMarks");
        Integer penaltyPercentage = (Integer) requestBody.get("penaltyPercentage");
        String feedback = (String) requestBody.get("feedback");

        // Get the current user
        String email = principal.getAttribute("email");
        User evaluator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        try {
            Mark mark = markService.createMarkBySubmissionId(submissionId, rawMarks, penaltyPercentage, feedback, evaluator);
            return ResponseEntity.status(HttpStatus.CREATED).body(mark);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Update an existing mark
     */
    @PutMapping("/{markId}")
    public ResponseEntity<?> updateMark(
            @PathVariable Long markId,
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal OAuth2User principal) {

        Integer rawMarks = (Integer) requestBody.get("rawMarks");
        Integer penaltyPercentage = (Integer) requestBody.get("penaltyPercentage");
        String feedback = (String) requestBody.get("feedback");
        String reason = (String) requestBody.get("reason");

        // Get the current user
        String email = principal.getAttribute("email");
        User changedBy = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        try {
            Mark mark = markService.updateMark(markId, rawMarks, penaltyPercentage, feedback, changedBy, reason);
            return ResponseEntity.ok(mark);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Delete a mark
     */
    @DeleteMapping("/{markId}")
    public ResponseEntity<?> deleteMark(@PathVariable Long markId) {
        try {
            markService.deleteMark(markId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
} 